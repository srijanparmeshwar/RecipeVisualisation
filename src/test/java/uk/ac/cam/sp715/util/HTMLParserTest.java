package uk.ac.cam.sp715.util;

import org.junit.Test;
import uk.ac.cam.sp715.recipes.Ingredient;
import uk.ac.cam.sp715.recipes.Recipe;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for HTMLParser: simple search test and simple recipe retrieval test, using hand-coded results.
 * Should check/improve later so that new recipes/rankings do not break tests.
 */
public class HTMLParserTest {

    @Test
    public void testSearch() throws Exception {
        List<Link> paths = HTMLParser.search("halloween");
        String[] expected = new String[] {"halloween_punch_45819", "halloweenbiscuits_93840", "scary_halloween_cookies_86970",
                "chocolatecobwebcupca_93842", "halloween_cake_10801", "halloween_ghost_cupcakes_04170", "skullpunch_92644",
                "witchcraft_90056", "nuttytoffeeapples_68088"};
        for(int i = 0; i<expected.length; i++) {
            assertEquals(paths.get(i).getLink(), expected[i]);
        }
    }

    @Test
    public void testGetRecipe() throws Exception {
        List<Ingredient> expectedIngredients = new LinkedList<>();
        expectedIngredients.add(new Ingredient("500ml/18fl oz cranberry juice"));
        expectedIngredients.add(new Ingredient("1.5 litres/2.5 pints lemonade"));
        expectedIngredients.add(new Ingredient("3 limes, juice only"));
        expectedIngredients.add(new Ingredient("large handful gummi worms"));

        List<String> expectedInstructions = new LinkedList<>();
        expectedInstructions.add("Pour all of the ingredients into a large glass bowl and mix until well combined.");
        expectedInstructions.add("Drape the gummi worms over the edge of the bowl.");

        Recipe expectedRecipe = new Recipe("Halloween punch", "This blood-red punch is perfect for a Halloween party. Just add a slug of gin or vodka for a grown-up version.", expectedIngredients, expectedInstructions);
        assertEquals(expectedRecipe, HTMLParser.getRecipe("halloween_punch_45819"));
    }
}