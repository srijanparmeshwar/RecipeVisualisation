package uk.ac.cam.sp715.wordnet;

import edu.mit.jwi.item.ISynset;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class to represent the hypernym/hyponym taxonomy from  WordNet. This allows simpler
 * exploration as compared to the JWI library.
 */
public class Taxonomy extends DefaultDirectedGraph<ISynset, DefaultEdge> {
    private final ISynset root;
    public Taxonomy(ISynset synset) {
        super(DefaultEdge.class);
        this.root = synset;
        this.addVertex(synset);
    }

    //Copy constructor
    public Taxonomy(Taxonomy graph) {
        super(DefaultEdge.class);
        this.root = graph.getRoot();
        for(ISynset synset : graph.vertexSet()) {
            this.addVertex(synset);
        }
        for(DefaultEdge edge : graph.edgeSet()) {
            ISynset source = graph.getEdgeSource(edge);
            ISynset target = graph.getEdgeTarget(edge);
            this.addEdge(source, target);
        }
    }

    public ISynset getRoot() {
        return root;
    }

    public Set<ISynset> getLeaves() {
        Set<ISynset> leaves = new HashSet<>();
        Set<DefaultEdge> explorationSet = this.edgesOf(root);
        if(explorationSet.isEmpty()) leaves.add(root);
        else {
            while(!explorationSet.isEmpty()) {
                Set<DefaultEdge> newExplorationSet = new HashSet<>();
                for(DefaultEdge edge : explorationSet) {
                    ISynset target = this.getEdgeTarget(edge);
                    Set<DefaultEdge> newEdges = this.edgesOf(target).stream().filter((newEdge) -> !this.getEdgeTarget(newEdge).equals(target)).collect(Collectors.toSet());
                    if(newEdges.isEmpty()) leaves.add(target);
                    else newExplorationSet.addAll(newEdges);
                }
                explorationSet = newExplorationSet;
            }
        }
        return leaves;
    }
}