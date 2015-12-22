package uk.ac.cam.sp715.ml;


import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import uk.ac.cam.sp715.flows.Role;

/**
 * Created by Srijan on 18/12/2015.
 */
public class ClassifierTrainer {

    public static LinearClassifier<Role, String> train(GeneralDataset<Role, String> dataset) {
        LinearClassifierFactory<Role, String> lcFactory = new LinearClassifierFactory<>();
        return lcFactory.trainClassifier(dataset);
    }

}
