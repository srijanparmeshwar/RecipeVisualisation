package uk.ac.cam.sp715.caching;

import uk.ac.cam.sp715.util.*;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistent least frequently used cache for recipes.
 * @param <V> The value type.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class PersistentCache<V extends Serializable> implements Serializable, Cache<RecipeKey, V> {
    private final String location;
    private final PriorityQueue<RecipeKey> keys;
    private int maxSize;
    private static final String name = "cache.ser";
    private static final Logger logger = Logging.getLogger(PersistentCache.class);

    /* Constructors. */
    public PersistentCache(int maxSize) {
        this(maxSize, "cache");
    }
    public PersistentCache(int maxSize, String location) {
        this.keys = new PriorityQueue<>();
        this.maxSize = maxSize;
        this.location = location;
        prepare();
    }

    /* Private utility methods. */
    private synchronized void prepare() {
        File directory = new File(this.location);
        if(!directory.exists()) directory.mkdirs();
    }
    private synchronized void save() throws IOToolsException {
        IOTools.save(this, getLocation(name));
    }
    private String getLocation(String... path) {
        return Paths.get(location, path).toString();
    }
    private synchronized void update() {
        if(!keys.isEmpty()) keys.add(keys.poll());
    }

    /* Public methods. */
    @Override
    public synchronized boolean containsKey(RecipeKey key) throws CacheException {
        try {
            boolean result = keys.contains(key);
            save();
            return result;
        } catch(IOToolsException iote) {
            logger.log(Level.SEVERE, "Error occurred updating cache files.", iote);
            throw new CacheException();
        }
    }
    @Override
    public synchronized V get(RecipeKey key) throws CacheException {
        try {
            V result = IOTools.read(getLocation(key.name()));
            save();
            return result;
        } catch(IOToolsException iote) {
            logger.log(Level.SEVERE, "Error occurred updating cache files.", iote);
            throw new CacheException();
        }
    }
    @Override
    public synchronized void add(RecipeKey key, V value) throws CacheException {
        try {
            if (keys.size() < maxSize) {
                keys.add(key);
                IOTools.save(value, getLocation(key.name()));
            } else {
                update();
                IOTools.delete(getLocation(keys.poll().name()));
                keys.add(key);
                IOTools.save(value, getLocation(key.name()));
            }
            save();
        } catch(IOToolsException iote) {
            logger.log(Level.SEVERE, "Error occurred updating cache files.", iote);
            throw new CacheException();
        }
    }
    @Override
    public synchronized int size() {
        return keys.size();
    }

    /* Public static utility methods. */
    public static boolean exists(String location) {
        return IOTools.exists(Paths.get(location, name).toString());
    }
    public static <V extends Serializable> PersistentCache<V> read(String location) throws IOToolsException {
        return IOTools.read(Paths.get(location, name).toString());
    }
}