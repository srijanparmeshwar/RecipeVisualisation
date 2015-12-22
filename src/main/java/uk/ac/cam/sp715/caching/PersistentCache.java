package uk.ac.cam.sp715.caching;

import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.util.*;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Srijan on 17/12/2015.
 */
public class PersistentCache<V extends Serializable> implements Serializable, LFUCache<PersistentCache.Key, V> {
    private final String location;
    private final PriorityQueue<Key> keys;
    private int maxSize;
    private static final String name = "cache.ser";
    private static final Logger logger = Logging.getLogger(PersistentCache.class.getName());

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
    public synchronized boolean containsKey(Key key) throws CacheException {
        try {
            boolean result = keys.contains(key);
            save();
            return result;
        } catch(IOToolsException iote) {
            logger.log(Level.SEVERE, "Error occurred updating cache files.", iote);
            throw new CacheException();
        }
    }
    public synchronized V get(Key key) throws CacheException {
        try {
            V result = IOTools.read(getLocation(key.filename()));
            save();
            return result;
        } catch(IOToolsException iote) {
            logger.log(Level.SEVERE, "Error occurred updating cache files.", iote);
            throw new CacheException();
        }
    }
    public synchronized void add(Key key, V value) throws CacheException {
        try {
            if (keys.size() < maxSize) {
                keys.add(key);
                IOTools.save(value, getLocation(key.filename()));
            } else {
                update();
                IOTools.delete(getLocation(keys.poll().filename()));
                keys.add(key);
                IOTools.save(value, getLocation(key.filename()));
            }
            save();
        } catch(IOToolsException iote) {
            logger.log(Level.SEVERE, "Error occurred updating cache files.", iote);
            throw new CacheException();
        }
    }
    public synchronized int size() {
        return keys.size();
    }

    public static class Key implements Serializable, Comparable<Key> {
        private final String filename;
        private int count;
        private static final Map<String, Key> map = new HashMap<>();

        private Key(String filename) {
            this.filename = filename;
            this.count = 0;
        }

        public static Key get(String key) {
            if (!map.containsKey(key)) map.put(key, new Key(key));
            Key value = map.get(key);
            value.count++;
            return value;
        }

        @Override
        public int compareTo(Key o) {
            return Integer.compare(this.count, o.count);
        }

        public int count() {
            return count;
        }

        public String filename() {
            return filename;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            return !(filename != null ? !filename.equals(key.filename) : key.filename != null);
        }

        @Override
        public int hashCode() {
            return filename != null ? filename.hashCode() : 0;
        }
    }

    /* Public static methods. */
    public static boolean exists(String location) {
        return IOTools.exists(Paths.get(location, name).toString());
    }
    public static <V extends Serializable> PersistentCache<V> read(String location) throws IOToolsException {
        return IOTools.read(Paths.get(location, name).toString());
    }
}