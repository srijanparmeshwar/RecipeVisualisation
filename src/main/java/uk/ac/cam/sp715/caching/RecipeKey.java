package uk.ac.cam.sp715.caching;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Srijan on 30/12/2015.
 */
public class RecipeKey implements Serializable, Comparable<RecipeKey> {
    private final String name;
    private int count;
    private static final Map<String, RecipeKey> map = new HashMap<>();

    private RecipeKey(String filename) {
        this.name = filename;
        this.count = 0;
    }

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
