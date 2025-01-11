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

////////// for OblivRangeRet
public class BlockTagLocation{ //this is used for OblivRangeRet as input
            private BitSet tag;
			private int leafLabel=0;
			private int bucketLevel=1;
            private int bucketOffset=0;   
            
            public BlockTagLocation(BitSet _t, int _leafPath, int _bucketLevel, int _bucketOffset){
                tag = _t;
                leafLabel = _leafPath;
                bucketLevel = _bucketLevel;
                bucketOffset = _bucketOffset;
            }

            public BitSet getTag() { return tag;}
            public int getLeafPath() { return leafLabel;}
            public int getBucketLevel(){ return bucketLevel;}
            public int getBucketOffset(){ return bucketOffset;}

}