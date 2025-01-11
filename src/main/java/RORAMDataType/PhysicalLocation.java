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


////////// for Client Fetch operation
public class PhysicalLocation 
		{
			private int leafLabel=0;
			private int bucketLevel=1;
			private int bucketOffset=0;

			public PhysicalLocation(int path, int level, int offset){
				leafLabel=path;
				bucketLevel=level;
				bucketOffset=offset;
			}
			public int getLeafLabel() { return leafLabel;}
			public int getBucketLevel() { return bucketLevel;}
			public int getBucketOffset(){ return bucketOffset;}

        }
        

