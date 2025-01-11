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

//just re-use block as the output of direct Block request
////////// for ObliviousRangeEval


        public class PathOffsetSelection{ //this is used for ObliviousRangeEval as input
            private BitSet tag;
            private int leafPath;
            private int bucketOffset;


            public PathOffsetSelection(BitSet _t, int _leafPath, int _bucketOffset){
                tag = _t;
                leafPath = _leafPath;
                bucketOffset = _bucketOffset;
            }

            public BitSet getTag() { return tag;}
            public int getLeafPath() { return leafPath;}
            public int getBucketOffset(){ return bucketOffset;}

        }
