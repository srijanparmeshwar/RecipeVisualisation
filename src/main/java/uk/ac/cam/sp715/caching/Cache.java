package uk.ac.cam.sp715.caching;

/**
 * Key/value cache interface.
 * @param <K> The key type.
 * @param <V> The value type.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public interface Cache<K extends Comparable<K>, V> {
    /**
     * Checks whether the cache contains this key and so
     * whether its associated value is also contained.
     * @param key Key to be checked.
     * @return True if the cache contains the key otherwise false.
     * @throws CacheException Thrown if error occurs accessing the cache.
     */
    boolean containsKey(K key) throws CacheException;

    /**
     * Returns the value associated with this key. This method
     * will return null if the (key, value) pair is not in the cache.
     * @param key Key to retrieve the associated value.
     * @return Value which is associated with the given key.
     * @throws CacheException Thrown if error occurs accessing the cache.
     */
    V get(K key) throws CacheException;

    /**
     * Adds an entry for this (key, value) pair if the cache is not
     * full, or if the key has a higher priority than the current
     * minimum priority key in the queue.
     * @param key Key.
     * @param value Value.
     * @throws CacheException Thrown if error occurs accessing the cache.
     */
    void add(K key, V value) throws CacheException;

    /**
     * Returns the number of entries in this cache.
     * @return The number of entries in the cache.
     */
    int size();
}
