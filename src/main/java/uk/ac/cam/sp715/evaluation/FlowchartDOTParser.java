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
 * Parses DOT format digraphs which have labels for nodes and
 * unlabelled edges.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class FlowchartDOTParser {

    /**
     * Parse DOT format string which should be of the format:
     *  digraph name {
     *      // Nodes
     *      node [label="..."];
     *      node [label="..."];
     *      ...
     *      // Edges
     *      node -> node;
     *      node -> node;
     *      ...
     *  }
     * Comments are not allowed.
     * @param graph Digraph in restricted DOT format.
     * @return DirectedGraph with vertices and default edges.
     */
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
}
