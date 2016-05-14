package uk.ac.cam.sp715.evaluation;

import edu.stanford.nlp.ling.IndexedWord;
import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import uk.ac.cam.sp715.flows.*;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.recognition.TaggedWord;
import uk.ac.cam.sp715.util.*;

import java.awt.*;
import java.io.*;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Srijan on 21/02/2016.
 */
public class Evaluator {

    private static final int SYSTEM = 0;

    private static class Row extends TreeMap<Integer, String> {
        public String toCSV(String delimiter) {
            StringBuilder rowString = new StringBuilder();
            for(int key = 0; key <= this.lastKey(); key++) {
                if(this.containsKey(key)) rowString.append(this.get(key));
                if(key != this.lastKey()) rowString.append(delimiter);
            }
            return rowString.toString();
        }
    }

    private static class Table extends TreeMap<Integer, Row> {

        public void put(int row, int column, String value) {
            if(!this.containsKey(row)) this.put(row, new Row());
            this.get(row).put(column, value);
        }

        public void put(int row, int column, Object value) {
            this.put(row, column, value.toString());
        }

        public List<String> toCSV(String delimiter) {
            return this.values()
                    .stream()
                    .map(row -> row.toCSV(delimiter))
                    .collect(Collectors.toList());
        }

        public List<String> toTex(String delimiter) {
            return this.values()
                    .stream()
                    .map(row -> row.toCSV(delimiter).replaceAll("Participant ", "P") + "\\\\\n\\hline")
                    .collect(Collectors.toList());
        }
    }

    private static class EvaluationTable extends Table {
        public EvaluationTable(int participants) {
            super();
            put(0, SYSTEM + 1, "System");
            put(SYSTEM + 1, 0, "System");
            put(SYSTEM + 1, SYSTEM + 1, round(1));
            for(int i = 1; i <= participants; i++) {
                put(0, i + 1, "Participant " + i);
                put(i + 1, 0, "Participant " + i);
                put(i + 1, i + 1, round(1));
            }
        }

        public void addResult(int participantA, int participantB, double value) {
            put(participantA + 1, participantB + 1, value);
        }

        public void addResult(int participantA, int participantB, String value) {
            put(participantA + 1, participantB + 1, value);
        }
    }

