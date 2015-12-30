package uk.ac.cam.sp715.wordnet;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import uk.ac.cam.sp715.util.Logging;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides exploration algorithms for WordNet.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class Explorer implements AutoCloseable {
    private final IDictionary dictionary;

    private static final Logger logger = Logging.getLogger(Explorer.class);

    public Explorer() {
        try {
            String wnhome = System.getenv("WNHOME");
            String path = wnhome + File.separator + "dict";
            URL url = new URL("file", null, path);
            dictionary = new Dictionary(url);
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Error finding path to WordNet. Please check WNHOME environment variable and WordNet installation.", e);
            throw new WordNetException();
        }
    }

    public void open() {
        try {
            dictionary.open();
            if(dictionary.isOpen()) {
                logger.log(Level.INFO, "Dictionary is open.");
            } else {
                logger.log(Level.SEVERE, "Error opening dictionary.");
                throw new WordNetException();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error finding path to WordNet. Please check WNHOME environment variable and WordNet installation.", e);
            throw new WordNetException();
        }
    }

    @Override
    public void close() {
        dictionary.close();
    }

    /**
     * Explores WordNet hyponym (is-a/subtype) relations for a given input graph.
     * It will recursively find hyponym synsets up to the given depth limit for each source vertex
     * i.e. the leaves of the input taxonomy. This method makes a copy of the input and works with this
     * so the original graph is not modified.
     * @param graph Taxonomy to be explored further via hyponym relations.
     * @param depth Maximum depth of taxonomy relative to the input leaves.
     * @return {@link Taxonomy} - The expanded taxonomy, including hyponym synsets up to the depth limit.
     */
    public Taxonomy exploreHyponyms(Taxonomy graph, int depth) {
        Taxonomy result = new Taxonomy(graph);
        if(depth>0) {
            Set<ISynset> leaves = result.getLeaves();
            for(ISynset synset : leaves) {
                List<ISynsetID> hyponymSets = synset.getRelatedSynsets(Pointer.HYPONYM);
                for (ISynsetID synsetID : hyponymSets) {
                    ISynset newSynset = dictionary.getSynset(synsetID);
                    Taxonomy newTaxonomy = new Taxonomy(newSynset);
                    Taxonomy newGraph = exploreHyponyms(newTaxonomy, depth - 1);
                    //If destination graph (result) is modified connect the synset vertices.
                    if(Graphs.addGraph(result, newGraph)) result.addEdge(synset, newSynset);
                }
            }
        }
        return result;
    }

    /**
     * Explores WordNet meronym (part-of) relations for a given input graph.
     * It will recursively find meronym synsets up to the given depth limit for each source vertex
     * i.e. the whole vertex set. This method makes a copy of the input and works with this
     * so the original graph is not modified.
     * @param graph Taxonomy to be explored further via meronym relations.
     * @param depth Maximum depth of meronymy relationships.
     * @return {@link Taxonomy} - The expanded taxonomy, including meronym synsets up to the depth limit.
     */
    public Taxonomy exploreMeronyms(Taxonomy graph, int depth) {
        Taxonomy result = new Taxonomy(graph);
        if(depth>0) {
            for(ISynset synset : graph.vertexSet()) {
                Set<ISynsetID> meronymSets = new HashSet<>();
                meronymSets.addAll(synset.getRelatedSynsets(Pointer.MERONYM_MEMBER));
                meronymSets.addAll(synset.getRelatedSynsets(Pointer.MERONYM_PART));
                meronymSets.addAll(synset.getRelatedSynsets(Pointer.MERONYM_SUBSTANCE));

                for(ISynsetID synsetID : meronymSets) {
                    ISynset newSynset = dictionary.getSynset(synsetID);
                    Taxonomy newTaxonomy = new Taxonomy(newSynset);
                    Taxonomy newGraph = exploreMeronyms(newTaxonomy, depth - 1);
                    //If destination graph (result) is modified connect the synset vertices.
                    if(Graphs.addGraph(result, newGraph)) {
                        for(DefaultEdge edge : result.incomingEdgesOf(synset)) {
                            for(ISynset target : newGraph.vertexSet()) {
                                result.addEdge(graph.getEdgeSource(edge), target);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public IIndexWord getIndexNoun(String noun) {
        return dictionary.getIndexWord(noun, POS.NOUN);
    }

    public IWord getWord(IWordID wordID) {
        return dictionary.getWord(wordID);
    }

    /**
     * This is the same as {@link #exploreHyponyms(Taxonomy, int)} however it essentially has no depth limit.
     * It calls {@link #exploreHyponyms(Taxonomy, int)} with {@link Integer#MAX_VALUE} as the depth limit.
     * @param graph Taxonomy to be explored further via hyponym relations.
     * @return {@link Taxonomy} - The expanded taxonomy, including hyponym synsets.
     */
    public Taxonomy exploreHyponyms(Taxonomy graph) {
        return exploreHyponyms(graph, Integer.MAX_VALUE);
    }

    /**
     * This is the same as {@link #exploreMeronyms(Taxonomy, int)} however it essentially has no depth limit.
     * It calls {@link #exploreHyponyms(Taxonomy, int)} with {@link Integer#MAX_VALUE} as the depth limit.
     * @param graph Taxonomy to be explored further via meroynm relations.
     * @return {@link Taxonomy} - The expanded taxonomy, including meronym synsets.
     */
    public Taxonomy exploreMeronyms(Taxonomy graph) {
        return exploreMeronyms(graph, Integer.MAX_VALUE);
    }
}
