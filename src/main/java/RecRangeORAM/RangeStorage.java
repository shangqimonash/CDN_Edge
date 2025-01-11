package RecRangeORAM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.*;

import Interfaces.ExternalStorageInterface;
import data.DataItem;
import data.SimpleDataItem;
import RORAMConfig.Block;
import RORAMConfig.Bucket;
import RORAMConfig.TreeConfigure;
import RORAMDataType.*;
import pollable.Pollable;
import services.DownloadObject;
import services.Request;
import services.ScheduledOperation;
import services.UploadObject;


public class RangeStorage {

	private ExternalStorageInterface storedTree;

	private long treeSize;

	private int stashSize;

	private int oramID;

	public RangeStorage(ExternalStorageInterface si,int _stashSize, int oramID)
	{
		//set size for the stash
		this.stashSize = _stashSize;

		// initialize the tree
		this.treeSize = 2*TreeConfigure.N-1;
		System.out.println("\tBuildTree: r-> " + this.oramID + ", tree size-> "  + treeSize);

		this.oramID = oramID;

		//init buckets in the fileStorage
		this.storedTree = si;

		final int removeIntervalSize = 512;
		final double sizeFactorForSlowdown = 0.75;
		Vector<Pollable> v = new Vector<Pollable>();
		Bucket temp;

		for (int i = 0; i < treeSize; i++)
		{
			//need to mention the
			temp = new Bucket(new Block());

			DataItem di = new SimpleDataItem(temp.toByteArray());
			int fileLabel = Math.floorDiv(i,TreeConfigure.bucketLimitPerFile);

			//fileName = oramID + fileLabel;
			String objectKey = Integer.toString(oramID)+"#" + Integer.toString(fileLabel);
			int fileOffset = i%(TreeConfigure.bucketLimitPerFile);

			UploadObject upload = new UploadObject(Request.initReqId, objectKey, di);
			upload.setObjectOffset(fileOffset*TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen));
			upload.setObjectSize(TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen));
			ScheduledOperation sop = storedTree.uploadObject(upload);

			v.add(sop);

