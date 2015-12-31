package uk.ac.cam.sp715.caching;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for LFU memory and persistent cache implementations.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class MemoryCacheTest {

    @Test
    public void memoryCacheTest() {
        try {
            Cache<RecipeKey, Integer> cache = new MemoryCache<>(3);
            RecipeKey a = RecipeKey.get("a");
            RecipeKey b = RecipeKey.get("b");
            RecipeKey c = RecipeKey.get("c");
            cache.add(a, 0);
            cache.add(b, 1);
            cache.add(c, 2);

            RecipeKey.get("d");
            RecipeKey d = RecipeKey.get("d");
            cache.add(d, 3);

            assertTrue(cache.containsKey(a) && cache.containsKey(b) && cache.containsKey(d));
            assertFalse(cache.containsKey(c));

            assertEquals(cache.get(a), new Integer(0));
            assertEquals(cache.get(b), new Integer(1));
            assertEquals(cache.get(d), new Integer(3));
        } catch (CacheException e) {
            fail(e.getMessage());
        }
    }
}
