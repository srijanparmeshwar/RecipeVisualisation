package uk.ac.cam.sp715.util;

import org.junit.Test;
import uk.ac.cam.sp715.recipes.Recipe;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for HTMLParser: simple search test and simple recipe retrieval test.
 * Checks that results are not null, and that certain strings are non-empty.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class HTMLParserTest {

    @Test
    public void testSearch() {
        try {
            List<Link> paths = HTMLParser.search("halloween");
            assertNotNull(paths);
            assertTrue(paths.size() > 0);
            for (Link link : paths) {
                assertNotNull(link);
                assertNotNull(link.getLink());
                assertTrue(link.getLink().length() > 0);
            }
        } catch(HTMLParseException hpe) {
            fail(hpe.getMessage());
        }
    }

    @Test
    public void testGetRecipe() {
        try {
            Recipe recipe = HTMLParser.getRecipe("halloween_punch_45819");
            assertNotNull(recipe);

            assertNotNull(recipe.getTitle());
            assertNotNull(recipe.getSummary());
            assertNotNull(recipe.getIngredients());
            assertNotNull(recipe.getDescription());

            assertTrue(recipe.getDescription().length()>0);
        } catch(HTMLParseException hpe) {
            fail(hpe.getMessage());
        }
    }
}