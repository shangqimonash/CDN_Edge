import RORAMConfig.TreeConfigure;
import thrift.generated.EdgeServer;
import thrift.generated.tPhysicalLocation;
import thrift.generated.tFullBlockContent;

import java.util.*;

import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import Interfaces.ExternalStorageInterface;
import RORAMConfig.Block;
import RecRangeORAM.RangeStorage;
import backend.LocalStorageSingleFile;
import backend.StorageAdapter;
import RecRangeORAM.StashRange;
import RORAMDataType.*;


public class EdgeServerHandler implements EdgeServer.Iface {

    Vector<StashRange> stash_vectors;
    Vector<RangeStorage> range_storages;


    public EdgeServerHandler(){
        stash_vectors = new Vector<StashRange>(6); //capacity to hold 6 Edge Servers
        range_storages = new Vector<RangeStorage>(6); //capacity to hold 6 Edge Servers

        for(int i =0; i < 6; i++){
            stash_vectors.add(null);
            range_storages.add(null);

        }
    }


    @Override
    public void send_data_message(String dataMessage) {
        System.out.println(dataMessage);
    }

    @Override
    public int setup_rORAM_Stash (int ORAM_ID, int Stash_Size, int range_Support){

        System.out.println("Init the CDN_Edge ORAM " + ORAM_ID + " stash size " + Stash_Size + " range support " + range_Support);

        //setup the folder for this rORAM[r]
        String dataDir = "data/" + String.valueOf(ORAM_ID) +"/"; // data dir
        ExternalStorageInterface si = new StorageAdapter(new LocalStorageSingleFile(dataDir, true));
        si.connect();
        RangeStorage rs = new RangeStorage(si, Stash_Size, ORAM_ID);
        range_storages.set(ORAM_ID,rs);


        //setup the Stash[r]
        StashRange sr=new StashRange(ORAM_ID,Stash_Size);
        stash_vectors.set(ORAM_ID,sr);
        return 1;
    }


    @Override
    public int stash_upload_by_oram_index(int oramIndex, java.util.List<tFullBlockContent> block_content_list, int blocknum){

        //System.out.println("Classname " + _e.encData.getClass().getName()); //arraylist

        //retrieve reference of the current object
        StashRange sr = stash_vectors.get(oramIndex);

        //looping to insert
        for(int b_index=0; b_index < blocknum ; b_index++){

            tFullBlockContent _e = block_content_list.get(b_index);  // _e.encData is a java.nio.ByteBuffer

            BitSet data = new BitSet(RORAMConfig.TreeConfigure.B*8);
            byte[] original = new byte[RORAMConfig.TreeConfigure.B];

            //if(oramIndex==0){
            //    System.out.println("_e.encData size " + _e.encData.size());
            //    System.out.println("_e.encData " + _e.encData);
            //}

            int offset = _e.encData.size() - RORAMConfig.TreeConfigure.B;
            for (int i=0; i< RORAMConfig.TreeConfigure.B; i++) { //not include null character
                original[i] =  _e.encData.get(i+offset).byteValue(); //read from the last buffer

                //if(oramIndex==0){
                //    System.out.print(original[i]);
                //}
            }
            //System.out.println("\n");

            data = BitSet.valueOf(original);

            Block tmp = new Block(data,_e.leafLabel,_e.bucketLevel,_e.bucketOffset,null);
            sr.insert(_e.bucketOffset,tmp);

            //for testing
            //if(oramIndex == 0){
            //    Block tmp_check = new Block(sr.retrieve(_e.bucketOffset));
            //    byte[] d = new byte[RORAMConfig.TreeConfigure.B];

            //    byte[] temp = tmp_check.data.toByteArray();
            //    for (int i=0; i<temp.length; i++) {
            //        d[i] = temp[i];
            //    }

            //for (int i=0; i< RORAMConfig.TreeConfigure.B; i++) {
            //    System.out.print(d[i]);
            //}
            //System.out.println("\nComparing array " + Arrays.equals(original, d));
            //}
            //end testing
        }


        //System.out.println("\nStash " + oramIndex + " current size " + stash_vectors.get(oramIndex).getCurrentCache());
        return 1;
    }

    @Override
    public java.util.List<tFullBlockContent> stash_fetch_by_locations (int oramIndex, java.util.List<tPhysicalLocation> location_list, int blocknum){

        //print stash size
        //for(int f=0; f < stash_vectors.size();f++){
        //    System.out.println("Stash[" + f + "] size is " + stash_vectors.get(f).getSize());
        //}

        java.util.List<tFullBlockContent> result = new  Vector<tFullBlockContent>(blocknum);

        //retrieve reference of the current object
        StashRange sr = stash_vectors.get(oramIndex);

        //looping to insert
        for(int b_index=0; b_index < blocknum ; b_index++){

            Block tmp = new Block(sr.retrieve(location_list.get(b_index).bucketOffset));

            tFullBlockContent serialised_tmp = new tFullBlockContent();
            serialised_tmp.leafLabel = tmp.treeLabel;
            serialised_tmp.bucketLevel = tmp.bucketLevel;
            serialised_tmp.bucketOffset = tmp.bucketOffset;

            Byte[] init_d = new Byte[RORAMConfig.TreeConfigure.B];
            Arrays.fill(init_d, (byte) 0);
            java.util.List<Byte> d = Arrays.asList(init_d);
            byte[] temp = tmp.data.toByteArray();
            for (int i=0; i<temp.length; i++) {
                d.set(i, temp[i]);
            }

            serialised_tmp.encData = d;

            //add to the list
            result.add(serialised_tmp);

        }

        return result;
    }



