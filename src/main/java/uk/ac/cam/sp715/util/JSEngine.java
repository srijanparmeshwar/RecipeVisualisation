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
    private static final Logger logger = Logging.getLogger(JSEngine.class);

    private static String escape(String input) {
        StringBuilder builder = new StringBuilder();
        for(char c : input.toCharArray()) {
            if(c == '\'') builder.append("\\\'");
            else builder.append(c);
        }
        return builder.toString();
    }

    /**
     * Takes query as input and returns URI encoded version.
     * @param input Query.
     * @return {@link String} - URI encoded query.
     */
    public static String encodeURIComponent(String input) {
        try {
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("nashorn");
            return (String) engine.eval("encodeURIComponent('" + escape(input) + "');");
        } catch (ScriptException e) {
            logger.log(Level.SEVERE, "Error encoding query via JavaScript engine.", e);
        }
        return input;
    }

    public static void main(String[] args) {
        System.out.println(JSEngine.encodeURIComponent("'hello'"));
    }
}
