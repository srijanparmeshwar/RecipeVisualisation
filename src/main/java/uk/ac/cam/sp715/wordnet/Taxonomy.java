package uk.ac.cam.sp715.wordnet;

import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWordID;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class to represent the hypernym/hyponym taxonomy from  WordNet. This allows simpler
 * exploration as compared to the JWI library. Vertices are WordNet synsets. Directed edges represent relations such that
 * iff there exists an edge {@code A -> B} then {@code A} is a hypernym of {@code B} and conversely {@code B} is a hyponym of {@code A},
 * or there is a holonym or meronym of {@code B} with these properties.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class Taxonomy extends DefaultDirectedGraph<ISynset, DefaultEdge> {

    /**
     * Constructs a taxonomy with the inputs as roots for further exploration.
     * @param roots - Root vertices.
     */
    public Taxonomy(Set<ISynset> roots) {
        super(DefaultEdge.class);
        roots.forEach(this::addVertex);
    }

    /**
     * Constructs a singleton taxonomy with input
     * as the root vertex.
     * @param root - Root vertex.
     */
    public Taxonomy(ISynset root) {
        super(DefaultEdge.class);
        this.addVertex(root);
    }

    /**
     * Copy constructor which provides a deep copy of the input.
     * @param original Taxonomy to be copied.
     */
    public Taxonomy(Taxonomy original) {
        super(DefaultEdge.class);
        original.vertexSet().forEach(this::addVertex);
        for(DefaultEdge edge : original.edgeSet()) {
            ISynset source = original.getEdgeSource(edge);
            ISynset target = original.getEdgeTarget(edge);
            this.addEdge(source, target);
        }
    }

    /**
     * Accessor method to get root vertices.
     * @return {@link Set}<{@link ISynset}> - Root vertices.
     */
    public Set<ISynset> getRoots() {return vertexSet()
            .parallelStream()
            .filter((vertex) -> incomingEdgesOf(vertex).isEmpty())
            .collect(Collectors.toCollection(HashSet::new));}

    /**
     * Returns the leaves of this taxonomy, the vertices which have no outgoing
     * edges.
     * @return {@link Set}<{@link ISynset}> - Set of synsets at leaves.
     */
    public Set<ISynset> getLeaves() {
        final Set<ISynset> roots = getRoots();
        final Set<ISynset> leaves = new HashSet<>();
        Set<DefaultEdge> explorationSet = new HashSet<>();
        for(ISynset root : roots) explorationSet.addAll(this.outgoingEdgesOf(root));
        Set<DefaultEdge> covered = new HashSet<>();
        if(explorationSet.isEmpty()) leaves.addAll(roots);
        else {
            while(!explorationSet.isEmpty()) {
                Set<DefaultEdge> newExplorationSet = new HashSet<>();
                for(DefaultEdge edge : explorationSet) {
                    //Stops cyclic exploration.
                    if(!covered.contains(edge)) {
                        ISynset target = this.getEdgeTarget(edge);
                        Set<DefaultEdge> newEdges = this.outgoingEdgesOf(target);
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

    /**
     * Types to identify entity types.
     */
    public enum TaxonomyType {
        APPLIANCES, UTENSILS, INGREDIENTS, OTHER
    }

    private static Map<TaxonomyType, Taxonomy> TAXONOMIES = initializeTaxonomies();

    private static Taxonomy getNounTaxonomy(Explorer explorer, String noun) {
        List<IWordID> wordIDs = explorer.getIndexNoun(noun).getWordIDs();
        if(wordIDs.size()>0) {
            ISynset synset = explorer.getWord(wordIDs.get(0)).getSynset();
            return explorer.exploreMeronyms(explorer.exploreHyponyms(new Taxonomy(synset)));
        } else {
            throw new WordNetException();
        }
    }

    private static Taxonomy getNounTaxonomy(Explorer explorer, String noun, int index) {
        List<IWordID> wordIDs = explorer.getIndexNoun(noun).getWordIDs();
        if(wordIDs.size()>0) {
            ISynset synset = explorer.getWord(wordIDs.get(index)).getSynset();
            return explorer.exploreMeronyms(explorer.exploreHyponyms(new Taxonomy(synset)));
        } else {
            throw new WordNetException();
        }
    }

    /**
     * Loads the collections for each type from the seeds.
     * Ingredients - food, solid food, fat.
     * Utensils - kitchen utensil, tableware.
     * Appliances - appliance.
     */
    private static Map<TaxonomyType, Taxonomy> initializeTaxonomies() {
        Explorer explorer = new Explorer();
        explorer.open();
        Map<TaxonomyType, Taxonomy> TAXONOMIES = new HashMap<>();
        Taxonomy ingredients = getNounTaxonomy(explorer, "food");
        Graphs.addGraph(ingredients, getNounTaxonomy(explorer, "solid food"));
        Graphs.addGraph(ingredients, getNounTaxonomy(explorer, "fat"));

        Taxonomy utensils = getNounTaxonomy(explorer, "kitchen utensil");
        Graphs.addGraph(utensils, getNounTaxonomy(explorer, "tableware"));

        TAXONOMIES.put(TaxonomyType.APPLIANCES, getNounTaxonomy(explorer, "appliance", 1));
        TAXONOMIES.put(TaxonomyType.UTENSILS, utensils);
        TAXONOMIES.put(TaxonomyType.INGREDIENTS, ingredients);
        explorer.close();
        return TAXONOMIES;
    }

    /**
     * Retrieves the entity type collections.
     * @return Collections for each entity type enumerated by {@link TaxonomyType}.
     */
    public static Map<TaxonomyType, Taxonomy> getTaxonomies() {
        return TAXONOMIES;
    }
}