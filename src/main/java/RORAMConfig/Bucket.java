package RORAMConfig;

import java.nio.ByteBuffer;

public class Bucket {

    public Block[] blocks; 
        
    public Bucket(Block b) { //assign the block b at the beginning, the rest are dummy-empty
        assert (b != null) : "No null block pointers allowed.";
        
        blocks = new Block[TreeConfigure.Z];
        blocks[0] = b;
        for (int i = 1; i < TreeConfigure.Z; i++)
            blocks[i] = new Block();
    }
        
    public Bucket(byte[] array) {
        
        blocks = new Block[TreeConfigure.Z];

        ByteBuffer bb = ByteBuffer.wrap(array);
        byte[] temp = new byte[TreeConfigure.extDataSize+TreeConfigure.nonceLen];
        for (int i = 0; i < TreeConfigure.Z; i++) {
            bb.get(temp);
            blocks[i] = new Block(temp);
        }
    }
        
    public Bucket(byte[] array, int cur_path, int cur_level) {
        
        blocks = new Block[TreeConfigure.Z];

        ByteBuffer bb = ByteBuffer.wrap(array);
        byte[] temp = new byte[TreeConfigure.extDataSize+TreeConfigure.nonceLen];
        for (int i = 0; i < TreeConfigure.Z; i++) {
            bb.get(temp);
            blocks[i] = new Block(temp,cur_path,cur_level,i);
        }
    }

    public byte[] toByteArray() {
        ByteBuffer bb = ByteBuffer.allocate(TreeConfigure.Z * (TreeConfigure.extDataSize+TreeConfigure.nonceLen));
        for (Block blk : blocks)
            bb.put(blk.toByteArray());
        
        return bb.array();
    }
}
