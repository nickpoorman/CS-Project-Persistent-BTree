package persistentbtree;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BTree {

    public Node root;
    RandomAccessFile file;
    public int TREE_SIZE = 0;
    static int NODE_ARITY = 12;
    static int order = NODE_ARITY;
    static int OFFSET = 16;
    static int EMPTY_NODE_POINTER = 8;
    static int KEY_LENGTH = 32;
    static int KEY_SIZE = KEY_LENGTH * NODE_ARITY;
    static int VALUE_LENGTH = 293;
    static int VALUE_SIZE = VALUE_LENGTH * NODE_ARITY;
    static int NODE_SIZE = 4096;
    static int PARENT_SIZE = 8;
    static int NUM_ELEMENTS_SIZE = 4;
    static int PAD = NODE_SIZE - PARENT_SIZE - NUM_ELEMENTS_SIZE - KEY_SIZE - VALUE_SIZE;
    public LinkedList<Long> emptyNodes = new LinkedList<Long>();
    static final boolean DEBUG = false;
    public boolean loading = false;
    public LinkedHashMap<String, Node> nodeCache;
    int removed = 0;

    public BTree(RandomAccessFile file) {
        nodeCache = new LinkedHashMap<String, Node>(200) {

            private static final int MAX_ENTRIES = 1000;

            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_ENTRIES;
            }
        };

        this.file = file;
        try {
            if (file.length() == 0) {
                //create the file
                createFile();
            //create the root node
            } else {
                //read in the file
                root = new Node();
                root = root.read(OFFSET);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean add(String key, String data) {
        boolean b = nativeAdd(key, data);
        if (b) {
            this.TREE_SIZE++;
            return true;
        } else {
            return false;
        }
    }

    private boolean nativeAdd(String key, String data) {
        try {
            if (key != null) {
                if (isRootNull()) {
                    root = new Node(key, data);//create a new root node
                    root.parent = -1;
                    file.seek(OFFSET);
                    root.commit(root, -1);
                    return true;
                }
                boolean needSplit = root.set(key, data);
                if (!needSplit) {
                    file.seek(OFFSET);
                    root.commit(root, -1);
                    return true;
                } else {
                    root.commit(root, -1);
                    root.splitRoot();
                    root.commit(root, -1);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setLoading(boolean l) {
        this.loading = l;
    }

    private void createFile() {
        try {
            file.seek(0);
            //create the RandomAccessFile
            //first set the tree size to zero
            file.writeInt(TREE_SIZE);

            //then set the NODE arity of the tree (amount of data entries per node)
            file.writeInt(NODE_ARITY);

            //then set the first empty node (one that has been removed)
            file.writeLong(-1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isRootNull() throws Exception {
        return file.length() <= OFFSET;
    }
    
    public int getSize(){
        return this.TREE_SIZE;
    }

    public String remove(String key) {
        try {
            if (key != null) {
                root.remove(key);
                file.seek(OFFSET);
                root.commit(root, -1);
                this.removed++;
                this.TREE_SIZE--;
                return key;
            } else {
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean contains(String key) {
        return this.searchForNode(key) != null;
    }

    public Node searchForNode(String key) {
        if (root == null && key != null) {
            return null;
        }
        return root.search(key);
    }

    public String getValue(String key) {
        if (root == null && key != null) {
            return null;
        }
        return root.getValue(key);
    }

    public static void print(Node node, int level) {
        if (node != null) {
            System.out.println(level + ": key: " + node.getKeysAsString() + " value: " + node.getValuesAsString());
            level++;
            for (int i = 0; i < node.getNumChildren(); i++) {
                print(node.getChild(i), level);
            }
        }
    }

    public String traverseKeys(Node node) {
        StringBuilder buf = new StringBuilder();
        if (node != null) {
            if (!node.hasChildren()) {
                buf.append(node.getKeysAsString());
            }
            for (int i = 0; i < node.getNumChildren(); ++i) {
                buf.append(traverseKeys(node.getChild(i)) + "\n");
            }
        }
        return buf.toString();

    }

    public String traverseValues(Node node) {
        StringBuilder buf = new StringBuilder();
        buf.append("\n");
        if (node != null) {
            if (!node.hasChildren()) {
                buf.append(node.getValuesAsString());
            }

            for (int i = 0; i < node.getNumChildren(); ++i) {
                buf.append(traverseValues(node.getChild(i)) + "\n");
            }
        }
        return buf.toString();
    }

    private class Node {

        List<String> keys;
        List<String> values;
        long parent;

        public Node(String key, String value) {
            this.keys = new LinkedList();
            this.values = new LinkedList();
            this.keys.add(key);
            this.values.add(value);
        }

        public Node() {
            this.parent = -666;
            this.keys = new LinkedList();
            this.values = new LinkedList();
        }

        public void addRemovedPointer(long pointer) {
            try {
                file.seek(EMPTY_NODE_POINTER);
                long emptyPointer = file.readLong();
                if (emptyPointer == -1) {
                    file.seek(EMPTY_NODE_POINTER);
                    file.writeLong(pointer);
                    Node removedNode = new Node();
                    removedNode.keys.add("$POINTER");
                    removedNode.values.add("$END");
                    long removedNodePointer = pointer;
                    nodeCache.put(removedNodePointer + "", removedNode);
                    this.commit(removedNode, removedNodePointer);
                } else {
                    Node removedNode = new Node();
                    removedNode.keys.add(0, "$POINTER");
                    removedNode.values.add(0, this.createPointerLocation(emptyPointer));
                    long removedNodePointer = pointer;
                    nodeCache.put(removedNodePointer + "", removedNode);
                    file.seek(EMPTY_NODE_POINTER);
                    file.writeLong(pointer);
                    this.commit(removedNode, removedNodePointer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public long popEmptyNodePointer() throws Exception {
            file.seek(EMPTY_NODE_POINTER);
            long emptyPointer = file.readLong();
            if (emptyPointer == -1) {
                return -1;
            } else {
                Node topNode = this.getNode(emptyPointer);
                if (!topNode.values.get(0).equals("$END")) {
                    String nodePointer = topNode.values.get(0);
                    long nextEmptyNode = this.getPointerLocation(nodePointer);
                    file.seek(EMPTY_NODE_POINTER);
                    file.writeLong(nextEmptyNode);
                } else {
                    file.seek(EMPTY_NODE_POINTER);
                    file.writeLong(-1);
                }
                return emptyPointer;
            }
        }

        public void commit(Node node, long seek) {
            try {
                //write the parent pointer to the file
                if (node.parent == -1 || seek == -1) {
                    file.seek(OFFSET);
                    //write parent
                    file.writeLong(-1);
                } else {
                    file.seek(seek);
                    //write parent
                    file.writeLong(node.parent);
                }
                //write the number of elements
                int numElements = node.keys.size();
                file.writeInt(numElements);
                //key array                
                //ByteBuffer byteBuffer = ByteBuffer.allocate(NODE_SIZE);
                //write all the keys
                for (String s : node.keys) {
                    byte[] bytes = s.getBytes();
                    file.write(bytes);
                    // byteBuffer.put(bytes);
                    //needed to pad the key                
                    int tmpPad = KEY_LENGTH - bytes.length;
                    byte[] buffer1 = new byte[tmpPad];
                    for (int i = 0; i < tmpPad; i++) {
                        buffer1[i] = ' ';
                    }
                    file.write(buffer1);
                // byteBuffer.put(buffer1);
                }
                //pad the end of the keys
                int padNumBytes = (NODE_ARITY - numElements) * KEY_LENGTH;
                byte[] buffer2 = new byte[padNumBytes];
                for (int i = 0; i < padNumBytes; i++) {
                    buffer2[i] = ' ';
                }
                file.write(buffer2);
                // byteBuffer.put(buffer2);
                //write all the values
                for (String s : node.values) {
                    byte[] bytes = s.getBytes();
                    file.write(bytes);
                    // byteBuffer.put(bytes);
                    //needed to pad the key
                    int tmpPad = VALUE_LENGTH - bytes.length;
                    byte[] buffer3 = new byte[tmpPad];
                    for (int i = 0; i < tmpPad; i++) {
                        buffer3[i] = ' ';
                    }
                    file.write(buffer3);
                // byteBuffer.put(buffer3);
                // file.seek(pointer2 + VALUE_LENGTH);
                }
                //pad the end of the values
                int padNumBytes2 = (NODE_ARITY - numElements) * VALUE_LENGTH;
                byte[] buffer4 = new byte[padNumBytes2];
                for (int i = 0; i < padNumBytes2; i++) {
                    buffer4[i] = ' ';
                }
                file.write(buffer4);
                // byteBuffer.put(buffer4);                
                //pad the end of the node to make the node = 4096 bytes (4k)
                byte[] buffer5 = new byte[PAD];
                for (int i = 0; i < PAD; i++) {
                    buffer5[i] = ' ';
                }
                file.write(buffer5);
                //  byteBuffer.put(buffer5);


                //write the ByteBuffer to disk
                // byteBuffer.rewind();

//                FileChannel outChannel = file.getChannel();
//                outChannel.position(file.getFilePointer());
//                outChannel.write(byteBuffer);
//                byte[] finalBytes = new byte[NODE_SIZE];
//                for (int i = 0; i < NODE_SIZE; ++i) {
//                    finalBytes[i] = byteBuffer.get(i);
//                }
//                file.write(finalBytes);
                //add it to the cache (or update it)
                nodeCache.put(seek + "", node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public Node read(long seek) {
            try {
                Node tmp = new Node();
                file.seek(seek);
                //read and set parent
                tmp.parent = file.readLong();
                //read number of elements
                int numElements = file.readInt();
                long pointer = file.getFilePointer();
                //read and set keys
                for (int i = 0; i < numElements; i++) {
                    byte[] inBytes = new byte[KEY_LENGTH];
                    file.readFully(inBytes, 0, KEY_LENGTH);
                    String s = new String(inBytes);
                    s = s.trim();
                    tmp.keys.add(s);
                }
                //seek to the end of keys
                long valuePointer = pointer + KEY_SIZE;
                file.seek(valuePointer);
                //read the values
                for (int i = 0; i < numElements; i++) {
                    byte[] inBytes = new byte[VALUE_LENGTH];
                    file.readFully(inBytes, 0, VALUE_LENGTH);
                    String s = new String(inBytes);
                    s = s.trim();
                    tmp.values.add(s);
                }
                return tmp;
            } catch (Exception e) {
                System.out.println("Error seek is: " + seek);
                e.printStackTrace();
                return null;
            }
        }

        private boolean hasUnderflow() {
            int half = (int) Math.ceil(1.0 * BTree.order / 2);
            return this.keys.size() < half;
        }

        public Node getNode(long address) {
            Node n;
            if (nodeCache.containsKey(address + "")) {
                n = nodeCache.get(address + "");
            } else {
                n = this.read(address);
            }
            return n;
        }

        private String getLeftMostKey(Node n) {
            Node tmp = n;
            while (tmp.hasChildren()) {
                long nPointerLocation = this.getPointerLocation(tmp.values.get(0));
                tmp = this.getNode(nPointerLocation);
            }
            return tmp.keys.get(0);

        }

        private int numberExtra() {
            int half = (int) Math.ceil(1.0 * BTree.order / 2);
            int size = this.keys.size();
            if (size > half) {
                int extra = size - half;
                int canSpare = (int) Math.ceil(1.0 * extra / 2);
                return canSpare;
            } else {
                return 0;
            }
        }

        public Node search(String key) {
            if (this.hasChildren()) {
                int ks = this.keys.size();
                for (int i = 0; i < ks; i++) {
                    if (key.equals(this.keys.get(i))) {
                        //if the key is equal to the one in the list                       
                        long nPointerLocation = this.getPointerLocation(this.values.get(i + 1));
                        Node n = this.getNode(nPointerLocation);
                        return n.search(key);
                    }
                    if (this.keys.get(i) == null || this.keys.get(i).equals("null")) {
                        break;
                    }
                    int cp = Strings.compareNatural(key, this.keys.get(i));
                    //if a key is found in the list thats greater then the key to be added
                    if (cp < 0) {
                        long nPointerLocation = this.getPointerLocation(this.values.get(i));
                        Node n = this.getNode(nPointerLocation);
                        return n.search(key);
                    }
                }
                //if the key wasnt found in the list or is greater then do something
                long nPointerLocation = this.getPointerLocation(this.values.get(this.keys.size() - 1));
                Node n = this.getNode(nPointerLocation);
                return n.search(key);
            } else {
                int ks = this.keys.size();
                for (int i = 0; i < ks; i++) {
                    if (key.equals(this.keys.get(i))) {
                        //if the key is equal to the one in the list
                        return this;
                    }
                    int cp = Strings.compareNatural(key, this.keys.get(i));
                    //if a key is found in the list thats greater then the key to be added
                    if (cp < 0) {
                        return null;
                    }
                }
                //if the key wasnt found in the list then do something
                return null;
            }
        }

        public boolean hasChildren() {
            if (this.values.size() == 0) {
                return false;
            } else {
                //if the first value is an address then it has children
                if (this.values.get(0).startsWith("$")) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        public void splitRoot() {
            boolean hadChildren = hasChildren();

            boolean fix = true;
            Node left = new Node();
            Node right = new Node();


            long leftPointer = this.getNextAvailPointer();
            nodeCache.put(leftPointer + "", left);
            long rightPointer = this.getNextAvailPointer();
            //create the right and left nodes on disk
            nodeCache.put(rightPointer + "", right);
            int half = (int) Math.ceil(1.0 * BTree.order / 2);
            int size = this.keys.size();
            for (int i = 0; i < size; i++) {
                if (i < half) {
                    left.set(this.keys.remove(0), this.values.remove(0));
                } else {
                    right.set(this.keys.remove(0), this.values.remove(0));
                }
            }
            if (hadChildren) {
                fix = false;
                //need to set the left seek location
                this.set(left.keys.set(left.keys.size() - 1, "null"), this.createPointerLocation(leftPointer));
            }
            left.parent = OFFSET;
            right.parent = OFFSET;
            if (fix) {
                this.set(right.keys.get(0), this.createPointerLocation(leftPointer));
            }
            //thinking about setting all the nulls to "$null"
            this.set("null", this.createPointerLocation(rightPointer));
            //now commit again
            this.commit(left, leftPointer);
            this.commit(right, rightPointer);
        }

        private String createPointerLocation(long pointer) {
            return "$" + pointer;
        }

        private long getPointerLocation(String pointer) {
            //returns -99 if the inputed string is not a pointer to a node
            if (pointer.startsWith("$")) {
                return Long.parseLong(pointer.substring(1));
            } else {
                return -99;
            }
        }

        private long getNextAvailPointer() {
            try {
                long nextPointer = this.popEmptyNodePointer();
                if (nextPointer == -1) {
                    long length = file.length();
                    if (!nodeCache.containsKey(length + "")) {
                        return length;
                    } else {
                        while (nodeCache.containsKey(length + "")) {
                            length = length + NODE_SIZE;
                        }
                    }
                    return length;
                } else {
                    return nextPointer;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }

        private void splitLeafNode(Node right) {
            Node left = new Node();
            long leftPointer = this.getNextAvailPointer();
            nodeCache.put(leftPointer + "", left);
            int half = (int) Math.ceil(1.0 * BTree.order / 2);
            for (int i = 0; i < half; i++) {
                left.set(right.keys.remove(0), right.values.remove(0));
            }
            //the parent pointer was a pain so i just left it out except for root
            this.set(right.keys.get(0), this.createPointerLocation(leftPointer));
            this.commit(left, leftPointer);
        }

        private void splitInternalNode(Node right) {
            Node left = new Node();
            long leftPointer = this.getNextAvailPointer();
            nodeCache.put(leftPointer + "", left);
            int half = (int) Math.ceil(1.0 * BTree.order / 2);
            for (int i = 0; i < half; i++) {
                left.set(right.keys.remove(0), right.values.remove(0));
            }
            //the parent pointer was a pain so i just left it out except for root
            String s = left.keys.set(left.keys.size() - 1, "null");
            this.set(s, this.createPointerLocation(leftPointer));
            this.commit(left, leftPointer);
        }

        private boolean needToSplit() {
            return this.keys.size() > BTree.order - 1;
        }

        private void splitChild(Node n) {
            if (!n.hasChildren()) {
                //do split leaf
                this.splitLeafNode(n);
            } else {
                //do split internal            
                this.splitInternalNode(n);
            }
        }

        public String getKeysAsString() {
            String tmp = "";
            StringBuilder buf = new StringBuilder();
            for (String s : this.keys) {
                buf.append(s + "\n");
            }
            tmp = buf.toString();
            return tmp;
        }

        public String getValuesAsString() {
            String tmp = "";
            StringBuilder buf = new StringBuilder();
            for (String s : this.values) {
                buf.append(s + "\n");
            }
            tmp = buf.toString();
            return tmp;
        }

        public String getValue(String key) {
            Node n = this.search(key);
            for (int i = 0; i < n.keys.size(); i++) {
                if (key.equals(n.keys.get(i))) {
                    return (String) n.values.get(i);
                }
            }
            return null;
        }

        public int getNumChildren() {
            return this.values.size();
        }

        public Node getChild(int i) {
            if (this.hasChildren()) {
                long nPointerLocation = this.getPointerLocation(this.values.get(i));
                Node n = this.getNode(nPointerLocation);
                return n;
            } else {
                return null;
            }
        }

        public boolean set(String key, String value) {
            if (this.keys.size() == 0) {
                this.keys.add(key);
                this.values.add(value);
                return this.needToSplit();
            } else {
                if (!this.hasChildren()) {
                    int ks = this.keys.size();
                    for (int i = 0; i < ks; i++) {
                        if (key.equals("null")) {
                            this.keys.add(key);
                            this.values.add(value);
                            return this.needToSplit();
                        }
                        int cp = Strings.compareNatural(key, this.keys.get(i));
                        //if its already there replace it
                        if (key.equals(this.keys.get(i))) {
                            this.keys.set(i, key);
                            this.values.set(i, value);
                            return this.needToSplit();
                        }
                        //if a key is found in the list thats greater then the key to be added
                        if (cp < 0) {
                            this.keys.add(i, key);
                            this.values.add(i, value);
                            return this.needToSplit();
                        }
                    }
                    //if none of the current keys are greater add at the end
                    this.keys.add(key);
                    this.values.add(value);
                    return this.needToSplit();
                } else {
                    //adds to internal nodes
                    if (value.startsWith("$")) {
                        if (key.equals("null")) {
                            //+if null is allready there then overwrite it
                            if (this.keys.get(this.keys.size() - 1).equals("null")) {
                                this.keys.set(this.keys.size() - 1, "null");
                                this.values.set(this.values.size() - 1, value);
                                return this.needToSplit();
                            } else {
                                this.keys.add("null");
                                this.values.add(value);
                                //check to see if internal needs to split
                                return this.needToSplit();
                            }
                        }
                        int ks = this.keys.size();
                        for (int i = 0; i < ks; i++) {
                            if (!this.keys.get(i).equals("null")) {
                                int cp = Strings.compareNatural(key, this.keys.get(i));
                                if (key.equals(this.keys.get(i))) {
                                    this.keys.set(i, key);
                                    this.values.set(i, value);
                                    return this.needToSplit();
                                }
                                //if a key is found in the list thats greater then the key to be added
                                if (cp < 0) {
                                    this.keys.add(i, key);
                                    this.values.add(i, value);
                                    return this.needToSplit();
                                }
                            }
                        }
                        //if none of the current keys are greater add at the end (but before null)
                        int ksize = this.keys.size();
                        for (int i = 0; i < ksize; i++) {
                            if (this.keys.get(i).equals("null")) {
                                this.keys.add(this.keys.size() - 1, key);
                                this.values.add(this.values.size() - 1, value);
                                return this.needToSplit();
                            }
                        }
                        this.keys.add(key);
                        this.values.add(value);
                        return this.needToSplit();

                    } else {
                        //if its adding a value (not adding a node to the values list in internal)
                        int ks = this.keys.size();
                        for (int i = 0; i < ks - 1; i++) {
                            int cp = Strings.compareNatural(key, this.keys.get(i));
                            if (key.equals(this.keys.get(i))) {
                                //go over one and go down all the way to left
                                long nPointerLocation = this.getPointerLocation(this.values.get(i + 1));
                                Node n = this.getNode(nPointerLocation);
                                if (n.set(key, value)) {
                                    this.splitChild(n);
                                    this.commit(n, nPointerLocation);
                                    return this.needToSplit();
                                } else {
                                    this.commit(n, nPointerLocation);
                                    return this.needToSplit();
                                }
                            }
                            if (cp < 0) {
                                long nPointerLocation = this.getPointerLocation(this.values.get(i));
                                Node n = this.getNode(nPointerLocation);
                                if (n.set(key, value)) {
                                    this.splitChild(n);
                                    this.commit(n, nPointerLocation);
                                    return this.needToSplit();
                                } else {
                                    this.commit(n, nPointerLocation);
                                    return this.needToSplit();
                                }
                            }
                        }

                        //if none of the keys in the list are greater go down the null key's child
                        long nPointerLocation = this.getPointerLocation(this.values.get(this.keys.size() - 1));
                        Node n = this.getNode(nPointerLocation);
                        if (n.set(key, value)) {
                            this.splitChild(n);
                            this.commit(n, nPointerLocation);
                            return this.needToSplit();
                        } else {
                            this.commit(n, nPointerLocation);
                            return this.needToSplit();
                        }
                    }
                }
            }
        }

        public boolean remove(String key) {
            int index = 999999;
            boolean underflow = false;
            boolean found = false;
            if (this.hasChildren()) {
                int ksize = this.keys.size();
                for (int i = 0; i < ksize - 1; i++) {
                    if (key.equals(this.keys.get(i))) {
                        //if the key is equal to the one in the list
                        long nPointerLocation = this.getPointerLocation(this.values.get(i + 1));
                        Node n = this.getNode(nPointerLocation);
                        index = i + 1;
                        underflow = n.remove(key);
                        //commit n after removing the key
                        this.commit(n, nPointerLocation);
                        found = true;
                        break;
                    }
                    if (this.keys.get(i).equals("null")) {
                        break;
                    }
                    int cp = Strings.compareNatural(key, this.keys.get(i));
                    //if a key is found in the list thats greater then the key to be added
                    if (cp < 0) {
                        long nPointerLocation = this.getPointerLocation(this.values.get(i));
                        Node n = this.getNode(nPointerLocation);
                        index = i;
                        underflow = n.remove(key);
                        //commit n after removing the key
                        this.commit(n, nPointerLocation);
                        found = true;
                        break;
                    }
                }

                //if the key wasnt found in the list or is greater then do something
                if (!found) {
                    long nPointerLocation = this.getPointerLocation(this.values.get(this.keys.size() - 1));
                    Node n = this.getNode(nPointerLocation);
                    underflow = n.remove(key);
                    //commit n after removing the key
                    this.commit(n, nPointerLocation);
                    found = true;
                    index = this.keys.size() - 1;
                }

                //Run underflow code --->
                long indexNodePointerLocation = this.getPointerLocation(this.values.get(index));
                Node indexNode = this.getNode(indexNodePointerLocation);
                //remove and then check to see if node had an underflow
                if (underflow) {
                    //if it has an underflow check immediate neighbors for extra
                    Node leftNeighbor = null;
                    Node rightNeighbor = null;
                    int leftExtra = 0;
                    int rightExtra = 0;
                    long leftNeighborPointerLocation = -999;
                    long rightNeighborPointerLocation = -999;
                    //if left neighbor exists get how many it can spare
                    if (index > 0) {
                        leftNeighborPointerLocation = this.getPointerLocation(this.values.get(index - 1));
                        leftNeighbor = this.getNode(leftNeighborPointerLocation);
                        leftExtra = leftNeighbor.numberExtra();
                    }
                    //if right neighbor exists get how many it can spare
                    if (index < this.keys.size() - 1) {
                        rightNeighborPointerLocation = this.getPointerLocation(this.values.get(index + 1));
                        rightNeighbor = this.getNode(rightNeighborPointerLocation);
                        rightExtra = rightNeighbor.numberExtra();
                    }
                    boolean stole = false;
                    //if left has some to spare steal from it
                    if (leftExtra > 0) {
                        int lnSize = leftNeighbor.keys.size();
                        int upTo = leftNeighbor.keys.size() - leftExtra;
                        for (int i = lnSize; i > upTo; i--) {
                            //if we are doing a steal on an internal node we have to fix null
                            if (leftNeighbor.hasChildren()) {
                                long nNeighborPointerLocation = this.getPointerLocation(this.values.get(index));
                                Node n = this.getNode(nNeighborPointerLocation);
                                indexNode.keys.add(0, this.getLeftMostKey(n));
                                indexNode.values.add(0, leftNeighbor.values.get(leftNeighbor.values.size() - 1));
                                this.commit(indexNode, indexNodePointerLocation);
                                long nNeighborPointerLocation2 = this.getPointerLocation(this.values.get(index));
                                Node n2 = this.getNode(nNeighborPointerLocation2);
                                this.keys.set(index - 1, this.getLeftMostKey(n2));
                                //not commiting here, not sure if its going to cause problems.... we'll see
                                leftNeighbor.keys.set(leftNeighbor.keys.size() - 2, "null");
                                leftNeighbor.keys.remove(leftNeighbor.keys.size() - 1);
                                leftNeighbor.values.remove(leftNeighbor.values.size() - 1);
                                this.commit(leftNeighbor, leftNeighborPointerLocation);
                            } else {
                                indexNode.set(leftNeighbor.keys.remove(i - 1), leftNeighbor.values.remove(i - 1));
                                this.commit(leftNeighbor, leftNeighborPointerLocation);
                                this.commit(indexNode, indexNodePointerLocation);
                            }
                        }
                        stole = true;
                        //fix anchor key
                        long nNeighborPointerLocation = this.getPointerLocation(this.values.get(index));
                        Node n = this.getNode(nNeighborPointerLocation);
                        this.keys.set(index - 1, this.getLeftMostKey(n));
                    } else {
                        //if right has some to spare steal from it
                        if (rightExtra > 0) {
                            for (int i = 0; i < rightExtra; i++) {
                                if (rightNeighbor.hasChildren()) {
                                    indexNode.keys.set(indexNode.keys.size() - 1, this.keys.get(index));
                                    this.keys.set(index, rightNeighbor.keys.get(0));
                                    indexNode.keys.add("null");
                                    indexNode.values.add(rightNeighbor.values.get(0));
                                    rightNeighbor.keys.remove(0);
                                    rightNeighbor.values.remove(0);
                                    this.commit(indexNode, indexNodePointerLocation);
                                    this.commit(rightNeighbor, rightNeighborPointerLocation);
                                } else {
                                    indexNode.set(rightNeighbor.keys.remove(0), rightNeighbor.values.remove(0));
                                    this.commit(rightNeighbor, rightNeighborPointerLocation);
                                    this.commit(indexNode, indexNodePointerLocation);
                                }
                            }
                            stole = true;
                            long nNeighborPointerLocation = this.getPointerLocation(this.values.get(index + 1));
                            Node n = this.getNode(nNeighborPointerLocation);
                            this.keys.set(index, this.getLeftMostKey(n));
                            if (index > 0) {
                                long nNeighborPointerLocation2 = this.getPointerLocation(this.values.get(index));
                                Node n2 = this.getNode(nNeighborPointerLocation2);
                                this.keys.set(index - 1, this.getLeftMostKey(n2));
                            }
                        }
                    }
                    //if neither neighbor had any to spare then merge with a neighbor
                    if (!stole) {
                        //if left neighbor exists merge with left
                        if (index > 0) {
                            if (leftNeighbor.hasChildren()) {
                                //the left neighbor has children we have to fix some stuff
                                leftNeighbor.keys.set(leftNeighbor.keys.size() - 1, this.getLeftMostKey((Node) indexNode));
                                this.commit(leftNeighbor, leftNeighborPointerLocation);
                            }
                            int ks = indexNode.keys.size();
                            for (int i = 0; i < ks; i++) {
                                //take all the values from the right and add them to the left
                                leftNeighbor.set(indexNode.keys.get(i), indexNode.values.get(i));
                                this.commit(leftNeighbor, leftNeighborPointerLocation);
                            }
                            this.keys.set(index - 1, this.keys.get(index));
                            this.addRemovedPointer(this.getPointerLocation(this.values.get(index)));
                            this.keys.remove(index);
                            this.values.remove(index);
                            if (this.parent == -1 && this.keys.size() < 2) {
                                root = leftNeighbor;
                                root.parent = -1;
                                root.commit(root, -1);
                                this.addRemovedPointer(leftNeighborPointerLocation);
                            }

                        } else {
                            //if left neighbor didn't exist and right neighbor exists merge with right
                            if (index < this.keys.size() - 1) {
                                if (rightNeighbor.hasChildren()) {
                                    //the right neighbor has children we have to fix some stuff
                                    long nNeighborPointerLocation = this.getPointerLocation(this.values.get(index + 1));
                                    Node n = this.getNode(nNeighborPointerLocation);
                                    indexNode.keys.set(indexNode.keys.size() - 1, this.getLeftMostKey(n));
                                    this.commit(indexNode, indexNodePointerLocation);
                                }
                                int ks = indexNode.keys.size();
                                for (int i = 0; i < ks; i++) {
                                    //take all the values from the left and add them to the right
                                    rightNeighbor.set(indexNode.keys.get(i), indexNode.values.get(i));
                                    this.commit(rightNeighbor, rightNeighborPointerLocation);
                                }
                                this.addRemovedPointer(this.getPointerLocation(this.values.get(index)));
                                this.keys.remove(index);
                                this.values.remove(index);
                                if (this.parent == -1 && this.keys.size() < 2) {
                                    root = rightNeighbor;
                                    root.parent = -1;
                                    root.commit(root, -1);
                                    this.addRemovedPointer(rightNeighborPointerLocation);
                                }
                            }
                        }
                    }
                }
                return this.hasUnderflow();
            } else {
                //If in leaf node
                int ks = this.keys.size();
                for (int i = 0; i < ks; i++) {
                    if (key.equals(this.keys.get(i))) {
                        //if the key is equal to the one in the list
                        //remove it
                        this.keys.remove(i);
                        this.values.remove(i);
                        return this.hasUnderflow();
                    }
                }
                //if the key wasnt found in the list then do something
                return this.hasUnderflow();
            }
        }
    }
}
