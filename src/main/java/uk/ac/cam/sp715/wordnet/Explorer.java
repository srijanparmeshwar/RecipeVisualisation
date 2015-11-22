package uk.ac.cam.sp715.wordnet;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
import org.jgrapht.Graphs;
import uk.ac.cam.sp715.util.Logging;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Srijan on 12/11/2015.
 */
public class Explorer implements AutoCloseable {
    private final IDictionary dictionary;

    private static final Logger logger = Logging.getLogger(Explorer.class.getName());

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
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error finding path to WordNet. Please check WNHOME environment variable and WordNet installation.", e);
            throw new WordNetException();
        }
    }

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
                    if(Graphs.addGraph(result, newGraph)) {
                        result.addEdge(synset, newSynset);
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

    public Taxonomy exploreHyponyms(Taxonomy graph) {
        return exploreHyponyms(graph, Integer.MAX_VALUE);
    }

    public static void main(String[] args) {
        Explorer explorer = new Explorer();
        explorer.open();
        //Map<IWordID, Set<ISynset>> synsets = explorer.explore("milk", POS.NOUN);
        Taxonomy.getTaxonomy(Taxonomy.TaxonomyType.APPLIANCES)
                .vertexSet()
                .forEach(System.out::println);
        System.out.println();
        Taxonomy.getTaxonomy(Taxonomy.TaxonomyType.UTENSILS)
                .vertexSet()
                .forEach(System.out::println);
        System.out.println();
        Taxonomy.getTaxonomy(Taxonomy.TaxonomyType.INGREDIENTS)
                .vertexSet()
                .forEach(System.out::println);
        /*for(IWordID indexWordID : synsets.keySet()) {
            System.out.println();
            System.out.println(indexWordID);
            System.out.println();
            for(ISynset synset : synsets.get(indexWordID)) {
                System.out.println(synset.getWords());

            }
        }*/
        explorer.close();
    }

    @Override
    public void close() {
        dictionary.close();
    }
}
