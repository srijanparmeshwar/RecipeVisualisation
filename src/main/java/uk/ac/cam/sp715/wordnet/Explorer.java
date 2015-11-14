package uk.ac.cam.sp715.wordnet;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
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

    public Set<ISynset> explore(String seed, POS seedPOS) {
        Set<ISynset> synsets = new HashSet<>();
        IIndexWord indexWord = dictionary.getIndexWord(seed, seedPOS);
        Set<ISynset> partialSynsets = getSynsets(indexWord);
        for(ISynset partialSynset : partialSynsets) {
            synsets.addAll(exploreHyponyms(partialSynset));
            synsets.add(partialSynset);
        }
        return synsets;
    }

    public Set<ISynset> exploreHyponyms(ISynset synset) {
        Set<ISynset> synsets = new HashSet<>();
        List<ISynsetID> hyponymSets = synset.getRelatedSynsets(Pointer.HYPONYM);
        synsets.add(synset);
        if(hyponymSets.size()>0) {
            hyponymSets.stream().map(dictionary::getSynset).forEach(tsynset -> synsets.addAll(exploreHyponyms(tsynset)));
        }
        return synsets;
    }

    public Set<ISynset> getSynsets(IIndexWord indexWord) {
        Set<ISynset> synsetMap = new HashSet<>();
        for(IWordID id : indexWord.getWordIDs()) {
            IWord word = dictionary.getWord(id);
            synsetMap.add(word.getSynset());
            word.getRelatedWords();
        }
        return synsetMap;
    }

    public static void main(String[] args) {
        Explorer explorer = new Explorer();
        explorer.open();
        for(ISynset synset : explorer.explore("cooking", POS.NOUN)) {
            System.out.println(synset.getWords());
        }
        explorer.close();
    }

    @Override
    public void close() {
        dictionary.close();
    }
}
