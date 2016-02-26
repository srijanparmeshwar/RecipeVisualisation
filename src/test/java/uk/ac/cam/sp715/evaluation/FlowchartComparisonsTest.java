package uk.ac.cam.sp715.evaluation;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by Srijan on 26/02/2016.
 */
public class FlowchartComparisonsTest {
    @Test
    public void testSameFlowchart() {
        DirectedGraph<Vertex, DefaultEdge> flowchart = new DefaultDirectedGraph<>(DefaultEdge.class);
        Vertex v0 = new Vertex(0, "Mix the chocolate and milk together.");
        Vertex v1 = new Vertex(1, "Put in pan.");
        Vertex v2 = new Vertex(2, "Line bowl with 5cm of paper.");
        Vertex v3 = new Vertex(3, "Add to the mixture.");

        flowchart.addVertex(v0);
        flowchart.addVertex(v1);
        flowchart.addVertex(v2);
        flowchart.addVertex(v3);

        flowchart.addEdge(v0, v1);
        flowchart.addEdge(v2, v3);

        FlowchartComparisons comparisons = new FlowchartComparisons(flowchart, flowchart);
        assertTrue(comparisons.getNodeScore() == 1);
        assertTrue(comparisons.getEdgeScore() == 1);
    }

    @Test
    public void testSameVertices() {
        DirectedGraph<Vertex, DefaultEdge> flowchartA = new DefaultDirectedGraph<>(DefaultEdge.class);
        Vertex u0 = new Vertex(0, "Mix the chocolate and milk together.");
        Vertex u1 = new Vertex(1, "Put in pan.");
        Vertex u2 = new Vertex(2, "Line bowl with 5cm of paper.");
        Vertex u3 = new Vertex(3, "Add to the mixture.");

        flowchartA.addVertex(u0);
        flowchartA.addVertex(u1);
        flowchartA.addVertex(u2);
        flowchartA.addVertex(u3);

        flowchartA.addEdge(u0, u1);
        flowchartA.addEdge(u2, u3);

        DirectedGraph<Vertex, DefaultEdge> flowchartB = new DefaultDirectedGraph<>(DefaultEdge.class);
        Vertex v0 = new Vertex(0, "Mix the chocolate and milk together.");
        Vertex v1 = new Vertex(1, "Put in pan.");
        Vertex v2 = new Vertex(2, "Line bowl with 5cm of paper.");
        Vertex v3 = new Vertex(3, "Add to the mixture.");

        flowchartB.addVertex(v0);
        flowchartB.addVertex(v1);
        flowchartB.addVertex(v2);
        flowchartB.addVertex(v3);

        flowchartB.addEdge(v1, v2);

        FlowchartComparisons comparisons = new FlowchartComparisons(flowchartA, flowchartB);
        assertTrue(comparisons.getNodeScore() == 1);
        assertTrue(comparisons.getEdgeScore() == 0);
    }

    @Test
    public void testDisjointFlowcharts() {
        DirectedGraph<Vertex, DefaultEdge> flowchartA = new DefaultDirectedGraph<>(DefaultEdge.class);
        Vertex u0 = new Vertex(0, "Mix the chocolate and milk together.");
        Vertex u1 = new Vertex(1, "Put in pan.");
        Vertex u2 = new Vertex(2, "Line bowl with 5cm of paper.");
        Vertex u3 = new Vertex(3, "Add to the mixture.");

        flowchartA.addVertex(u0);
        flowchartA.addVertex(u1);
        flowchartA.addVertex(u2);
        flowchartA.addVertex(u3);

        flowchartA.addEdge(u0, u1);
        flowchartA.addEdge(u2, u3);

        DirectedGraph<Vertex, DefaultEdge> flowchartB = new DefaultDirectedGraph<>(DefaultEdge.class);
        Vertex v0 = new Vertex(0, "Hello world!");
        Vertex v1 = new Vertex(1, "!dlrow olleH");

        flowchartB.addVertex(v0);
        flowchartB.addVertex(v1);

        flowchartB.addEdge(v0, v1);

        FlowchartComparisons comparisons = new FlowchartComparisons(flowchartA, flowchartB);
        assertTrue(comparisons.getNodeScore() == 0);
        assertTrue(comparisons.getEdgeScore() == 0);
    }
}
