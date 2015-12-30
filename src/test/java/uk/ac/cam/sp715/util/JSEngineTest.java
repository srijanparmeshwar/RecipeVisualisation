package uk.ac.cam.sp715.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test to check that the output of {@link JSEngine#encodeURIComponent(String)}
 * is the same as that given by browser JS engines.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class JSEngineTest {

    @Test
    public void testEncodeURIComponent() {
        String input = "abc1_& xyz+/-";
        String expected = "abc1_%26%20xyz%2B%2F-";
        assertEquals(JSEngine.encodeURIComponent(input), expected);
    }

}
