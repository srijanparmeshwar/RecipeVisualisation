package uk.ac.cam.sp715.recognition;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.junit.Test;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.util.HTMLParseException;
import uk.ac.cam.sp715.util.HTMLParser;
import uk.ac.cam.sp715.util.Pipeline;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static uk.ac.cam.sp715.wordnet.Taxonomy.*;
import static uk.ac.cam.sp715.wordnet.Taxonomy.TaxonomyType.*;

/**
 * Tests for {@link EntityAnnotator} to check whether the correct
 * entity tags are added to words for simple examples.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class EntityAnnotatorTest {

    @Test
    public void testAnnotate() {
        String testText = "Heat the milk, add to the chocolate mixture in the bowl.";
        List<TaxonomyType> expected = Arrays.asList(OTHER, OTHER, INGREDIENTS, OTHER, OTHER, OTHER, OTHER, INGREDIENTS, OTHER, OTHER, UTENSILS, OTHER);
        Pipeline pipeline = Pipeline.getPipeline("tokenize, ssplit, pos, lemma, ner, depparse");
        Annotation annotation = new Annotation(testText);
        pipeline.annotate(annotation);
        EntityAnnotator annotator = new EntityAnnotator(null, null);
        annotator.annotate(annotation);
        for(CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            assertNull(sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class));
            EntityAnnotator.AugmentedSemanticGraph dependencies = sentence.get(EntityAnnotator.EntityAnnotations.class);
            List<TaggedWord> tokens = dependencies.orderedTokens();
            List<TaxonomyType> actual = tokens
                    .stream()
                    .map(TaggedWord::entity)
                    .collect(Collectors.toList());
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testAnnotateRecipe() {
        try {
            Recipe recipe = HTMLParser.getRecipe("halloween_punch_45819");
            List<TaxonomyType>[] expectedLists = new List[] {
                    Arrays.asList(OTHER, OTHER, OTHER, OTHER, INGREDIENTS, OTHER, OTHER, OTHER, UTENSILS, OTHER, INGREDIENTS, OTHER, OTHER, OTHER, OTHER),
                    Arrays.asList(OTHER, OTHER, INGREDIENTS, OTHER, OTHER, OTHER, OTHER, OTHER, UTENSILS, OTHER)
            };
            Pipeline pipeline = Pipeline.getMainPipeline();
            Annotation annotation = pipeline.annotate(recipe);
            List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
            for(int i = 0; i<sentences.size() && i<expectedLists.length; i++) {
                List<TaxonomyType> expected = expectedLists[i];
                CoreMap sentence = sentences.get(i);
                assertNull(sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class));
                EntityAnnotator.AugmentedSemanticGraph dependencies = sentence.get(EntityAnnotator.EntityAnnotations.class);
                List<TaggedWord> tokens = dependencies.orderedTokens();
                List<TaxonomyType> actual = tokens
                        .stream()
                        .map(TaggedWord::entity)
                        .collect(Collectors.toList());
                assertEquals(expected, actual);
            }
        } catch(HTMLParseException hpe) {
            fail(hpe.getMessage());
        }
    }

}