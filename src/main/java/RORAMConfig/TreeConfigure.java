package RORAMConfig;


public class TreeConfigure {

    //public static final int StashSize = 200;

    // set N to be the smallest power of 2	it that is bigger than 'data.length'. 
	//N = (int) Math.pow(2, Math.ceil(Math.log(maxBlocks)/Math.log(2)));
	// the number of logic blocks in the tree, number of leaf blocks
    public static final int N = 65536;//256;// number of leaf node 

    // depth of the tree
    public static final int D = Utils.bitLength(N)-1; //tree depth
		
    public static final int Z = 5;

    public static final int B = 2048;//4KB //2048;//32;//4000;// data block size 2KB 

    public static final int nonceLen = 10; // length of nonce (to encrypt each data piece) in bytes

    //extDataSize + nonceLen = blockSIze
    public static final int extDataSize = B + 4 + 4 + 4; //except the nonceLen
   	
	public static int bucketLimitPerFile = (int) Math.pow(2, 16); //65536 buckets per file
	
	public static enum OpType {Read, Write};


}
