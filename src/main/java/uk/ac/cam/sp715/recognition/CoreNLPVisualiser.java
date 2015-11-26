package uk.ac.cam.sp715.recognition;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import uk.ac.cam.sp715.flows.Flow;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.util.HTMLParseException;
import uk.ac.cam.sp715.util.HTMLParser;

import java.util.List;
import java.util.Properties;

/**
 * Created by Srijan on 26/11/2015.
 */
public class CoreNLPVisualiser extends Visualiser {
    private final StanfordCoreNLP pipeline;
    public CoreNLPVisualiser(StanfordCoreNLP pipeline) {
        this.pipeline = pipeline;
    }
    @Override
    public Flow parse(Recipe recipe) {
        Annotation annotation = new Annotation(recipe.getDescription());
        pipeline.annotate(annotation);

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sentence : sentences) {
            System.out.println();
            System.out.println(sentence.toString());
            SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
            dependencies.edgeListSorted().forEach(System.out::println);
        }
        return null;
    }

    public static void main(String[] args) throws HTMLParseException {
        Properties props = new Properties();
        props.setProperty("annotators",
                "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        CoreNLPVisualiser visualiser = new CoreNLPVisualiser(pipeline);
        visualiser.parse(HTMLParser.getRecipe(HTMLParser.search("choc").get(0).getLink()));
    }
}
