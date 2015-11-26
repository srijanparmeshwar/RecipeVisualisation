package uk.ac.cam.sp715.flows;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Created by Srijan on 19/11/2015.
 */
public class Flow extends DefaultDirectedGraph<Node, DefaultEdge> {
    public Flow() {
        super(DefaultEdge.class);
    }

}
