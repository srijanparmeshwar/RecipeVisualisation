package uk.ac.cam.sp715.caching;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

public class MemoryCache<K extends Comparable<K>, V>  implements LFUCache<K, V>, Iterable<K> {
    private final PriorityQueue<K> keys;
    private final Map<K, V> entries;
    private final int maxSize;
    public MemoryCache(int maxSize) {
        this.keys = new PriorityQueue<>();
        this.entries = new HashMap<>();
        this.maxSize = maxSize;
    }
    public synchronized boolean containsKey(K key) {
        return entries.containsKey(key);
    }
    public synchronized V get(K key) {
        return entries.get(key);
    }
    private synchronized void update() {
        if(!keys.isEmpty()) keys.add(keys.poll());
    }
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