			//delay writing file if needed
			if(i > 0 && (i % removeIntervalSize) == 0)
			{
				Pollable.removeCompleted(v);

				if(v.size() >= (int)(removeIntervalSize * sizeFactorForSlowdown))
				{
					int factor = (int)Math.ceil(v.size() / removeIntervalSize); if(factor > 5) { factor = 5; }
					try { Thread.sleep(factor * 5); }
					catch (InterruptedException e) { System.out.println(e.getMessage()); }
					Pollable.removeCompleted(v);
				}
			}
		}

		Pollable.waitForCompletion(v);

	}

	/*
	 * Download a sequential chunk of buckets from each level
	 * startingBucket - indicate which bucket to start fetching from in each level
	 * numOfBuckets - number of buckets to fetch from each level
	 */
	private ArrayList<Block> downloadChunk(long reqId, int startingBucket[], int numOfBuckets[]) {


		DownloadObject download;
		String objectKey;

		Vector<ScheduledOperation> v = new Vector<ScheduledOperation>();
		int[] bucketsUptoEnd = new int[TreeConfigure.D+1];
		int[] bucketsFromStart = new int[TreeConfigure.D+1];

		ArrayList<Integer> pollableList = new ArrayList<Integer>();

		for(int i=TreeConfigure.D; i >= 0; i--){

			bucketsUptoEnd[i] = 0;
			bucketsFromStart[i] = 0;

			int startingOffsetAtLevel; // knowing physically where to start in the file

			/*  Identify wrap around for level based on numOfBuckets */
			if(i == 0)
				startingOffsetAtLevel = 0;
			else
				startingOffsetAtLevel = (int) (Math.pow(2, i)-1);

			int t = 0;
			while(t < numOfBuckets[i]) {
				if((startingBucket[i]+t) < ((int) (Math.pow(2, i))))
					bucketsUptoEnd[i] += 1;
				else
					bucketsFromStart[i] += 1;
				t++;
			}


			int temp = 0;
			int fileLabel = Math.floorDiv(startingBucket[i]+startingOffsetAtLevel,TreeConfigure.bucketLimitPerFile);

			int fileOffset = (startingBucket[i]+startingOffsetAtLevel)%(TreeConfigure.bucketLimitPerFile); //set the beginning of the current reading file
			int cntr = 0;

			//inject (l,bu,o) into this sop so that we can retrieve it later and assign to the file.
			int temp_path = startingBucket[i] % (int) Math.pow(2,i); //????
			ArrayList<Integer> tracking_paths = new ArrayList<Integer>();

			//pollableList.size() returns how many segments have read
			//pollableList.get(k) returns the cntr value (size of each read segment)
			//each v.get(k) returns the byte array of that read segment.

			/* Retrieve buckets up to end of the level starting from startBucket */
			while(temp < bucketsUptoEnd[i]) {

				if(temp + fileOffset >= TreeConfigure.bucketLimitPerFile) { //check if it exceeds the current file, we need to read
					objectKey = Integer.toString(oramID)+"#"  + Integer.toString(fileLabel);
					download = new DownloadObject(reqId, objectKey);
					download.setObjectOffset((fileOffset)*TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen));
					download.setObjectSize(TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen)*cntr);

					ScheduledOperation sop = storedTree.downloadObject(download);
					pollableList.add(cntr);

					//System.out.println("pollableList add(cntr) " + cntr + " , track path: " + tracking_paths + ", level D " + (i+1));

					fileOffset = 0;
					fileLabel += 1;
					cntr = 0;

					//set tracking paths and level for further retrieve
					sop.setPaths_Level(tracking_paths, i+1);
					v.add(sop);

					//reset tracking paths
					tracking_paths.clear();
				}

				temp++;	//run by the bucket, thus we can increase the path
				cntr++;

				//tracking the path
				tracking_paths.add(temp_path % (int) Math.pow(2,i));
				temp_path++;

			}

			//read for the last one
			objectKey = Integer.toString(oramID)+"#"  + Integer.toString(fileLabel);
			download = new DownloadObject(reqId, objectKey);
			download.setObjectOffset((fileOffset)*TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen));
			download.setObjectSize(TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen)*cntr);

			ScheduledOperation sop = storedTree.downloadObject(download);
			pollableList.add(cntr);
			//set tracking paths and level for further retrieve

			//System.out.println("pollableList add(cntr) " + cntr + " , track path " + tracking_paths + ", level D " + (i+1));

			sop.setPaths_Level(tracking_paths, i+1);
			v.add(sop);

			//reset variables to read the wrapping arround
			temp = 0;
			fileLabel = Math.floorDiv(startingOffsetAtLevel,TreeConfigure.bucketLimitPerFile);
			fileOffset = startingOffsetAtLevel%(TreeConfigure.bucketLimitPerFile);
			cntr = 0;

			tracking_paths.clear();
			temp_path = 0;


			/* Retrieve buckets after wrap around from start of level */
			if(bucketsFromStart[i] > 0) {
				while(temp < bucketsFromStart[i]) {

					if(temp + fileOffset >= TreeConfigure.bucketLimitPerFile) {
						objectKey = Integer.toString(oramID)+"#" + Integer.toString(fileLabel);
						download = new DownloadObject(reqId, objectKey);

						download.setObjectOffset((fileOffset)*TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen));
						download.setObjectSize(TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen)*cntr);
						sop = storedTree.downloadObject(download);
						pollableList.add(cntr);
						fileOffset = 0;
						fileLabel += 1;
						cntr = 0;

						//set tracking paths and level for further retrieve
						//System.out.println("pollableList add(cntr) " + cntr + " , track path " + tracking_paths+ ", level D " + (i+1));

						sop.setPaths_Level(tracking_paths, i+1);
						v.add(sop);

						//reset tracking paths
						tracking_paths.clear();
					}

					temp++; //run by the bucket, thus we can increase the path
					cntr++;

					//tracking the path
					tracking_paths.add(temp_path % (int) Math.pow(2,i));
					temp_path++;

				}


				objectKey = Integer.toString(oramID)+"#" + Integer.toString(fileLabel);
				download = new DownloadObject(reqId, objectKey);
				download.setObjectOffset((fileOffset)*TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen));
				download.setObjectSize(TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen)*cntr);
				sop = storedTree.downloadObject(download);
				pollableList.add(cntr);

				//set tracking paths and level for further retrieve
				//System.out.println("pollableList add(cntr) " + cntr + " , track path " + tracking_paths + ", level D " + (i+1));

				sop.setPaths_Level(tracking_paths, i+1);

				v.add(sop);

				tracking_paths.clear();
			}
		}


		Pollable.waitForCompletion(v);


		/* Reconcile all fetched items and return block list */
		ArrayList<Block> blockList = new ArrayList<Block>();
		for(int k = 0; k < pollableList.size(); k++) { //pollableList.size() returns how many segments have read

			//each v.get(k).getDataItem().getData() returns the byte array of that read segment.
			byte[] b = v.get(k).getDataItem().getData();

			ArrayList<Integer> ret_tracking_paths = v.get(k).getPathOrder(); //-> return the list of paths; totally, there are cntr paths.
			int ret_level = v.get(k).getStorageLevel(); //get the current level of that segment

			//System.out.println("k= " + k + " : ret_track ->" + ret_tracking_paths + "level " + ret_level);

			//declare the byte array of the bucket
			byte[] bucket = new byte[TreeConfigure.Z*(TreeConfigure.extDataSize+TreeConfigure.nonceLen)];

			// a bucket is collection of blocks having the same path
			//pollableList.get(k) returns the cntr value (i.e., the number of buckets) of each read segment
			for(int j = 0; j < pollableList.get(k); j++) {

				//set byte to the bucketket
				for(int i = 0; i < TreeConfigure.Z*(TreeConfigure.extDataSize+TreeConfigure.nonceLen); i++) {
					bucket[i] = b[j*(TreeConfigure.Z*(TreeConfigure.extDataSize+TreeConfigure.nonceLen))+i];
				}

				//in the construction of Bucket, we also set offset in Z as well for blocks
				Bucket bkt = new Bucket(bucket,ret_tracking_paths.get(j),ret_level);
				for(Block blk : bkt.blocks)
					blockList.add(blk);
			}
		}

		return  blockList;

	}

	/*
	 * Upload a sequential chunk of buckets to each level
	 * startingBucket - indicate which bucket to start uploading to in each level
	 * numOfBuckets - number of buckets to upload to each level
	 * blockList - blocks to upload
	 */
	public void uploadChunk(long reqId, int startingBucket[], int numOfBuckets[], ArrayList<Block> blockList) {

		//prepare buffer such that write as in a batch of range
		//need to maintain the order of blockList in correct order of -l,bu,o- when writing
		//or the block here is finding for it based on l,bu,o

		Vector<ScheduledOperation> v = new Vector<ScheduledOperation>();
		String objectKey;
		UploadObject upload;

		int startingOffsetAtLevel;
		int nextBlock = 0;

		for(int level = TreeConfigure.D; level >= 0; level--) {

			/*  Identify wrap around for level based on numOfBuckets */
			if(level == 0) {
				startingOffsetAtLevel = 0;
			}
			else {
				startingOffsetAtLevel = (int) (Math.pow(2, level)-1);
			}

			int bucketsUptoEnd = 0;
			int bucketsFromStart= 0;

			int t = 0;
			while(t < numOfBuckets[level]) {
				if((startingBucket[level]+t) < ((int) (Math.pow(2, level))))
					bucketsUptoEnd += 1;
				else
					bucketsFromStart += 1;
				t++;
			}

			int temp = 0;
			int fileLabel = Math.floorDiv(startingBucket[level]+startingOffsetAtLevel,TreeConfigure.bucketLimitPerFile);
			int fileOffset = (startingBucket[level]+startingOffsetAtLevel)%(TreeConfigure.bucketLimitPerFile);
			int cntr = 0;

			/* upload buckets till end of level from starting bucket */
			while(temp < bucketsUptoEnd) {

				if(temp + fileOffset >= TreeConfigure.bucketLimitPerFile) {
					byte[] b = new byte[cntr*(TreeConfigure.extDataSize+TreeConfigure.nonceLen)*TreeConfigure.Z];

					int k = 0;

					for(int i = 0; i < TreeConfigure.Z*cntr; i++) {
						byte[] blk = blockList.get(nextBlock++).toByteArray();
						for(int j = 0; j < (TreeConfigure.extDataSize+TreeConfigure.nonceLen); j++)
							b[k++] = blk[j];
					}

					objectKey = Integer.toString(oramID)+"#" +  Integer.toString(fileLabel);
					upload = new UploadObject(reqId, objectKey,new SimpleDataItem(b));
					upload.setObjectOffset((fileOffset)*TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen));
					upload.setObjectSize(TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen)*cntr);
					ScheduledOperation sop = storedTree.uploadObject(upload);
					fileOffset = 0;
					fileLabel += 1;
					cntr=0;
					v.add(sop);
				}

				temp++;
				cntr++;
			}

			byte[] b = new byte[cntr*(TreeConfigure.extDataSize+TreeConfigure.nonceLen)*TreeConfigure.Z];
			for(int i = 0, k = 0; i < TreeConfigure.Z*cntr; i++) {
				byte[] blk = blockList.get(nextBlock++).toByteArray();
				for(int j = 0; j < (TreeConfigure.extDataSize+TreeConfigure.nonceLen); j++)
					b[k++] = blk[j];
			}

			objectKey = Integer.toString(oramID)+"#" +  Integer.toString(fileLabel);
			upload = new UploadObject(reqId, objectKey,new SimpleDataItem(b));
			upload.setObjectOffset((fileOffset)*TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen));
			upload.setObjectSize(TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen)*cntr);
			ScheduledOperation sop = storedTree.uploadObject(upload);
			v.add(sop);


			/* upload buckets after wrap around from start of level */
			if(bucketsFromStart > 0) {
				temp = 0;
				fileLabel = Math.floorDiv(startingOffsetAtLevel,TreeConfigure.bucketLimitPerFile);
				fileOffset = (startingOffsetAtLevel)%(TreeConfigure.bucketLimitPerFile);
				cntr = 0;

				while(temp < bucketsFromStart) {

					if(temp + fileOffset >= TreeConfigure.bucketLimitPerFile) {


						b = new byte[cntr*(TreeConfigure.extDataSize+TreeConfigure.nonceLen)*TreeConfigure.Z];

						int k = 0;

						for(int i = 0; i < TreeConfigure.Z*cntr; i++) {
							byte[] blk = blockList.get(nextBlock++).toByteArray();
							for(int j = 0; j < (TreeConfigure.extDataSize+TreeConfigure.nonceLen); j++)
								b[k++] = blk[j];
						}

						objectKey = Integer.toString(oramID)+"#" +  Integer.toString(fileLabel);
						upload = new UploadObject(reqId, objectKey, new SimpleDataItem(b));
						upload.setObjectOffset((fileOffset)*TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen));
						upload.setObjectSize(TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen)*temp);
						sop = storedTree.uploadObject(upload);
						fileOffset = 0;
						fileLabel += 1;
						cntr = 0;
						v.add(sop);
					}

					cntr++;
					temp++;
				}

				b = new byte[cntr*(TreeConfigure.extDataSize+TreeConfigure.nonceLen)*TreeConfigure.Z];
				for(int i = 0,k=0; i < TreeConfigure.Z*cntr; i++) {
					byte[] blk = blockList.get(nextBlock++).toByteArray();
					for(int j = 0; j < (TreeConfigure.extDataSize+TreeConfigure.nonceLen); j++)
						b[k++] = blk[j];
				}

				objectKey = Integer.toString(oramID)+"#" + Integer.toString(fileLabel);
				upload = new UploadObject(reqId, objectKey,new SimpleDataItem(b));
				upload.setObjectOffset((fileOffset)*TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen));
				upload.setObjectSize(TreeConfigure.Z*(TreeConfigure.extDataSize + TreeConfigure.nonceLen)*cntr);
				sop = storedTree.uploadObject(upload);
				v.add(sop);
			}
		}

		Pollable.waitForCompletion(v);
	}

	/*
	 * Read and return blocks along adjacent (on storage) path(s)
	 * leaf - starting leaf identifier
	 * batchSize - number of adjacent paths to read
	 */

	public ArrayList<Block> batchReadPaths(long reqId, int leaf,int batchSize)
	{
		int[] numberOfBuckets = new int[TreeConfigure.D+1];
		int[] bucketLabel = new int[TreeConfigure.D+1];
		for (int i = TreeConfigure.D; i >= 0; i--)
		{
			if(i == 0)
				bucketLabel[i] = 0;
			else
				bucketLabel[i] = (leaf)%((int) Math.pow(2, i));


			numberOfBuckets[i] = ((int) Math.min(Math.pow(2, i),batchSize));
		}

		return downloadChunk(reqId, bucketLabel, numberOfBuckets);
	}

	/***
	 *
	 * Fetch range by client request, then do filtering
	 * int: begining leaf path
	 * array of location request
	 * batch size
	 * return the list of blocks, just 2^r
	 */
	public ArrayList<Block> batchFetchRange_Edge(long reqId, int leaf, ArrayList<PhysicalLocation> reqLocations, int batchSize)
	{

		int[] numberOfBuckets = new int[TreeConfigure.D+1]; //number to read at
		int[] bucketLabel = new int[TreeConfigure.D+1];

		/* Read corresponding paths from the tree */ //looping to the tree level and find out min 2^r blocks per level
		for (int i = TreeConfigure.D; i >= 0; i--)
		{
			if(i == 0)
				bucketLabel[i] = 0;
			else
				bucketLabel[i] = (leaf)%((int) Math.pow(2, i));

			numberOfBuckets[i] = ((int) Math.min(Math.pow(2, i),batchSize)); //number of bucket at each level to read
		}

		/// then do filering wants to access at each level
		ArrayList<Block> blockListFetch = downloadChunk(reqId, bucketLabel, numberOfBuckets);
		ArrayList<Block> result = new ArrayList<Block>();

		//filter the all blocks in the buckets in the range to retrieve at the location
		for(int i=0; i < reqLocations.size();i++){

			boolean test= false;
			int expectedPath = reqLocations.get(i).getLeafLabel() % (int) Math.pow(2, reqLocations.get(i).getBucketLevel() -1);

			for(int j=0; j < blockListFetch.size();j++)
			{
				Block blk  = blockListFetch.get(j);
				int retrievedPath = blk.treeLabel % (int) Math.pow(2, blk.bucketLevel-1);

				if (expectedPath == retrievedPath &&
						blk.bucketLevel == reqLocations.get(i).getBucketLevel() &&
						blk.bucketOffset == reqLocations.get(i).getBucketOffset() &&
						test ==false) {

					result.add(new Block(blk));
					test = true;

					blockListFetch.remove(j);
					j--;
				}
			}

			if(test==false){
				System.out.println("Could not find: l: " + reqLocations.get(i).getLeafLabel() +
						" bu: " + reqLocations.get(i).getBucketLevel() + " o: " + reqLocations.get(i).getBucketOffset());
			}

		}

		//when query on storage,uploadChunk, check (bu level, then path %, and index(offset) before writing)
		//if(leafLabel%Math.pow(2, i) == union.get(j).treeLabel%Math.pow(2, i))
		//in downloadchunk, also do the same, as same bu, path%, offset the same



		return result;
	}

	public ArrayList<Block_Transfer_CE[]> batchFetch_DPF_Range_Edge(long reqId, int leaf, ArrayList<PathOffsetSelection> reqSelections, int batchSize)
	{

		int[] numberOfBuckets = new int[TreeConfigure.D+1]; //number to read at
		int[] bucketLabel = new int[TreeConfigure.D+1]; //the starting to read at a certain level
		/* Read corresponding paths from the tree */ //looping to the tree level and find out min 2^r blocks per level
		for (int i = TreeConfigure.D; i >= 0; i--)
		{
			if(i == 0)
				bucketLabel[i] = 0;
			else
				bucketLabel[i] = (leaf)%((int) Math.pow(2, i));

			numberOfBuckets[i] = ((int) Math.min(Math.pow(2, i),batchSize)); //number of bucket at each level to read
		}

		ArrayList<Block> blockListFetch = downloadChunk(reqId, bucketLabel, numberOfBuckets);

		//filter the all  blocks in the fetched buckets in the range to retrieve at the location for DPF evaluation
		//each pathoffset selection will return logN (D blocks)
		ArrayList<Block_Transfer_CE[]> result = new ArrayList<Block_Transfer_CE[]>();


		for(int reqS=0; reqS < reqSelections.size();reqS++){
			//given a path and offset - looping though each level to select one block in the blockListFetch with the same offset
			Block_Transfer_CE[] candidates = new Block_Transfer_CE[TreeConfigure.D+1];

			//looping to each level
			for (int i = TreeConfigure.D; i >= 0; i--)
			{
				int expectedPath = reqSelections.get(reqS).getLeafPath() %((int) Math.pow(2, i)); //i is the current iterative level

				boolean test= false;

				//filter the all blocks in the buckets in the range to retrieve exacted block at the  location with offset,
				for(int j=0; j < blockListFetch.size();j++)
				{
					if(test==false) {
						Block blk  = blockListFetch.get(j);
						int retrievedPath = blk.treeLabel % (int) Math.pow(2,i);

						if (expectedPath == retrievedPath &&
								blk.bucketOffset == reqSelections.get(reqS).getBucketOffset()) {

							//-reassign a new block; note that i= bucketLevel - 1
							int index= this.stashSize  + (i*TreeConfigure.Z) +  blk.bucketOffset;

							candidates[i] = new Block_Transfer_CE(blk.data, reqSelections.get(reqS).getTag(), index, blk.r);

							test = true;

							blockListFetch.remove(j);
							j--;
						}
					}
				}
			}

			result.add(candidates);
		}

		return result;
	}

	//note: batchSize should be only 2^r
	//reqBlockTags: the tags for each block in this 2^r paths
	public ArrayList<Block_Transfer_CE> batchReadPaths_Evict_Edge(long reqId, int leaf, ArrayList<BlockTagLocation> reqBlockTags, int batchSize)
	{
		int[] numberOfBuckets = new int[TreeConfigure.D+1];
		int[] bucketLabel = new int[TreeConfigure.D+1];
		for (int i = TreeConfigure.D; i >= 0; i--)
		{
			if(i == 0)
				bucketLabel[i] = 0;
			else
				bucketLabel[i] = (leaf)%((int) Math.pow(2, i));

			numberOfBuckets[i] = ((int) Math.min(Math.pow(2, i),batchSize));
		}

		ArrayList<Block> blockListFetch = downloadChunk(reqId, bucketLabel, numberOfBuckets);

		//filter the all  blocks in the fetched buckets in the range to retrieve at the location
		ArrayList<Block_Transfer_CE> result = new ArrayList<Block_Transfer_CE>();

		//filter the blocks in the fetched buckets
		for(BlockTagLocation meta : reqBlockTags){

			boolean test= false;
			int expectedPath = meta.getLeafPath() % (int) Math.pow(2, meta.getBucketLevel() -1);

			for(int j=0; j < blockListFetch.size();j++)
			{
				Block blk  = blockListFetch.get(j);
				int retrievedPath = blk.treeLabel % (int) Math.pow(2, blk.bucketLevel-1);

				if (expectedPath == retrievedPath &&
						meta.getBucketLevel() == blk.bucketLevel &&
						meta.getBucketOffset() == blk.bucketOffset &&
						test ==false) {

					//-reassign a new block; note that i= bucketLevel - 1
					int index= this.stashSize  + (blk.bucketLevel-1)*TreeConfigure.Z +  blk.bucketOffset;

					result.add(new Block_Transfer_CE(blk.data, meta.getTag(),index,blk.r));
					test = true;

					blockListFetch.remove(j);
					j--;
				}
			}

			if(test==false){
				System.out.println("Could not find: l: " + meta.getLeafPath() +
						" bu: " + meta.getBucketLevel() + " o: " + meta.getBucketOffset());
			}

		}

		return result;
	}


	public void batchUploadRange_Edge(long reqId,int startLeaf,ArrayList<ArrayList<Block> > aList,int rangeSize){

		//need to supply startingBucket[], int numOfBuckets[], ArrayList<Block> blockList
		//where starting bucket[i] is the value of the smallest path at level i % (Int)pow (2,i) at level i
		//numberofbucket[i], is the number of buckets, = (#blocks at that level[i]/Z)
		//blockList is the array by the order, starting from depth to root

		int [] startingBucket = new int [TreeConfigure.D+1];
		int [] numOfBuckets = new int [TreeConfigure.D+1];


		int leafNum = (int) Math.pow(2,TreeConfigure.D);
		ArrayList<Block> final_blocks = new ArrayList<Block>();


		//re-arrange blocks in block_content_list to flaten blocks using block's meta data
		for(int level = TreeConfigure.D; level >= 0; level--) {

			int cur_buck_num = 0;

			//starting eviction in a batch of range in the current leaveLabel
			for(int l=0; l < (int)Math.min((int)Math.pow(2,level),rangeSize); l++){

				int leafLabel = (startLeaf+l)%leafNum;
				//identify blocks in the arraylist aList.get(l) that has the same leafLabel, then sort by offset
				ArrayList<Block> filter  =   new ArrayList<Block>();

				for(int j = 0 ; j < aList.get(level).size(); j++){
					if ((aList.get(level).get(j).treeLabel % (int)Math.pow(2,level)) == leafLabel){
						filter.add(new Block(aList.get(level).get(j)));

						//remove the object from aList.get(level)
						aList.get(level).remove(j);
						j--;
					}
				}

				Collections.sort(filter, new SortbyOffset());
				//test filter contains Z blocks
				if(filter.size() != TreeConfigure.Z){
					System.out.println("Not enought blocks to fil up level " + level + " lealLabel " + leafLabel);
				}

				//store in the flat array
				final_blocks.addAll(filter);

				cur_buck_num++;
			}

			//for this tree level i, set bucketLabel[i] is the starting location to modify in the tree's memory
			if(level > 0) { //get the current path of this bucket
				startingBucket[level] = (int) (startLeaf% (int)Math.pow(2,level));
			}else {
				startingBucket[level] = 0; //set the path for the root is 0
			}

			//update the number of bucket to be written into the tree's memory at this level
			numOfBuckets[level] = cur_buck_num;

		}

		uploadChunk(1, startingBucket, numOfBuckets, final_blocks);


	}

	class SortbyOffset implements Comparator<Block>
	{
		// Used for sorting in ascending order
		public int compare(Block a, Block b)
		{
			return a.bucketOffset - b.bucketOffset;
		}
	}
}