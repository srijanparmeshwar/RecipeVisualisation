package uk.ac.cam.sp715.ml;

import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.cam.sp715.flows.FeatureVectors;
import uk.ac.cam.sp715.flows.Role;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.recognition.EntityAnnotator;
import uk.ac.cam.sp715.recognition.TaggedWord;
import uk.ac.cam.sp715.util.IOTools;
import uk.ac.cam.sp715.util.IOToolsException;
import uk.ac.cam.sp715.util.Pipeline;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Statistical evaluation of the Semantic Role Labelling classifier.
 */
public class ClassifierTest {
    private static final Pipeline pipeline = Pipeline.getMainPipeline();

    private static GeneralDataset<Role, String> trainSet;
    private static GeneralDataset<Role, String> testSet;
    private static Classifier<Role, String> classifier;

    @BeforeClass
    public static void setup() {
        try {
            LinkedList<Recipe> recipes = IOTools.read(Paths.get("data", "recipes.ser").toString());
            Map<Integer, List<String>> featureMap = new HashMap<>();
            int index = 0;

            for (Recipe recipe : recipes) {
                Annotation annotation = pipeline.annotate(recipe);

                for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                    EntityAnnotator.AugmentedSemanticGraph dependencies = sentence.get(EntityAnnotator.EntityAnnotations.class);
                    List<TaggedWord> tokens = dependencies.orderedTokens();
                    int position = 0;
                    for (TaggedWord token : tokens) {
                        List<String> features = FeatureVectors.getFeatures(token, position, dependencies, tokens);
                        featureMap.put(index, features);
                        position++;
                        index++;
                    }
                }
            }

            GeneralDataset<Role, String> dataset = DataHandler.constructDataset(DataHandler.loadLabels("srl-train.txt"), featureMap);
            Pair<GeneralDataset<Role, String>, GeneralDataset<Role, String>> pair = dataset.split(0.2);

            trainSet = pair.first();
            testSet = pair.second();
            classifier = ClassifierTrainer.train(trainSet);
        } catch (IOToolsException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAccuracy() {
        System.out.println("Evaluating classifier accuracy: " + classifier.evaluateAccuracy(testSet));
    }

    private static void testPR(Role role) {
        Pair<Double, Double> PR = classifier.evaluatePrecisionAndRecall(testSet, role);
        System.out.println("Role: " + role + ", Precision: " + PR.first() + ", Recall: " + PR.second());
    }

    @Test
    public void testPrecisionAndRecall() {
        testPR(Role.ACTION);
        testPR(Role.DOBJECT);
        testPR(Role.IOBJECT);
        testPR(Role.OTHER);
    }
}
