package uk.ac.cam.sp715.recognition;

import edu.stanford.nlp.ling.CoreLabel;
import uk.ac.cam.sp715.wordnet.Taxonomy.TaxonomyType;

import java.util.LinkedList;
import java.util.List;

public class TaggedToken extends CoreLabel {
    private final List<CoreLabel> tokens;
    private final TaxonomyType tag;
    public TaggedToken(TaxonomyType tag) {
        super();
        this.tokens = new LinkedList<>();
        this.tag = tag;
    }
    public TaggedToken(CoreLabel label, TaxonomyType tag) {
        super(label);
        tokens = new LinkedList<>();
        tokens.add(label);
        this.tag = tag;
    }
    public void addToken(CoreLabel token) {
        tokens.add(token);
    }
    public void addAll(List<CoreLabel> tokens) {
        this.tokens.addAll(tokens);
    }
    public List<CoreLabel> getTokens() {
        return tokens;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        boolean firstToken = true;
        for (CoreLabel token : tokens) {
            if (firstToken) firstToken = false;
            else {
                builder.append(" ");
            }
            builder.append(token.word());
        }
        builder.append("_");
        builder.append(tag);
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
}
