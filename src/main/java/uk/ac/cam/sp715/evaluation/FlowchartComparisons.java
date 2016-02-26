package uk.ac.cam.sp715.evaluation;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import uk.ac.cam.sp715.flows.CoreNLPVisualiser;
import uk.ac.cam.sp715.flows.Flow;
import uk.ac.cam.sp715.flows.HybridVisualiser;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.util.IOTools;
import uk.ac.cam.sp715.util.IOToolsException;
import uk.ac.cam.sp715.util.Pipeline;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Srijan on 20/02/2016.
 */
public class FlowchartComparisons {

    private static class NodeBag extends HashMap<String, Integer> {
        public void put(String lemma) {
            if(containsKey(lemma)) put(lemma, get(lemma) + 1);
            else put(lemma, 1);
        }
        public int get(String lemma) {
            if(containsKey(lemma)) return super.get(lemma);
            else return 0;
        }
    }

    private static final Pipeline pipeline = Pipeline.getLemmaPipeline();
    private final Map<NodeBag, NodeBag> correspondences;
    private final double nodeScore;
    private final double edgeScore;

    public FlowchartComparisons(DirectedGraph<Vertex, DefaultEdge> graphA, DirectedGraph<Vertex, DefaultEdge> graphB) {
        DirectedGraph<NodeBag, DefaultEdge> cgraphA = convertGraph(graphA);
        DirectedGraph<NodeBag, DefaultEdge> cgraphB = convertGraph(graphB);
        correspondences = new HashMap<>();
        nodeScore = nodeScore(cgraphA, cgraphB);
        edgeScore = edgeScore(cgraphA, cgraphB);
    }

    private double nodeScore(DirectedGraph<NodeBag, DefaultEdge> graphA, DirectedGraph<NodeBag, DefaultEdge> graphB) {
        double intersection = 0;
        List<NodeBag> setA = new LinkedList<>(graphA.vertexSet());
        List<NodeBag> setB = new LinkedList<>(graphB.vertexSet());

        double[][] scores = new double[setA.size()][setB.size()];
        double[][] costs = new double[setA.size()][setB.size()];
        for(int i = 0; i < setA.size(); i++) {
            NodeBag bagA = setA.get(i);
            for(int j = 0; j < setB.size(); j++) {
                NodeBag bagB = setB.get(j);
                scores[i][j] = nodeSimilarity(bagA, bagB);
                costs[i][j] = 1 - scores[i][j];
            }
        }

        HungarianAlgorithm algorithm = new HungarianAlgorithm(costs);
        int[] assignments = algorithm.execute();
        for(int i = 0; i < assignments.length; i++) {
            int assignment = assignments[i];
            if(assignment >= 0) {
                NodeBag bagA = setA.get(i);
                NodeBag bagB = setB.get(assignment);
                double score = scores[i][assignment];
                if(score > 0) {
                    correspondences.put(bagA, bagB);
                    intersection += score;
                }
            }
        }

        return intersection / (graphA.vertexSet().size() + graphB.vertexSet().size() - intersection);
    }

    public double getNodeScore() {
        return nodeScore;
    }

    public double pathLength(DirectedGraph<NodeBag, DefaultEdge> graph, NodeBag source, NodeBag target) {
        if(graph.containsEdge(source, target)) return 1;
        else {
            double min = Double.POSITIVE_INFINITY;
            for(NodeBag bag : graph.outgoingEdgesOf(source)
                    .stream()
                    .map(graph::getEdgeTarget)
                    .collect(Collectors.toSet())) {
                double pathLength = pathLength(graph, bag, target);
                if(pathLength < min && Double.isFinite(pathLength) && pathLength > 0) min = pathLength;
            }
            return min + 1;
        }
    }

    private static String collect(NodeBag c) {
        return c.keySet().stream().collect(Collectors.joining(" "));
    }

    private double edgeScore(DirectedGraph<NodeBag, DefaultEdge> graphA, DirectedGraph<NodeBag, DefaultEdge> graphB) {
        double intersection = 0;
        for(NodeBag A1 : graphA.vertexSet()) {
            if(correspondences.containsKey(A1)) {
                for(DefaultEdge edge : graphA.outgoingEdgesOf(A1)) {
                    NodeBag A2 = graphA.getEdgeTarget(edge);
                    if(correspondences.containsKey(A2)) {
                        NodeBag B1 = correspondences.get(A1);
                        NodeBag B2 = correspondences.get(A2);
                        double pathLength = pathLength(graphB, B1, B2);
                        double norm = Math.exp(1.0 - 1.0 * pathLength);
                        intersection += norm;
                        //System.out.println("(" + collect(A1) + ", " + collect(A2) + ") ~ " + pathLength + " ~ " + norm + " ~ (" + collect(B1) + ", " + collect(B2) + ")");
                        //if(pathExists(graph, correspondences.get(A1), correspondences.get(A2))) intersection++;
                    }
                }
            }
        }
        return intersection / (graphA.edgeSet().size() + graphB.edgeSet().size() - intersection);
    }

    public double getEdgeScore() {
        return edgeScore;
    }

    private DirectedGraph<NodeBag, DefaultEdge> convertGraph(DirectedGraph<Vertex, DefaultEdge> graph) {
        DirectedGraph<NodeBag, DefaultEdge> result = new DefaultDirectedGraph<>(DefaultEdge.class);
        Map<Vertex, NodeBag> map = new HashMap<>();
        for(Vertex vertex : graph.vertexSet()) {
            Annotation annotation = new Annotation(vertex.getLabel());
            pipeline.annotate(annotation);
            NodeBag bag = new NodeBag();
            for(CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                for(CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    bag.put(token.lemma().toLowerCase());
                }
            }
            map.put(vertex, bag);
            result.addVertex(bag);
        }

        for(DefaultEdge edge : graph.edgeSet()) {
            Vertex src = graph.getEdgeSource(edge);
            Vertex dst = graph.getEdgeTarget(edge);
            result.addEdge(map.get(src), map.get(dst));
        }

        return result;
    }

    /**
     * Jacccard similarity index extended to multiset i.e. similarity(A, B) = |intersection(A, B)| / (|A| + |B| - |intersection(A, B)|)
     * @param A set A.
     * @param B set B.
     * @return {@link Double} Similarity index
     */
    private double nodeSimilarity(NodeBag A, NodeBag B) {
        if(A.size() == 0 || B.size() == 0) return 0;
        else {
            double intersection = 0;
            double union = 0;
            for(String string : A.keySet()) {
                int ACount = A.get(string);
                int BCount = B.get(string);
                intersection += Math.min(ACount, BCount);
                union += Math.max(ACount, BCount);
            }
            return intersection / union;
        }
    }

    public static void saveSystemOutput() throws IOException, IOToolsException {
        Map<String, Recipe> recipes = IOTools.read(Paths.get("flowcharts/testrecipes.ser").toString());
        HybridVisualiser visualiser = new HybridVisualiser(new CoreNLPVisualiser(Pipeline.getMainPipeline()));
        HashMap<Task, Flow> recipeFlows = new HashMap<>();
        for(Task task : Task.values()) {
            Flow flow = visualiser.parse(recipes.get(task.toString()));
            recipeFlows.put(task, flow);
        }
        IOTools.save(recipeFlows, Paths.get("flowcharts/testflows.ser").toString());
    }

}
