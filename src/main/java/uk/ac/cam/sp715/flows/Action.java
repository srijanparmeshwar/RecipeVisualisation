package uk.ac.cam.sp715.flows;

import java.util.List;

/**
 * Created by Srijan on 19/11/2015.
 */
public class Action implements Node {
    private final String subject;
    private final String description;
    private final List<String> objects;
    public Action(String subject, String description, List<String> objects) {
        this.subject = subject;
        this.description = description;
        this.objects = objects;
    }
}
