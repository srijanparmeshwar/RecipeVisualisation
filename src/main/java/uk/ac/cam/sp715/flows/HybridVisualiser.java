package uk.ac.cam.sp715.flows;

import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.ling.IndexedWord;
import org.jgrapht.graph.DefaultEdge;
import uk.ac.cam.sp715.ml.DataHandler;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.recognition.TaggedWord;
import uk.ac.cam.sp715.util.IOToolsException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Srijan on 18/01/2016.
 */
public class HybridVisualiser extends Visualiser {
    private final Classifier<Boolean, String> dependencyClassifier;
    private final CoreNLPVisualiser visualiser;
    public HybridVisualiser(CoreNLPVisualiser visualiser) {
        try {
            this.visualiser = visualiser;
            this.dependencyClassifier = DataHandler.getDependencyClassifier();
        } catch (IOToolsException | IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    @Override
    public Flow parse(Recipe recipe) {
        Flow flow = new Flow();
        Flow heuristicFlow = visualiser.parse(recipe);

        List<Action> actions = new ArrayList<>(heuristicFlow.vertexSet());
        Collections.sort(actions, (o1, o2) -> Integer.compare(o1.getID(), o2.getID()));
        for(Action action : actions) flow.addVertex(action);

        for(Action src : actions) {
            for(Action dst : actions) {
                if(src.getID() < dst.getID()) {
                    if(dependencyClassifier.classOf(FeatureVectors.getDatum(src, dst, heuristicFlow))) {
                        flow.addEdge(src, dst);
                    }
                }
            }
        }

        flow.mergeFlows(heuristicFlow);
        return flow;
    }
}
