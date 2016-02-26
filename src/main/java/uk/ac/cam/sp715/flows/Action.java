package uk.ac.cam.sp715.flows;

import uk.ac.cam.sp715.recognition.TaggedWord;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Class to represent an action with a set of dependent objects.
 * It is also used to represent nodes in the {@link Flow} outputs
 * from the visualiser.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class Action implements Serializable {
    private final int id;
    private final TaggedWord description;
    private final List<TaggedWord> dObjects;
    private final List<TaggedWord> iObjects;
    public Action(int id, TaggedWord description, List<TaggedWord> dObjects, List<TaggedWord> iObjects) {
        this.id = id;
        this.description = description;
        this.dObjects = dObjects;
        this.iObjects = iObjects;
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
    public void addObject(TaggedWord word, Role role) {
        if(role == Role.IOBJECT) iObjects.add(word);
        else dObjects.add(word);
    }
    public List<TaggedWord> getObjects() {
        List<TaggedWord> all = new LinkedList<>();
        all.addAll(dObjects);
        all.addAll(iObjects);
        return all;
    }
    public List<TaggedWord> getDObjects() {
        return dObjects;
    }
    public List<TaggedWord> getIObjects() {
        return iObjects;
    }
    public void remove(TaggedWord word, Role role) {
        if(role == Role.IOBJECT) iObjects.remove(word);
        else dObjects.remove(word);
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(description);
        builder.append(", ");
        builder.append(dObjects);
        builder.append(", ");
        builder.append(iObjects);
        return builder.toString();
    }
}
