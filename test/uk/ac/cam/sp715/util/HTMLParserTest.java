package uk.ac.cam.sp715.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Srijan on 30/10/2015.
 */
public class HTMLParserTest {

    @Test
    public void testSearch() throws Exception {
        List<String> paths = HTMLParser.search("halloween");
        String[] expected = new String[] {"halloween_punch_45819", "halloweenbiscuits_93840", "scary_halloween_cookies_86970",
                "chocolatecobwebcupca_93842", "halloween_cake_10801", "halloween_ghost_cupcakes_04170", "skullpunch_92644",
                "witchcraft_90056", "nuttytoffeeapples_68088", "bewitched_92643", "roastchestnuts_68084", "parkin_with_cider_and_65702",
                "wicked_whisky_sour_85443", "sloeginfizz_88826", "roastpumpkinseeds_70660"};
        for(int i = 0; i<paths.size(); i++) {
            assertEquals(paths.get(i), expected[i]);
        }
    }

    @Test
    public void testGetRecipe() throws Exception {

    }
}