    private static DirectedGraph<Vertex, DefaultEdge> convert(Flow flow, Recipe recipe) {
        DirectedGraph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Map<Action, Vertex> map = new HashMap<>();

        for(Action action : flow.vertexSet()) {
            String description = recipe.getDescription().substring(action.start(), action.end());
            Vertex vertex = new Vertex(action.getID(), description);
            map.put(action, vertex);
            graph.addVertex(vertex);
        }

        for(DefaultEdge edge : flow.edgeSet()) {
            Action src = flow.getEdgeSource(edge);
            Action dst = flow.getEdgeTarget(edge);
            graph.addEdge(map.get(src), map.get(dst));
        }

        return graph;
    }

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
        for(int i = 1; i < 7; i++) {
            if(i != 0) {
                Map<Task, DirectedGraph<Vertex, DefaultEdge>> map = new HashMap<>();
                for(Task task : Task.values()) map.put(task, parser.parse(loadAsString(i + "/task" + task + ".dot")));
                flowcharts.add(map);
            }
        }
        return flowcharts;
    }

    private static String round(double x) {
        String colour = "";
        if(x > 0.8) {
            colour = "\\cellcolor{c5}";
        } else if(x > 0.6) {
            colour = "\\cellcolor{c4}";
        } else if(x > 0.4) {
            colour = "\\cellcolor{c3}";
        } else if(x > 0.2) {
            colour = "\\cellcolor{c2}";
        } else {
            colour = "\\cellcolor{c1}";
        }
        DecimalFormat df = new DecimalFormat("0.00");
        df.setRoundingMode(RoundingMode.CEILING);
        return colour + df.format(x);
    }

    private static void evaluate() throws IOToolsException, IOException {
        List<Map<Task, DirectedGraph<Vertex, DefaultEdge>>> humanFlowcharts = loadHumanFlowcharts();
        Map<Task, DirectedGraph<Vertex, DefaultEdge>> systemFlowcharts = loadSystemFlowcharts();

        double nSum = 0;
        double eSum = 0;
        double nSum2 = 0;
        double eSum2 = 0;
        double n = 0;

        double nMean = nSum / n;
        double eMean = eSum / n;
        double nVar = (nSum2 - nSum * nSum / n) / (n - 1) ;
        double eVar = (eSum2 - eSum * eSum / n) / (n - 1) ;
        double nSTV = Math.sqrt(nVar);
        double eSTV = Math.sqrt(eVar);
        double nSEM = nSTV / Math.sqrt(n);
        double eSEM = eSTV / Math.sqrt(n);

        for(Task task : Task.values()) {
            EvaluationTable nodeTable = new EvaluationTable(humanFlowcharts.size());
            EvaluationTable edgeTable = new EvaluationTable(humanFlowcharts.size());
            System.out.println("Task: " + task);
            nSum = 0;
            eSum = 0;
            nSum2 = 0;
            eSum2 = 0;
            n = 0;
            for(int i = 0; i < humanFlowcharts.size(); i++) {
                DirectedGraph<Vertex, DefaultEdge> graphA = humanFlowcharts.get(i).get(task);
                for(int j = 0; j < humanFlowcharts.size(); j++) {
                    if(i < j) {
                        DirectedGraph<Vertex, DefaultEdge> graphB = humanFlowcharts.get(j).get(task);
                        FlowchartComparisons comparisons = new FlowchartComparisons(graphA, graphB);
                        //System.out.println(" -> " + (i + 1) + " ~ " + comparisons.getNodeScore() + " ~ " + (j + 1));
                        nodeTable.addResult(i + 1, j + 1, round(comparisons.getNodeScore()));
                        nodeTable.addResult(j + 1, i + 1, round(comparisons.getNodeScore()));
                        edgeTable.addResult(i + 1, j + 1, round(comparisons.getEdgeScore()));
                        edgeTable.addResult(j + 1, i + 1, round(comparisons.getEdgeScore()));
                        nSum += comparisons.getNodeScore();
                        nSum2 += comparisons.getNodeScore() * comparisons.getNodeScore();
                        eSum += comparisons.getEdgeScore();
                        eSum2 += comparisons.getEdgeScore() * comparisons.getEdgeScore();
                        n++;
                    }
                }
            }

            nMean = nSum / n;
            eMean = eSum / n;
            nVar = (nSum2 - nSum * nSum / n) / (n - 1) ;
            eVar = (eSum2 - eSum * eSum / n) / (n - 1) ;
            nSTV = Math.sqrt(nVar);
            eSTV = Math.sqrt(eVar);
            nSEM = nSTV / Math.sqrt(n);
            eSEM = eSTV / Math.sqrt(n);
            System.out.println("Human nodes: [" + nMean + ", " + nSTV + ", " + nSEM + "]");
            System.out.println("Human edges: [" + eMean + ", " + eSTV + ", " + eSEM + "]");

            nSum = 0;
            eSum = 0;
            nSum2 = 0;
            eSum2 = 0;
            n = 0;
            for(int i = 0; i < humanFlowcharts.size(); i++) {
                DirectedGraph<Vertex, DefaultEdge> graphA = humanFlowcharts.get(i).get(task);
                DirectedGraph<Vertex, DefaultEdge> systemGraph = systemFlowcharts.get(task);
                FlowchartComparisons comparisons = new FlowchartComparisons(graphA, systemGraph);
                //System.out.println(" -> " + (i + 1) + " ~ " + comparisons.getNodeScore() + " ~ System");
                nodeTable.addResult(SYSTEM, i + 1, round(comparisons.getNodeScore()));
                nodeTable.addResult(i + 1, SYSTEM, round(comparisons.getNodeScore()));
                edgeTable.addResult(SYSTEM, i + 1, round(comparisons.getEdgeScore()));
                edgeTable.addResult(i + 1, SYSTEM, round(comparisons.getEdgeScore()));
                nSum += comparisons.getNodeScore();
                nSum2 += comparisons.getNodeScore() * comparisons.getNodeScore();
                eSum += comparisons.getEdgeScore();
                eSum2 += comparisons.getEdgeScore() * comparisons.getEdgeScore();
                n++;
            }

            nMean = nSum / n;
            eMean = eSum / n;
            nVar = (nSum2 - nSum * nSum / n) / (n - 1) ;
            eVar = (eSum2 - eSum * eSum / n) / (n - 1) ;
            nSTV = Math.sqrt(nVar);
            eSTV = Math.sqrt(eVar);
            nSEM = nSTV / Math.sqrt(n);
            eSEM = eSTV / Math.sqrt(n);
            System.out.println("System nodes: [" + nMean + ", " + nSTV + ", " + nSEM + "]");
            System.out.println("System edges: [" + eMean + ", " + eSTV + ", " + eSEM + "]");

            //nodeTable.toCSV(",").forEach(System.out::println);
            //edgeTable.toCSV(",").forEach(System.out::println);
            //IOTools.save(nodeTable.toCSV(","), Paths.get("flowcharts", task + "-node-results.csv"));
            //IOTools.save(edgeTable.toCSV(","), Paths.get("flowcharts", task + "-edge-results.csv"));
            IOTools.save(nodeTable.toTex(" & "), Paths.get("flowcharts", task + "-node-results.tex"));
            IOTools.save(edgeTable.toTex(" & "), Paths.get("flowcharts", task + "-edge-results.tex"));
        }
    }

    /**
     * Saves current system output for required recipes to be compared later on.
     * @throws IOException Thrown if error occurs reading test recipes file.
     * @throws IOToolsException Thrown if error occurs saving system output.
     */
    public static void saveSystemOutput() throws IOException, IOToolsException {
        Map<String, Recipe> recipes = IOTools.read(Paths.get("flowcharts/testrecipes.ser").toString());
        Visualiser visualiser = new HybridVisualiser(new CoreNLPVisualiser(Pipeline.getMainPipeline()));
        HashMap<Task, Flow> recipeFlows = new HashMap<>();
        for(Task task : Task.values()) {
            Flow flow = visualiser.parse(recipes.get(task.toString()));
            recipeFlows.put(task, flow);
        }
        IOTools.save(recipeFlows, Paths.get("flowcharts/testflows.ser").toString());
    }

    private static final String GRHOME = System.getenv("GRHOME");
    private static final DOTExporter<Vertex, DefaultEdge> exporter = new DOTExporter<>(vertex -> String.valueOf(vertex.getID()), Vertex::getLabel, defaultEdge -> "", action -> new HashMap<>(), defaultEdge -> new HashMap<>());

    public static String toDOT(DirectedGraph<Vertex, DefaultEdge> graph) {
        StringWriter writer = new StringWriter();
        exporter.export(writer, graph);
        writer.flush();
        String value = writer.toString();
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return value;
    }

    public static void saveEPS(DirectedGraph<Vertex, DefaultEdge> flow, String name) throws IOException {
        String dot = toDOT(flow);
        String[] split = dot.split("\r\n|\r|\n");
        List<String> s = new ArrayList<>(Arrays.asList(split));
        s.add(1, "\tnode [shape=\"box\"];");
        Process process = Runtime.getRuntime().exec(Paths.get(GRHOME, "dot -Teps -o " + name).toString());

        try(OutputStream stdin = process.getOutputStream();
            OutputStreamWriter bufferedOutputStream = new OutputStreamWriter(stdin);
            BufferedWriter writer = new BufferedWriter(bufferedOutputStream)) {
            writer.write(s.stream().collect(Collectors.joining("\n")));
            writer.flush();
        }
    }

    public static void renderFlowcharts() throws IOToolsException, IOException {
        /*Map<Task, Flow> flows = IOTools.read(getPath("testflows" + 4 + ".ser"));
        Map<String, Recipe> recipes = IOTools.read(Paths.get("flowcharts/testrecipes.ser").toString());
        for(Task task : Task.values()) {
            saveEPS(convert(flows.get(task), recipes.get(task.toString())), "s1" + task + ".eps");
        }*/
        List<Map<Task, DirectedGraph<Vertex, DefaultEdge>>> humanFlowcharts = loadHumanFlowcharts();
        int i = 1;
        for(Map<Task, DirectedGraph<Vertex, DefaultEdge>> flowcharts : humanFlowcharts) {
            for(Task task : Task.values()) {
                saveEPS(flowcharts.get(task), "p" + i + (task.toString()) + ".eps");
            }
            i++;
        }
    }

    static Color c1 = new Color(224, 224, 224);
    static Color c2 = new Color(224, 168, 168);
    static Color c3 = new Color(224, 112, 112);
    static Color c4 = new Color(224, 56, 56);
    static Color c5 = new Color(224, 0, 0);

    public static void main(String[] args) throws IOToolsException, IOException, HTMLParseException {
        //saveSystemOutput();
        //evaluate(1);
        //evaluate(2);
        //evaluate(3);
        //evaluate(4);
        //renderFlowcharts();
        evaluate();
    }

}
