package uk.ac.cam.sp715.recognition;

import edu.stanford.nlp.ling.IndexedWord;
import uk.ac.cam.sp715.wordnet.Taxonomy.TaxonomyType;

import java.util.LinkedList;
import java.util.List;

public class TaggedWord extends IndexedWord {
    private final List<IndexedWord> tokens;
    private final TaxonomyType tag;
    public TaggedWord(TaxonomyType tag) {
        super();
        this.tokens = new LinkedList<>();
        this.tag = tag;
    }
    public TaggedWord(IndexedWord label, TaxonomyType tag) {
        super(label);
        tokens = new LinkedList<>();
        tokens.add(label);
        this.tag = tag;
        this.setBeginPosition(label.beginPosition());
        this.setEndPosition(label.endPosition());
    }
    public void addToken(IndexedWord token) {
        tokens.add(token);
    }
    public void addAll(List<IndexedWord> tokens) {
        this.tokens.addAll(tokens);
        this.tokens.forEach((token) -> {
            if(token.beginPosition()<this.beginPosition()) this.setBeginPosition(token.beginPosition());
            if(token.endPosition()>this.endPosition()) this.setEndPosition(token.endPosition());
        });
    }
    public List<IndexedWord> getTokens() {
        return tokens;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        boolean firstToken = true;
        for (IndexedWord token : tokens) {
            if (firstToken) firstToken = false;
            else {
                builder.append(" ");
            }
            builder.append(token.word());
        }
        return builder.toString();
    }
    public boolean isTypedEntity() {
        return tag != TaxonomyType.OTHER;
    }
    public boolean isNoun() {
        return tag().startsWith("N");
    }
    public TaxonomyType entity() {
        return tag;
    }
    public String getLemma() {
        StringBuilder builder = new StringBuilder();
        boolean firstToken = true;
        for (IndexedWord token : tokens) {
            if (firstToken) firstToken = false;
            else {
                builder.append(" ");
            }
            builder.append(token.lemma());
        }
        return builder.toString();
    }
    public int start() {
        int min = Integer.MAX_VALUE;
        for(IndexedWord token : getTokens()) {
            if(token.beginPosition()<min) min = token.beginPosition();
        }
        return min;
    }
    public int end() {
        int max = Integer.MIN_VALUE;
        for(IndexedWord token : getTokens()) {
            if(token.endPosition()>max) max = token.endPosition();
        }
        return max;
    }
}
