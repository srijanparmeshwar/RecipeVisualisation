package uk.ac.cam.sp715.ml;


import edu.stanford.nlp.classify.*;
import edu.stanford.nlp.optimization.DiffFunction;
import edu.stanford.nlp.optimization.Minimizer;
import edu.stanford.nlp.optimization.QNMinimizer;
import edu.stanford.nlp.util.Factory;
import uk.ac.cam.sp715.flows.Role;

/**
 * Created by Srijan on 18/12/2015.
 */
public class ClassifierTrainer {
    private static final LinearClassifierFactory<Role, String> lcFactory = new LinearClassifierFactory<>();
    public static Classifier<Role, String> train(GeneralDataset<Role, String> dataset) {
        return lcFactory.trainClassifier(dataset);
    }

}
