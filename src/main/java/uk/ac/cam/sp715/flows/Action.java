package uk.ac.cam.sp715.flows;

import uk.ac.cam.sp715.recognition.TaggedWord;

import java.util.List;

/**
 * Created by Srijan on 19/11/2015.
 */
public class Action implements Node {
    private final int id;
    private final TaggedWord description;
    private final List<TaggedWord> objects;
    public Action(int id, TaggedWord description, List<TaggedWord> objects) {
        this.id = id;
        this.description = description;
        this.objects = objects;
    }
    public void print() {
        System.out.println(description);
        System.out.println(objects);
    }
    public int start() {
        int min = description.start();
        for(TaggedWord token : getObjects()) {
            if(token.start()<min) min = token.start();
        }
        return min;
    }
    public int end() {
        int max = description.end();
        for(TaggedWord token : getObjects()) {
            if(token.end()<max) max = token.end();
        }
        return max;
    }
    public int sentIndex() {
        return description.sentIndex();
    }
    public int getID() {
        return id;
    }
    public String id() {
        return String.valueOf(id);
    }
    public String description() {
        return description.toString();
    }
    public void addObject(TaggedWord word) {
        objects.add(word);
    }
    public List<TaggedWord> getObjects() {
        return objects;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(description);
        builder.append(" ");
        builder.append(objects);
        return builder.toString();
    }
}
