package uk.ac.cam.sp715.ml;

import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.util.Pair;
import org.junit.Test;
import uk.ac.cam.sp715.flows.Role;
import uk.ac.cam.sp715.util.IOToolsException;

import java.util.HashMap;
import java.util.Map;

/**
 * Statistical evaluation of the Semantic Role Labelling classifier.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class ClassifierTest {

    private class Result {
        private final double totalPrecision;
        private final double totalRecall;
        private final double count;
        public Result(double precision, double recall, double count) {
            this.totalPrecision = precision;
            this.totalRecall = recall;
            this.count = count;
        }
        public Result add(Result other) {
            return new Result(totalPrecision + other.totalPrecision, totalRecall + other.totalRecall, count + other.count);
        }
        public double getPrecision() {return totalPrecision / count;}
        public double getRecall() {return totalRecall / count;}
        public double getF1Score() {return 2 * totalPrecision * totalRecall / (count * (totalPrecision + totalRecall));}
    }

    private class Evaluator {
        private double totalAccuracy;
        private final Map<Role, Result> results;
        private double count;
        public Evaluator() {
            count = 0;
            totalAccuracy = 0;
            results = new HashMap<>();
            for(Role role : Role.values()) results.put(role, new Result(0, 0, 0));
        }
        public void add(double other) {
            totalAccuracy += other;
            count++;
        }
        public void add(Role role, Result other) {
            results.put(role, results.get(role).add(other));
        }
        public double getAccuracy() {return  totalAccuracy / count;}
        public Result getPrecisionAndRecall(Role role) {
            return results.get(role);
        }
    }

    @Test
    public void testCrossValidation() {
        try {
            GeneralDataset<Role, String> dataSet = SRLDataHandler.loadTrainingData(4);
            int nfolds = 10;

            Evaluator evaluator = new Evaluator();

            for(int index = 0; index < nfolds; index++) {
                Pair<GeneralDataset<Role, String>, GeneralDataset<Role, String>> folds = dataSet.splitOutFold(index, nfolds);
                GeneralDataset<Role, String> trainSet = folds.first();
                GeneralDataset<Role, String> testSet = folds.second();

                Classifier<Role, String> classifier = ClassifierTrainer.train(trainSet);
                double accuracy = classifier.evaluateAccuracy(testSet);
                evaluator.add(accuracy);
                for(Role role : Role.values()) {
                    double roleCount = testSet.numDatumsPerLabel().getCount(role) > 0 ? 1 : 0;
                    if(roleCount > 0) {
                        Pair<Double, Double> precRecall = classifier.evaluatePrecisionAndRecall(testSet, role);
                        double precision = precRecall.first();
                        double recall = precRecall.second();
                        evaluator.add(role, new Result(precision, recall, roleCount));
                    }
                }
            }

            System.out.println(nfolds + "-fold cross validation");
            System.out.println("Accuracy: " + evaluator.getAccuracy());
            for(Role role : Role.values()) {
                Result result = evaluator.getPrecisionAndRecall(role);
                System.out.println("Role: " + role + ", Precision: " + result.getPrecision() + ", Recall: " + result.getRecall() + ", F1: " + result.getF1Score());
            }
        } catch (IOToolsException e) {
            e.printStackTrace();
        }
    }

}
