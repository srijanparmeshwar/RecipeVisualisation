package uk.ac.cam.sp715.caching;

/**
 * Key/value cache interface.
 * @param <K> The key type.
 * @param <V> The value type.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public interface Cache<K extends Comparable<K>, V> {
    boolean containsKey(K key) throws CacheException;
    V get(K key) throws CacheException;
    void add(K key, V value) throws CacheException;
    int size();
}
