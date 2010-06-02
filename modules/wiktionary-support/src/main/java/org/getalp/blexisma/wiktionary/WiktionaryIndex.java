package org.getalp.blexisma.wiktionary;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * WiktionaryIndex is a Persistent HashMap designed to hold an index on a
 * Wiktionary dump file.
 * 
 * @author serasset
 * 
 */
public class WiktionaryIndex implements Map<String, String> {

    // TODO: Create a static map to hold shared instances (1 per dump file) and avoid allocating more than one 
    // WiktionaryIndexer per wiktionary language.
    
    /**
	 * 
	 */
    private static final long serialVersionUID = 7658718925280104333L;
    private static final int AVERAGE_PAGE_SIZE = 730; // figure taken from French Wiktionary
    private static final String UTF_16 = "UTF-16";
    private static final String UTF_8 = "UTF-8";

    File dumpFile;
    File indexFile;
    HashMap<String, OffsetValue> map;
    RandomAccessFile xmlf ;
    
    public static String indexFilename(String dumpFilename) {
        return dumpFilename + ".idx";
    }

    public static File indexFile(File dumpFile) {
        return new File(indexFilename(dumpFile.getPath()));
    }

    /**
     * Creates a WiktionaryIndex for the wiktionary dump whose filename is
     * passed as a parameter
     * 
     * @param filename
     *            the name of the file containing the wiktionary dump
     * @throws WiktionaryIndexerException thrown if any error occur during index initialization
     */
    public WiktionaryIndex(String filename) throws WiktionaryIndexerException {
        this(new File(filename));
    }

    /**
     * Creates a WiktionaryIndex for the wiktionary dump whose filename is
     * passed as a parameter
     * 
     * @param file
     *            the file containing the wiktionary dump.
     * @throws WiktionaryIndexerException thrown if any error occur during index initialization
     */
    public WiktionaryIndex(File file) throws WiktionaryIndexerException {
        dumpFile = file;
        indexFile = indexFile(file);
        if (this.indexIsUpToDate()) {
            this.loadIndex();
        } else {
            this.initIndex();
        }
        try {
            xmlf = new RandomAccessFile(dumpFile,"r");  
        } catch (IOException ex) {
            throw new WiktionaryIndexerException("Could not open wiktionary dump file " + dumpFile.getPath(), ex);
        }
    }

    // TODO: check if index content is up to date ?
    public boolean indexIsUpToDate() {
        return indexFile.canRead();
    }

    public void dumpIndex() throws WiktionaryIndexerException {
        try {
            RandomAccessFile out = new RandomAccessFile(indexFile, "rw");
            FileChannel fc = out.getChannel();
            
            ByteBuffer buf = ByteBuffer.allocate(4098);
            
            // Write index signature out.write(...)
            buf.put("Wkt!00".getBytes(UTF_8));
            buf.putInt(map.size());
            for (Map.Entry<String,OffsetValue> entry : map.entrySet()) {
                // TODO: it may be more efficient to create a Charset decoder or use a reusable byte[]
                // but it seems that it is not possible in jdk1.5... has to wait for jdk 1.6
                byte[] bk = entry.getKey().getBytes(UTF_8);
                OffsetValue v = entry.getValue();
                // I serialize 1 int, the string, 2 int --> bk.length + 12 bytes;
                // If there is not enough room left in the buffer, first write it out, then proceed
                if (buf.remaining() < bk.length+12 ) {
                    buf.flip();
                    fc.write(buf);
                    buf.clear();
                }
                
                buf.putInt(bk.length);
                buf.put(bk);
                buf.putInt(v.start);
                buf.putInt(v.length);
            }
            buf.flip();
            fc.write(buf);
            out.close();
          } catch(IOException e) {
              throw new WiktionaryIndexerException("IOException when writing map to index file", e);
          }
    }

