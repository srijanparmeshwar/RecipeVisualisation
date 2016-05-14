package uk.ac.cam.sp715.evaluation;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.w3c.dom.Node;
import uk.ac.cam.sp715.util.Pipeline;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides a graph kernel implementation for unweighted DAGs with
 * strings as vertices. The similarity index is calculated
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class FlowchartComparisons {

    private static class NodeBag extends HashMap<String, Integer> {
        private static AtomicLong currentID = new AtomicLong(0);
        private final long id;
        public NodeBag() {
            id = currentID.getAndIncrement();
        }
        public void put(String lemma) {
            if(containsKey(lemma)) put(lemma, get(lemma) + 1);
            else put(lemma, 1);
        }
        public int get(String lemma) {
            if(containsKey(lemma)) return super.get(lemma);
            else return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            NodeBag nodeBag = (NodeBag) o;

            return id == nodeBag.id;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (int) (id ^ (id >>> 32));
            return result;
        }
    }

    private static final Pipeline pipeline = Pipeline.getLemmaPipeline();
    private final Map<NodeBag, NodeBag> correspondences;
    private final Map<NodeBag, NodeBag> inverseCorrespondences;
    private final double nodeScore;
    private final double edgeScore;

    public FlowchartComparisons(DirectedGraph<Vertex, DefaultEdge> graphA, DirectedGraph<Vertex, DefaultEdge> graphB) {
        DirectedGraph<NodeBag, DefaultEdge> cgraphA = convertGraph(graphA);
        DirectedGraph<NodeBag, DefaultEdge> cgraphB = convertGraph(graphB);
        correspondences = new HashMap<>();
        inverseCorrespondences = new HashMap<>();
        nodeScore = nodeScore(cgraphA, cgraphB);
        edgeScore = edgeScore(cgraphA, cgraphB);
    }

    private double nodeScore(DirectedGraph<NodeBag, DefaultEdge> graphA, DirectedGraph<NodeBag, DefaultEdge> graphB) {
        double intersection = 0;
        List<NodeBag> setA = new LinkedList<>(graphA.vertexSet());
        List<NodeBag> setB = new LinkedList<>(graphB.vertexSet());
        //System.out.print("(" + setA.size() +  ", " + setB.size() + ", " + ((double) Math.min(setA.size(), setB.size()) / (double) Math.max(setA.size(), setB.size())) + ")");

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
                if(score > 0.05) {
                    correspondences.put(bagA, bagB);
                    inverseCorrespondences.put(bagB, bagA);
                    intersection += 1;
                }
            }
        }

        return intersection / (graphA.vertexSet().size() + graphB.vertexSet().size() - intersection);
    }

    public double getNodeScore() {
        return nodeScore;
    }

    public double pathLength(DirectedGraph<NodeBag, DefaultEdge> graph, NodeBag source, NodeBag target) {
        List<DefaultEdge> shortestPath = DijkstraShortestPath.findPathBetween(graph, source, target);
        if(shortestPath != null) return shortestPath.size();
        else return Double.POSITIVE_INFINITY;
    }

    /*
    private static String collect(NodeBag c) {
        return c.keySet().stream().collect(Collectors.joining(" "));
    }*/

    private double asymmetricEdgeScore(DirectedGraph<NodeBag, DefaultEdge> graphA, DirectedGraph<NodeBag, DefaultEdge> graphB, Map<NodeBag, NodeBag> correspondences) {
        double intersection = 0;
        double i = 0;
        double u = 0;
        /*
        for(NodeBag A1 : graphA.vertexSet()) {
            if(correspondences.containsKey(A1)) {
                for(DefaultEdge edge : graphA.outgoingEdgesOf(A1)) {
                    NodeBag A2 = graphA.getEdgeTarget(edge);
                    if(correspondences.containsKey(A2)) {
                        NodeBag B1 = correspondences.get(A1);
                        NodeBag B2 = correspondences.get(A2);
                        double pathLength = pathLength(graphB, B1, B2);
                        double norm = norm(pathLength);
                        intersection += norm;
                        i += norm;
                        //System.out.println("(" + collect(A1) + ", " + collect(A2) + ") ~ " + pathLength + " ~ " + norm + " ~ (" + collect(B1) + ", " + collect(B2) + ")");
                        //if(pathExists(graph, correspondences.get(A1), correspondences.get(A2))) intersection++;
                    }
                    u++;
                }
            }
        }
        return i / Math.max(u, 1);*/
        for(NodeBag key : correspondences.keySet()) {
            NodeBag value = correspondences.get(key);
            for(DefaultEdge edge : graphA.outgoingEdgesOf(key)) {
                NodeBag keyTarget = graphA.getEdgeTarget(edge);
                double norm = 0;
                if(correspondences.containsKey(keyTarget)) {
                    NodeBag valueTarget = correspondences.get(keyTarget);
                    double pathLength = pathLength(graphB, value, valueTarget);
                    norm = norm(pathLength);
                }
                i += norm;
                u++;
            }
        }

        return i / Math.max(1, u);
        //return intersection / (graphA.edgeSet().size() + graphB.edgeSet().size() - intersection);
    }

    private double edgeScore(DirectedGraph<NodeBag, DefaultEdge> graphA, DirectedGraph<NodeBag, DefaultEdge> graphB) {
        return (asymmetricEdgeScore(graphA, graphB, correspondences) + asymmetricEdgeScore(graphB, graphA, inverseCorrespondences)) / 2;
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
     * Modified Jaccard similarity index, extended to multiset i.e.
     * similarity(A, B) = |intersection(A, B)| / (|A| + |B| - |intersection(A, B)|)
     * Sum of minimum of counts divided by sum of maximum of counts.
     * @param A set A.
     * @param B set B.
     * @return {@link Double} Similarity index
     */
    private double nodeSimilarity(NodeBag A, NodeBag B) {
        if(A.size() == 0 || B.size() == 0) return 0;
        else {
            double intersection = 0;
            double union = 0;
            Set<String> keys = new HashSet<>();
            keys.addAll(A.keySet());
            keys.addAll(B.keySet());
            for(String string : keys) {
                int ACount = A.get(string);
                int BCount = B.get(string);
                intersection += Math.min(ACount, BCount);
                union += Math.max(ACount, BCount);
            }
            return intersection / union;
        }
    }

    private static double norm(double x) {
        double alpha = 2;
        double shift = 3;
        double correction = Math.exp(alpha * (1 - shift));
        return 1 / (1 - correction + Math.exp(alpha * (x - shift)));
    }

    public static void main(String[] args) {
        for(double x : new double[] {1, 2, 3, 4}) {
            System.out.println(norm(x));
        }
    }

}
