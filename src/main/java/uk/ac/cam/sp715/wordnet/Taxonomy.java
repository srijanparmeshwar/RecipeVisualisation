package uk.ac.cam.sp715.wordnet;

import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class to represent the hypernym/hyponym taxonomy from  WordNet. This allows simpler
 * exploration as compared to the JWI library.
 */
public class Taxonomy extends DefaultDirectedGraph<ISynset, DefaultEdge> {
    private final ISynset root;

    /**
     * Constructs a singleton taxonomy with input
     * as the root vertex.
     * @param root - Root vertex.
     */
    public Taxonomy(ISynset root) {
        super(DefaultEdge.class);
        this.root = root;
        this.addVertex(root);
    }

    /**
     * Copy constructor which provides a deep copy of the input.
     * @param original - Taxonomy to be copied.
     */
    public Taxonomy(Taxonomy original) {
        super(DefaultEdge.class);
        this.root = original.getRoot();
        for(ISynset vertex : original.vertexSet()) {
            this.addVertex(vertex);
        }
        for(DefaultEdge edge : original.edgeSet()) {
            ISynset source = original.getEdgeSource(edge);
            ISynset target = original.getEdgeTarget(edge);
            this.addEdge(source, target);
        }
    }

    /**
     * Accessor method to get root vertex.
     * @return {@link ISynset} - Root vertex.
     */
    public ISynset getRoot() {return root;}

    /**
     * Returns the leaves of this taxonomy, the vertices which have no outgoing
     * edges.
     * @return {@link Set}<{@link ISynset}> - Set of synsets at leaves.
     */
    public Set<ISynset> getLeaves() {
        ISynset root = getRoot();
        Set<ISynset> leaves = new HashSet<>();
        Set<DefaultEdge> explorationSet = this.edgesOf(root);
        Set<DefaultEdge> covered = new HashSet<>();
        if(explorationSet.isEmpty()) leaves.add(root);
        else {
            while(!explorationSet.isEmpty()) {
                Set<DefaultEdge> newExplorationSet = new HashSet<>();
                for(DefaultEdge edge : explorationSet) {
                    if(!covered.contains(edge)) {
                        ISynset target = this.getEdgeTarget(edge);
                        //Only outgoing edges from source.
                        Set<DefaultEdge> newEdges = this.edgesOf(target).parallelStream().filter((newEdge) -> !this.getEdgeTarget(newEdge).equals(target)).collect(Collectors.toSet());
                        if (newEdges.isEmpty()) leaves.add(target);
                        else newExplorationSet.addAll(newEdges);
                        covered.add(edge);
                    }
                }
                explorationSet = newExplorationSet;
            }
        }
        return leaves;
    }

    public enum TaxonomyType {
        APPLIANCES, UTENSILS, INGREDIENTS, NONE
    }

    private static Map<TaxonomyType, Taxonomy> TAXONOMIES;

    private static Taxonomy getNounTaxonomy(Explorer explorer, String noun) {
        List<IWordID> wordIDs = explorer.getIndexNoun(noun).getWordIDs();
        if(wordIDs.size()>0) {
            ISynset synset = explorer.getWord(wordIDs.get(0)).getSynset();
            return explorer.exploreHyponyms(new Taxonomy(synset));
        } else {
            throw new WordNetException();
        }
    }

    private static void initializeTaxonomies() {
        Explorer explorer = new Explorer();
        explorer.open();
        TAXONOMIES = new HashMap<>();
        Taxonomy ingredients = getNounTaxonomy(explorer, "food");
        Graphs.addGraph(ingredients, getNounTaxonomy(explorer, "solid food"));
        Graphs.addGraph(ingredients, getNounTaxonomy(explorer, "fat"));
        TAXONOMIES.put(TaxonomyType.APPLIANCES, getNounTaxonomy(explorer, "kitchen appliance"));
        TAXONOMIES.put(TaxonomyType.UTENSILS, getNounTaxonomy(explorer, "kitchen utensil"));
        TAXONOMIES.put(TaxonomyType.INGREDIENTS, ingredients);
        explorer.close();
    }

    public static Taxonomy getTaxonomy(TaxonomyType type) {
        if(TAXONOMIES == null) initializeTaxonomies();
        return TAXONOMIES.get(type);
    }

    public static Map<TaxonomyType, Taxonomy> getTaxonomies() {
        if(TAXONOMIES == null) initializeTaxonomies();
        return TAXONOMIES;
    }
}