    public void loadIndex() throws WiktionaryIndexerException {
        try {
            RandomAccessFile in = new RandomAccessFile(indexFile, "r");
            FileChannel fc = in.getChannel();
            
            ByteBuffer buf = ByteBuffer.allocate(4098);
            
            fc.read(buf);
            buf.flip();
            byte[] signature = new byte[6]; 
            buf.get(signature, 0, signature.length);
            String signatureString = new String(signature, UTF_8);
            if (! signatureString.equals("Wkt!00")) 
                throw new WiktionaryIndexerException("Index file seems to be corrupted", null);
            
            int mapSize = buf.getInt();
            
            map = new HashMap<String,OffsetValue>((int)(mapSize / .75));
            byte[] bk = new byte[2048]; // We assume that no entry title is longer than 2048
            
            for (int i=0; i< mapSize; i++) {
                // First read the next entry size
                if (buf.remaining() < 4) { // 4 bytes = 1 int...
                    readNextChunk(fc, buf);
                }
                int kSize = buf.getInt();
                // Check if the whole entry is already in the buffer, else advance buffer to fit whole entry
                if (buf.remaining() < kSize + 12) { // kSize byte + 2 ints...
                    readNextChunk(fc, buf);
                }
                // read the entry
                buf.get(bk, 0, kSize);
                String key = new String(bk, 0, kSize, UTF_8);
                int vstart = buf.getInt();
                int vlength = buf.getInt();
                map.put(key, new OffsetValue(vstart, vlength));               
            }
            in.close();
          } catch(IOException e) {
              throw new WiktionaryIndexerException("IOException when reading map from index file", e);
          }
    }

    private void readNextChunk(FileChannel fc, ByteBuffer buf) throws IOException {
        byte [] ba = new byte[buf.remaining()];
        buf.get(ba);
        buf.clear();
        buf.put(ba);
        fc.read(buf);
        buf.flip();
    }

    public void initIndex() throws WiktionaryIndexerException {
        int initialCapacity = (int) ((this.dumpFile.length() / AVERAGE_PAGE_SIZE) / .75);
        map = new HashMap<String, OffsetValue>(initialCapacity);
        WiktionaryIndexer.createIndex(dumpFile, map);
        long starttime = System.currentTimeMillis();
        System.out.println("Dumping index...");
        this.dumpIndex();
        long endtime = System.currentTimeMillis();
        System.out.println(" Dumping index Time = " + (endtime - starttime) + "; ");
    }

    public void clear() {
        throw new RuntimeException("put: unsupported method (a WiktionaryIndex is read/only.");
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        throw new RuntimeException("containsValue: unsupported method.");
    }

    public Set<Map.Entry<String, String>> entrySet() {
        throw new RuntimeException("entrySet: unsupported method.");
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public String get(Object key) {
        OffsetValue ofs = map.get(key);
        String res = null;
        try {
            xmlf.seek(ofs.start*2 + 2); // in utf-16, 2 first bytes for the BOM
            byte[] b = new byte[ofs.length*2];
            xmlf.readFully(b);
            res = new String(b, UTF_16);
        } catch (IOException ex) {
            res = null;
        }
        return res;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public String put(String key, String value) {
        throw new RuntimeException("put: unsupported method (a WiktionaryIndex is read/only).");
    }

    public void putAll(Map<? extends String, ? extends String> t) {
        throw new RuntimeException("putAll: unsupported method (a WiktionaryIndex is read/only).");        
    }

    public String remove(Object key) {
        throw new RuntimeException("remove: unsupported method (a WiktionaryIndex is read/only).");        
    }

    public int size() {
        return map.size();
    }

    public Collection<String> values() {
        throw new RuntimeException("values: unsupported method.");        
    }
    
    public String getTextOfPage(Object key) {
        return WiktionaryIndexer.getTextElementContent(this.get(key));
    }
    
    public static void main(String[] args) throws WiktionaryIndexerException {
        long starttime = System.currentTimeMillis();
        WiktionaryIndex wi = new WiktionaryIndex(args[0]);
        long endtime = System.currentTimeMillis();
        System.out.println(" Indexing Time = " + (endtime - starttime) + "; " + wi.map.size() + " pages indexed.");
        
        System.out.println("accueil : " +wi.get("accueil"));
        System.out.println("dictionnaire : " +wi.get("dictionnaire"));
        System.out.println("amour : " +wi.get("amour"));
        
        long startloadtime = System.currentTimeMillis();
        System.out.println("Loading map");
        wi.loadIndex();
        long endloadtime = System.currentTimeMillis();
        System.out.println(" Loading Time = " + (endloadtime - startloadtime) + "; ");
        
        System.out.println("accueil : " +wi.get("accueil"));
        System.out.println("dictionnaire : " +wi.get("dictionnaire"));
        System.out.println("amour : " +wi.get("amour"));
  
     }
}