package RORAMConfig;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

public class Block {

    public BitSet data;	//size = dataSize		
    public int treeLabel;
    public int bucketLevel; //if it is in the Stash,bucketLevel = 0, and Offser is the stash index
    public int bucketOffset;
    public byte[] r; //sizeOf nonceLen, this is later to store the nonce

    
    public Block(Block blk) {
        assert (blk.data != null) : "no BitSet data pointers is allowed to be null.";
        try { data = (BitSet) blk.data.clone(); } 
        catch (Exception e) { e.printStackTrace(); System.exit(1); }

        treeLabel = blk.treeLabel;

        bucketLevel = blk.bucketLevel;
        bucketOffset = blk.bucketOffset;

        r = blk.r;
    }
    
    public Block(BitSet data, int label, int bLevel, int bOffset, byte[] _r) {
        assert (data != null) : "Null BitSet data pointer.";
        
        this.data = data;

        this.treeLabel = label;
        this.bucketLevel = bLevel;
        this.bucketOffset = bOffset;

        if(_r==null){
            r = new byte[TreeConfigure.nonceLen];
        } else{
            r = _r;
        }

    }
    
    public Block() {
        data = new BitSet(TreeConfigure.B*8); //each byte =  8 bit
        data.clear();

        treeLabel = 0;
        bucketLevel = 1;
        bucketOffset = 0;

        r = new byte[TreeConfigure.nonceLen];
    }
    
    public Block(byte[] bytes) {

        byte[] bs = new byte[TreeConfigure.B];
        ByteBuffer bb = ByteBuffer.wrap(bytes); //.order(ByteOrder.LITTLE_ENDIAN);

        //extract data
        bb = bb.get(bs); 
        data = BitSet.valueOf(bs);

        treeLabel = bb.getInt();
        bucketLevel = bb.getInt();
        bucketOffset = bb.getInt();


        r = new byte[TreeConfigure.nonceLen];
        bb.get(r);
    }

    public Block(byte[] bytes,  int cur_path, int cur_level, int offset ) {

        byte[] bs = new byte[TreeConfigure.B];
        ByteBuffer bb = ByteBuffer.wrap(bytes); //.order(ByteOrder.LITTLE_ENDIAN);

        //extract data
        bb = bb.get(bs);
        data = BitSet.valueOf(bs);

        r = new byte[TreeConfigure.nonceLen];

        //update new data from retrieving - path, level, offset
        treeLabel = cur_path;
        bucketLevel = cur_level;
        bucketOffset = offset;

    }

    public void erase() { 
        
        data.clear(); //reset to 0000

        treeLabel = 0; 				
        bucketLevel = 1;
        bucketOffset = 0;
    }
    
    
    public byte[] toByteArray() {
        ByteBuffer bb = ByteBuffer.allocate(TreeConfigure.extDataSize+TreeConfigure.nonceLen);

        // convert data into a byte array of length 'dataSize'
        byte[] d = new byte[TreeConfigure.B];
        byte[] temp = data.toByteArray();
        for (int i=0; i<temp.length; i++) {
            d[i] = temp[i];
        }
        
        bb.put(d);
        
        bb.putInt(treeLabel).putInt(bucketLevel).putInt(bucketOffset);

        bb.put((r == null) ? new byte[TreeConfigure.nonceLen] : r);
            
        return bb.array();
    }

    public byte[] toByteArray2() {
        ByteBuffer bb = ByteBuffer.allocate(12);

        
        bb.putInt(treeLabel).putInt(bucketLevel).putInt(bucketOffset);
            
        return bb.array();
    }

    
    public String toString() {return Arrays.toString(toByteArray());}

    public String meta_toString() {return Arrays.toString(toByteArray2());}
}
