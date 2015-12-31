package uk.ac.cam.sp715.caching;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Keys for priority queue caches. It implements {@link Comparable} such
 * that caches which use these are LFU caches.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class RecipeKey implements Serializable, Comparable<RecipeKey> {
    private final String name;
    private int count;
    private static final Map<String, RecipeKey> map = new HashMap<>();

    private RecipeKey(String filename) {
        this.name = filename;
        this.count = 0;
    }

    /**
     * Requests the key wrapper, and increments the number of requests for this key.
     * @param key The recipe query.
     * @return The wrapped key.
     */
    public static RecipeKey get(String key) {
        if (!map.containsKey(key)) map.put(key, new RecipeKey(key));
        RecipeKey value = map.get(key);
        value.count++;
        return value;
    }

    @Override
    public int compareTo(RecipeKey o) {
        return Integer.compare(this.count, o.count);
    }

    public int count() {
        return count;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "[" + name + ", " + count + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RecipeKey key = (RecipeKey) o;

        return !(name != null ? !name.equals(key.name) : key.name != null);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
