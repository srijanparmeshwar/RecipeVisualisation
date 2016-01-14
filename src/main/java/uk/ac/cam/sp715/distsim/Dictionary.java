package uk.ac.cam.sp715.distsim;

import uk.ac.cam.sp715.recognition.EntityAnnotator;
import uk.ac.cam.sp715.recognition.EntityAnnotator.AugmentedSemanticGraph;
import uk.ac.cam.sp715.recognition.TaggedWord;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Bijective dictionary to store and retrieve strings.
 */
public class Dictionary {
    private final Map<String, Integer> indices;
    private final Map<Integer, String> words;
    private int size = 0;
    public Dictionary() {indices = new HashMap<>(); words = new HashMap<>();}
    public Dictionary(Collection<AugmentedSemanticGraph> sentences) {
        this();
        for(AugmentedSemanticGraph sentence : sentences) {
            for(TaggedWord token : sentence.orderedTokens()) {
                String lowercase = token.toString().toLowerCase();
                if(!containsWord(lowercase)) add(lowercase);
            }
        }
    }
    public boolean containsWord(String s) {
        return indices.containsKey(s);
    }
    public void add(String s) {
        indices.put(s, size);
        words.put(size, s);
        size++;
    }
    public int index(String s) {
        if(!indices.containsKey(s)) add(s);
        return indices.get(s);
    }
    public String word(int index) {
        return words.get(index);
    }
}