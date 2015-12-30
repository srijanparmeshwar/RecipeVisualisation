package uk.ac.cam.sp715.util;

/**
 * BBC link which is passed to the web application as JSON,
 * and is used by the program to retrieve the recipes.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class Link {
    private final String link;
    private final String title;
    public Link(String link, String title) {
        this.link = link;
        this.title = title;
    }
    public String getLink() {
        return link;
    }
    public String getTitle() {
        return title;
    }
    public String toJSON() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"link\":\"");
        builder.append(link);
        builder.append("\", \"title\":\"");
        builder.append(title);
        builder.append("\"}");
        return builder.toString();
    }
    @Override
    public String toString() {
        return "[" + link + ", " + title + "]";
    }
}
