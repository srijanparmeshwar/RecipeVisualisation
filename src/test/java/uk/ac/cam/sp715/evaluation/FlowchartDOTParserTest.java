package uk.ac.cam.sp715.evaluation;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created by Srijan on 26/02/2016.
 */
public class FlowchartDOTParserTest {

    private static final FlowchartDOTParser parser = new FlowchartDOTParser();
    private class EdgeSet extends HashMap<Vertex, Vertex> {
        public EdgeSet(DirectedGraph<Vertex, DefaultEdge> graph) {
            for(DefaultEdge edge : graph.edgeSet()) {
                put(graph.getEdgeSource(edge), graph.getEdgeTarget(edge));
            }
        }
    }

    @Test
    public void testParseSimpleGraph() {
        String dotGraph = "digraph {" +
                " 0 [label=\"Mix the chocolate and milk together.\"];" +
                "}";
        DirectedGraph<Vertex, DefaultEdge> expected = new DefaultDirectedGraph<>(DefaultEdge.class);
        expected.addVertex(new Vertex(0, "Mix the chocolate and milk together."));

        DirectedGraph<Vertex, DefaultEdge> result = parser.parse(dotGraph);
        assertEquals(expected.vertexSet(), result.vertexSet());
        assertEquals(new EdgeSet(expected), new EdgeSet(result));
    }

    @Test
    public void testParseGraph() {
        String dotGraph = "digraph {" +
                " 0 [label=\"Mix the chocolate and milk together.\"];" +
                " 1 [label=\"Put in pan.\"];" +
                " 2 [label=\"Line bowl with 5cm of paper.\"];" +
                " 3 [label=\"Add to the mixture.\"];" +
                " 0 -> 1;" +
                " 2 -> 3;" +
                "}";
        DirectedGraph<Vertex, DefaultEdge> expected = new DefaultDirectedGraph<>(DefaultEdge.class);
        Vertex v0 = new Vertex(0, "Mix the chocolate and milk together.");
        Vertex v1 = new Vertex(1, "Put in pan.");
        Vertex v2 = new Vertex(2, "Line bowl with 5cm of paper.");
        Vertex v3 = new Vertex(3, "Add to the mixture.");

        expected.addVertex(v0);
        expected.addVertex(v1);
        expected.addVertex(v2);
        expected.addVertex(v3);

        expected.addEdge(v0, v1);
        expected.addEdge(v2, v3);

        DirectedGraph<Vertex, DefaultEdge> result = parser.parse(dotGraph);
        assertEquals(expected.vertexSet(), result.vertexSet());
        assertEquals(new EdgeSet(expected), new EdgeSet(result));
    }
}
