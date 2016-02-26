package uk.ac.cam.sp715.recognition;

import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;
import org.jgrapht.graph.DefaultDirectedGraph;
import uk.ac.cam.sp715.recipes.Ingredient;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.util.Pipeline;
import uk.ac.cam.sp715.wordnet.Explorer;
import uk.ac.cam.sp715.wordnet.Taxonomy;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Srijan on 19/01/2016.
 */
public class LowerCaseAnnotator implements Annotator {
    /**
     * Pipeline annotator name.
     */
    public static final String NAME = "lowercase";

    public LowerCaseAnnotator(String string, Properties props) {}

    private static void toLowerCase(CoreLabel token) {
        String lowercase = token.word().toLowerCase();
        token.setOriginalText(lowercase);
        token.set(CoreAnnotations.TextAnnotation.class, lowercase);
        token.set(CoreAnnotations.ValueAnnotation.class, lowercase);
    }

    public void annotate(Annotation annotation) {
        for(CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for(int i = 0; i < tokens.size(); i++) {
                CoreLabel previous = i > 0 ? tokens.get(i - 1) : null;
                CoreLabel token = tokens.get(i);
                CoreLabel next = i < tokens.size() - 1 ? tokens.get(i + 1) : null;
                if(previous == null || previous.toString().equals(",")) {
                    if(token.toString().length() > 0
                            && Character.isUpperCase(token.toString().charAt(0))
                            && (next == null || !Character.isUpperCase(next.toString().charAt(0)))
                            && (previous == null || !Character.isUpperCase(previous.toString().charAt(0)))) toLowerCase(token);
                }
            }
        }
    }

    @Override
    public Set<Annotator.Requirement> requirementsSatisfied() {
        return Collections.singleton(new Annotator.Requirement(NAME));
    }

    @Override
    public Set<Annotator.Requirement> requires() {
        return Collections.singleton(Annotator.SSPLIT_REQUIREMENT);
    }

}
