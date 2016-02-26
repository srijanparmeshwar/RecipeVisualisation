package uk.ac.cam.sp715.util;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.recognition.EntityAnnotator;

import java.util.Properties;

/**
 * Created by Srijan on 15/12/2015.
 */
public class Pipeline extends StanfordCoreNLP {
    /**
     * Construct a basic pipeline. The Properties will be used to determine which annotators to create, and a default AnnotatorPool will be used to create the annotators.
     * @param properties Properties to be used.
     */
    public Pipeline(Properties properties) {
        super(properties);
    }
    public static Pipeline getPipeline(String annotators) {
        Properties props = new Properties();
        props.setProperty("customAnnotatorClass.lowercase", "uk.ac.cam.sp715.recognition.LowerCaseAnnotator");
        props.setProperty("customAnnotatorClass.entities", "uk.ac.cam.sp715.recognition.EntityAnnotator");
        props.setProperty("annotators", annotators);
        return new Pipeline(props);
    }
    public static Pipeline getMainPipeline() {
        return getPipeline("tokenize, ssplit, lowercase, pos, lemma, ner, depparse, entities");
    }
    public static Pipeline getLemmaPipeline() {
        return getPipeline("tokenize, ssplit, lowercase, pos, lemma");
    }
    /**
     * Run the pipeline on an input annotation. The annotation is modified in place.
     * @param recipe Recipe to be annotated.
     * @return Annotated recipe.
     */
    public Annotation annotate(Recipe recipe) {
        EntityAnnotator.augmentIngredientDictionary(recipe);
        Annotation annotation = new Annotation(recipe.getDescription());
        annotate(annotation);
        return annotation;
    }
}
