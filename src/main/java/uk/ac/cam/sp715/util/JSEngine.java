package uk.ac.cam.sp715.util;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to encode URI format for HTTP requests.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class JSEngine {
    private static final Logger logger = Logging.getLogger(JSEngine.class.getName());

    /**
     * Takes query as input and returns URI encoded version.
     * @param input Query.
     * @return {@link String} - URI encoded query.
     */
    public static String encodeURI(String input) {
        try {
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("nashorn");
            return (String) engine.eval("encodeURI('" + input + "');");
        } catch (ScriptException e) {
            logger.log(Level.SEVERE, "Error encoding query via JavaScript engine.", e);
        }
        return input;
    }
}
