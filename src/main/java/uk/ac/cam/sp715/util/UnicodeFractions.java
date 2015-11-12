package uk.ac.cam.sp715.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Unicode fraction replacement map.
 */
public class UnicodeFractions {
    public static final Map<String, String> fractions = new HashMap<>();
    static {
        fractions.put("\u00BC", ".25");
        fractions.put("\u00BD", ".5");
        fractions.put("\u00BE", ".75");
        fractions.put("\u2153", ".33");
        fractions.put("\u2154", ".66");
        fractions.put("\u2155", ".2");
        fractions.put("\u2156", ".4");
        fractions.put("\u2157", ".6");
        fractions.put("\u2158", ".8");
        fractions.put("\u2159", ".17");
        fractions.put("\u215A", ".83");
        fractions.put("\u215B", ".125");
        fractions.put("\u215C", ".375");
        fractions.put("\u215D", ".625");
        fractions.put("\u215E", ".875");
    }
}
