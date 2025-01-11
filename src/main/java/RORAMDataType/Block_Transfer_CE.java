package RORAMDataType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
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

public class Block_Transfer_CE { //this is used for ObliviousRangeEval as output

    private BitSet data;
    private BitSet tag;
    private int index;

    private byte[] r; // random nonce

    public Block_Transfer_CE(BitSet _d, BitSet _t, int _i,  byte[] _r) {

        data = _d;
        tag = _t;
        index = _i;

        r = _r;
    }

    public BitSet getData() { return data; }
    public BitSet getTag(){ return tag;}
    public int getIndex(){return index;}
    public byte[] getRandom(){return r;}

}

