package uk.ac.cam.sp715.util;

/**
 * Exception which can occur when parsing BBC website pages, either search
 * results or recipe articles.
 */
public class HTMLParseException extends Exception {
    private final String query;
    public HTMLParseException(String query) {
        this.query = query;
    }
    public String getQuery() {return query;}
}
