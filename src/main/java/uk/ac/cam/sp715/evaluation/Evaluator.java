package uk.ac.cam.sp715.evaluation;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.parser.metrics.AbstractEval;
import edu.stanford.nlp.parser.metrics.Eval;
import edu.stanford.nlp.util.SystemUtils;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import uk.ac.cam.sp715.flows.Action;
import uk.ac.cam.sp715.flows.Flow;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.recognition.TaggedWord;
import uk.ac.cam.sp715.util.IOTools;
import uk.ac.cam.sp715.util.IOToolsException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Srijan on 21/02/2016.
 */
public class Evaluator {

    private static Map<Task, DirectedGraph<Vertex, DefaultEdge>> loadSystemFlowcharts() throws IOToolsException {
        Map<Task, Flow> recipes = IOTools.read(getPath("testflows.ser"));
        Map<Task, DirectedGraph<Vertex, DefaultEdge>> result = new HashMap<>();
        for(Task task : Task.values()) {
            Flow flow = recipes.get(task);
            DirectedGraph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
            Map<Action, Vertex> map = new HashMap<>();
            for(Action action : flow.vertexSet()) {
                String lemmas = action.description() + " " +
                        action.getObjects()
                                .stream()
                                .map(TaggedWord::getTokens)
                                .flatMap(List::stream)
                                .map(IndexedWord::lemma)
                                .collect(Collectors.joining(" "));
                Vertex vertex = new Vertex(action.getID(), lemmas);
                map.put(action, vertex);
                graph.addVertex(vertex);
            }

            for(DefaultEdge edge : flow.edgeSet()) {
                Action src = flow.getEdgeSource(edge);
                Action dst = flow.getEdgeTarget(edge);
                graph.addEdge(map.get(src), map.get(dst));
            }

            result.put(task, graph);
        }

        return result;
    }

    private static String getPath(String path) {
        return Paths.get("flowcharts", path).toString();
    }

    private static String loadAsString(String path) throws IOException {
        return Files.readAllLines(Paths.get("flowcharts", path)).stream().collect(Collectors.joining());
    }

    private static List<Map<Task, DirectedGraph<Vertex, DefaultEdge>>> loadHumanFlowcharts() throws IOException {
        List<Map<Task, DirectedGraph<Vertex, DefaultEdge>>> flowcharts = new LinkedList<>();
        FlowchartDOTParser parser = new FlowchartDOTParser();
        for(int i = 1; i < 4; i++) {
            Map<Task, DirectedGraph<Vertex, DefaultEdge>> map = new HashMap<>();
            for(Task task : Task.values()) map.put(task, parser.parse(loadAsString(i + "/task" + task + ".dot")));
            flowcharts.add(map);
        }
        return flowcharts;
    }

    private static void evaluate() throws IOToolsException, IOException {
        List<Map<Task, DirectedGraph<Vertex, DefaultEdge>>> humanFlowcharts = loadHumanFlowcharts();
        Map<Task, DirectedGraph<Vertex, DefaultEdge>> systemFlowcharts = loadSystemFlowcharts();
        for(Task task : Task.values()) {
            System.out.println("Task: " + task);
            System.out.println("--------------");
            System.out.println();

            System.out.println("\tInterannotator scores: " + task);
            System.out.println("\t--------------");
            System.out.println();

            double inodeSum = 0;
            double snodeSum = 0;
            double iedgeSum = 0;
            double sedgeSum = 0;
            double icount = 0;
            double scount = 0;
            for(int i = 0; i < humanFlowcharts.size(); i++) {
                DirectedGraph<Vertex, DefaultEdge> graphA = humanFlowcharts.get(i).get(task);
                for(int j = 0; j < humanFlowcharts.size(); j++) {
                    if(i < j) {
                        DirectedGraph<Vertex, DefaultEdge> graphB = humanFlowcharts.get(j).get(task);
                        System.out.println("\t\tParticipant " + (i + 1) + " & Participant " + (j + 1));
                        System.out.println("\t\t--------------");
                        System.out.println();

                        FlowchartComparisons comparisons = new FlowchartComparisons(graphA, graphB);
                        double nodeScore = comparisons.getNodeScore();
                        double edgeScore = comparisons.getEdgeScore();
                        System.out.println("\t\tNode similarity: " + nodeScore);
                        System.out.println("\t\tEdge similarity: " + edgeScore);
                        System.out.println();

                        inodeSum += nodeScore;
                        iedgeSum += edgeScore;
                        icount++;
                    }
                }
            }

            for(int i = 0; i < humanFlowcharts.size(); i++) {
                DirectedGraph<Vertex, DefaultEdge> graphA = humanFlowcharts.get(i).get(task);

                DirectedGraph<Vertex, DefaultEdge> systemGraph = systemFlowcharts.get(task);
                System.out.println("\t\tParticipant " + (i + 1) + " & System");
                System.out.println("\t\t--------------");
                System.out.println();

                FlowchartComparisons comparisons = new FlowchartComparisons(graphA, systemGraph);
                double nodeScore = comparisons.getNodeScore();
                double edgeScore = comparisons.getEdgeScore();
                System.out.println("\t\tNode similarity: " + nodeScore);
                System.out.println("\t\tEdge similarity: " + edgeScore);
                System.out.println();

                snodeSum += nodeScore;
                sedgeSum += edgeScore;
                scount++;
            }


            System.out.println("\tInterannotator node average: " + inodeSum / icount);
            System.out.println("\tInterannotator edge average: " + iedgeSum / icount);

            System.out.println("\tSystem node average: " + snodeSum / scount);
            System.out.println("\tSystem edge average: " + sedgeSum / scount);
        }
    }

    public static void main(String[] args) throws IOToolsException, IOException {
        evaluate();
    }

}
