package uk.ac.cam.sp715.evaluation;

import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Srijan on 20/02/2016.
 */
public class FlowchartDOTParser {

    public DirectedGraph<Vertex, DefaultEdge> parse(String graph) {
        String[] split = graph.split("\\{");
        if(split.length < 2) throw new ParseException();
        else {
            graph = split[1];
            split = graph.split("\\}");
            if(split.length < 1) throw new ParseException();
            else {
                graph = split[0];
                List<String> data = Arrays.asList(graph.split(";"));
                List<Vertex> nodes = new LinkedList<>();
                List<int[]> edges = new LinkedList<>();
                for(String string : data) {
                    if(string.contains("->")) edges.add(parseEdge(string));
                    else nodes.add(parseNode(string));
                }

                DirectedGraph<Vertex, DefaultEdge> directedGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

                Map<Integer, Vertex> map = new HashMap<>();
                for(Vertex Vertex : nodes) {
                    map.put(Vertex.getID(), Vertex);
                    directedGraph.addVertex(Vertex);
                }

                for(int[] pair : edges) {
                    directedGraph.addEdge(map.get(pair[0]), map.get(pair[1]));
                }

                return directedGraph;
            }
        }
    }

    private int parseInt(String string) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(string);
        if(matcher.find()) return Integer.parseInt(matcher.group());
        else throw new ParseException();
    }

    private String parseLabel(String string) {
        Pattern pattern = Pattern.compile("label=\"(.*)\"");
        Matcher matcher = pattern.matcher(string);
        if(matcher.find()) return matcher.group(1);
        else throw new ParseException();
    }

    private Vertex parseNode(String nodeString) {
        int id = parseInt(nodeString);
        String label = parseLabel(nodeString);
        return new Vertex(id, label);
    }

    private int[] parseEdge(String edgeString) {
        int[] result = new int[2];
        String[] split = edgeString.split("->");
        if(split.length > 1) {
            result[0] = parseInt(split[0]);
            result[1] = parseInt(split[1]);
            return result;
        } else throw new ParseException();
    }

    public static void main(String[] args) throws IOException {
        FlowchartDOTParser parser = new FlowchartDOTParser();
        String graphString = Files.readAllLines(Paths.get("flowcharts/1/taskB.dot")).stream().collect(Collectors.joining(""));
        DirectedGraph<Vertex, DefaultEdge> graph = parser.parse(graphString);
        DOTExporter<Vertex, DefaultEdge> exporter = new DOTExporter<>(Vertex -> Integer.toString(Vertex.getID()), Vertex::getLabel, defaultEdge -> "");
        StringWriter writer = new StringWriter();
        exporter.export(writer, graph);
        writer.flush();
        System.out.println(writer.toString());
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException();
        }

    }

}
