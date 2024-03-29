package uk.ac.cam.sp715.ml;

import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.util.Pair;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.cam.sp715.flows.Role;
import uk.ac.cam.sp715.util.IOToolsException;

/**
 * Statistical evaluation of the baseline classifier for Semantic Role Labelling.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class BaselineClassifierTest {
    private static GeneralDataset<Role, String> dataSet;
    private static GeneralDataset<Role, String> testSet;
    private static Classifier<Role, String> classifier;

    @BeforeClass
    public static void setup() {
        try {
            dataSet = SRLDataHandler.loadTrainingData(4);
            testSet = dataSet;
            classifier = new BaselineClassifier();
        } catch (IOToolsException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAccuracy() {
        System.out.println("Evaluating baseline classifier accuracy: " + classifier.evaluateAccuracy(testSet));
    }

    private static void testPR(Role role) {
        Pair<Double, Double> PR = classifier.evaluatePrecisionAndRecall(testSet, role);
        double precision = PR.first();
        double recall = PR.second();
        System.out.println("Role: " + role + ", Precision: " + precision + ", Recall: " + recall + ", F1: " + (2 * precision * recall / (precision + recall)));
    }

    @Test
    public void testPrecisionAndRecall() {
        testPR(Role.ACTION);
        testPR(Role.DOBJECT);
        testPR(Role.IOBJECT);
        testPR(Role.OTHER);
    }
}