    @Override
    public java.util.List<tFullBlockContent>  edge_fetch_by_locations(int oramIndex, java.util.List<tPhysicalLocation> location_list, int startLeaf, int blocknum){

        //blocknum is the number of queries in location_list, i.e.,  location_list.size()

        int rangeSize =  (int) Math.pow(2,oramIndex); //number of ORAM paths to read, starting from startLeaf

        System.out.println("Retrieve from rORAM[" + oramIndex + "]");

        //data conversion from tPhysicalLocation -> PhysicalLocation
        ArrayList<PhysicalLocation> reqLocations = new ArrayList<PhysicalLocation>();
        for(int v=0; v < blocknum;v++){
            System.out.println("l->" + location_list.get(v).leafLabel + " bu->" + location_list.get(v).bucketLevel + " o->" + location_list.get(v).bucketOffset);

            PhysicalLocation pl = new PhysicalLocation(location_list.get(v).leafLabel , location_list.get(v).bucketLevel, location_list.get(v).bucketOffset);
            reqLocations.add(pl);
        }

        long startTime = System.currentTimeMillis();

        ArrayList<Block> blockListFetch = range_storages.get(oramIndex).batchFetchRange_Edge(1,startLeaf,reqLocations,rangeSize);


        long endTime = System.currentTimeMillis();

        System.out.println("edge_fetch_by_locations: Random workload: Completed "+ blocknum + " queries in " + (endTime-startTime) + " ms -- "
                + (blocknum*1000/(endTime-startTime)) + " queries per second");


        java.util.List<tFullBlockContent> result = new  Vector<tFullBlockContent>(blocknum);

        //data conversion to tFullBlockContent
        for(int b_index=0; b_index < location_list.size() ; b_index++){

            tFullBlockContent serialised_tmp = new tFullBlockContent();
            serialised_tmp.leafLabel = blockListFetch.get(b_index).treeLabel;
            serialised_tmp.bucketLevel = blockListFetch.get(b_index).bucketLevel;
            serialised_tmp.bucketOffset = blockListFetch.get(b_index).bucketOffset;

            Byte[] init_d = new Byte[RORAMConfig.TreeConfigure.B];
            Arrays.fill(init_d, (byte) 0);
            java.util.List<Byte> d = Arrays.asList(init_d);
            byte[] temp = blockListFetch.get(b_index).data.toByteArray();
            for (int i=0; i<temp.length; i++) {
                d.set(i, temp[i]);
            }

            serialised_tmp.encData = d;

            //add to the list
            result.add(serialised_tmp);

        }

        return result;

    }


    @Override
    public int edge_upload_by_locations(int oramIndex, java.util.List<tFullBlockContent> block_content_list, int startLeaf, int blocknum){

        //blocknum is the number of queries in location_list, i.e.,  block_content_list.size()

        int rangeSize =  (int) Math.pow(2,oramIndex); //number of ORAM paths to read, starting from startLeaf
        System.out.println("Writing " + rangeSize + " paths from rORAM[" + oramIndex + "]");

        //init arraylist of blocks for each level
        ArrayList<ArrayList<Block> > aList = new ArrayList<ArrayList<Block> >(TreeConfigure.D+1); //create [level e][array of blocks later to be sorted by path]
        for(int level = TreeConfigure.D; level >=0; level--)
            aList.add(new ArrayList<Block>());


        for(int b_index=0; b_index < block_content_list.size() ; b_index++){

            //convert to byte array
            byte[] serialised_data = new byte[RORAMConfig.TreeConfigure.B];
            for(int i = 0; i < RORAMConfig.TreeConfigure.B; i++){
                serialised_data[i] = block_content_list.get(b_index).encData.get(i).byteValue();
            }

            //keep the same bucket level of the block
            Block blk = new Block(serialised_data, block_content_list.get(b_index).leafLabel, block_content_list.get(b_index).bucketLevel, block_content_list.get(b_index).bucketOffset);
            aList.get(blk.bucketLevel-1).add(blk);
        }

        long startTime = System.currentTimeMillis();

        range_storages.get(oramIndex).batchUploadRange_Edge(1,startLeaf,aList,rangeSize);

        long endTime = System.currentTimeMillis();

        System.out.println("edge_upload_by_locations: Random workload: Completed "+ blocknum + " queries in " + (endTime-startTime) + " ms -- "
                + (blocknum*1000/(endTime-startTime)) + " queries per second");

        return 1;
    }

}