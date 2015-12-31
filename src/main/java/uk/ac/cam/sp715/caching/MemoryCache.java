package uk.ac.cam.sp715.caching;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * In memory priority queue cache.
 * @param <K> The key type.
 * @param <V> The value type.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class MemoryCache<K extends Comparable<K>, V>  implements Cache<K, V>, Iterable<K> {
    private final PriorityQueue<K> keys;
    private final Map<K, V> entries;
    private final int maxSize;

    /**
     * Constructs an in memory cache.
     * @param maxSize The maximum number of entries allowed in the cache.
     */
    public MemoryCache(int maxSize) {
        this.keys = new PriorityQueue<>();
        this.entries = new HashMap<>();
        this.maxSize = maxSize;
    }
    @Override
    public synchronized boolean containsKey(K key) {
        return entries.containsKey(key);
    }
    @Override
    public synchronized V get(K key) {
        return entries.get(key);
    }
    private synchronized void update() {
        if(!keys.isEmpty()) keys.add(keys.poll());
    }
    @Override
    public synchronized void add(K key, V value) {
        if(entries.size()<maxSize) {
            keys.add(key);
            entries.put(key, value);
        } else {
            update();
            entries.remove(keys.poll());
            keys.add(key);
            entries.put(key, value);
        }
    }
    @Override
    public synchronized int size() {
        return entries.size();
    }
    @Override
    public Iterator<K> iterator() {
        synchronized (this) {
            return entries.keySet().iterator();
        }
    }
}
