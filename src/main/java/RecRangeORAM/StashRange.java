package RecRangeORAM;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;


import RORAMConfig.Block;


public class StashRange {
    private Vector<Block> blocks;
    private int total_size; // in blocks
    private int oRAMID;
    private int current_size;

    public StashRange(int oRAMID, int size) 
    {
        System.out.println("\tBuildStash: r-> " + oRAMID + ", stash size -> "  + size);
        // use list of blocks (in memory)
        this.oRAMID  = oRAMID;

        //this.blocks.clear();
        this.blocks = new Vector<Block>(size);

        this.total_size = size;
        this.current_size = 0;

        for(int j=0; j < total_size; j++){
            this.blocks.add(new Block());
        }
    }

    public void insert(int index, Block _a) {
        //os.writeInt(blocks.size());
        //for(int i=0; i<blocks.size(); i++)
        //{
        //    Block blk = blocks.get(i);
        //    byte[] data = blk.toByteArray();
        //    os.writeInt(data.length);
        //    os.write(data);

        blocks.set(index, _a);
        this.current_size++;

    }

    public Block retrieve(int index){
        this.current_size--;
        if(this.current_size < 0) {
            this.current_size = 0;
        }
        return this.blocks.get(index);
    }

    //protected Tree.Block[] getStash(long reqID, Tree tr){
		
		//Vector<ScheduledOperation> v = new Vector<ScheduledOperation>();
		//Tree.Block[] stash = new Tree.Block[tr.stash.size];
        //
		//for (int i = 0; i < tr.stash.size; i++)
		//{
		//	Tree.Block blk = null;
		//	if(i < tr.stash.blocks.size()) { blk = tr.stash.blocks.get(i); }
		//	else { blk = tr.new Block(); }
		//	stash[i] = blk;
		//}

        //referesh the block and reduce current_size--
		//return stash;	
	//}


    public int getCurrentCache(){
        return this.current_size;
    }

    public int getSize(){
        return this.total_size;
    }
}
