package uk.ac.cam.sp715.recognition;

import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import uk.ac.cam.sp715.util.HTMLParseException;
import uk.ac.cam.sp715.util.HTMLParser;
import uk.ac.cam.sp715.wordnet.Explorer;
import uk.ac.cam.sp715.wordnet.Taxonomy;
import uk.ac.cam.sp715.wordnet.Taxonomy.TaxonomyType;

import java.util.*;

/**
 * Created by Srijan on 21/11/2015.
 */
public class EntityRecognizer implements AutoCloseable {
    private final Explorer explorer;
    private static Map<TaxonomyType, Set<String>> dictionaries;

    public EntityRecognizer() {
        explorer = new Explorer();
        if(dictionaries == null) initializeDictionaries();
    }

    public void open() {
        explorer.open();
    }

    public void close() {
        explorer.close();
    }

    private static void initializeDictionaries() {
        Map<TaxonomyType, Taxonomy> taxonomies = Taxonomy.getTaxonomies();
        dictionaries = new HashMap<>();
        for(TaxonomyType type : taxonomies.keySet()) dictionaries.put(type, new HashSet<>());

        for(TaxonomyType type : taxonomies.keySet()) {
            for(ISynset synset : taxonomies.get(type).vertexSet()) {
                for(IWord word : synset.getWords()) {
                    dictionaries.get(type).add(word.getLemma());
                }
            }
        }
    }
    public TaxonomyType getType(String noun) {
        IIndexWord indexWord = explorer.getIndexNoun(noun);
        if(indexWord != null) {
            for (IWordID wordID : indexWord.getWordIDs()) {
                String lemma = explorer.getWord(wordID).getLemma();
                for (TaxonomyType type : dictionaries.keySet()) {
                    if (dictionaries.get(type).contains(lemma)) return type;
                }
            }
        }
        return TaxonomyType.NONE;
    }

    public static void experimental() throws HTMLParseException {
        Properties props = new Properties();
        props.setProperty("annotators",
                "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation annotation = new Annotation(
                HTMLParser.getRecipe(HTMLParser.search("milk pork").get(0)).getDescription());
        pipeline.annotate(annotation);
        Taxonomy appliances = Taxonomy.getTaxonomy(TaxonomyType.APPLIANCES);
        Taxonomy utensils = Taxonomy.getTaxonomy(TaxonomyType.UTENSILS);
        Taxonomy ingredients = Taxonomy.getTaxonomy(TaxonomyType.INGREDIENTS);
        EntityRecognizer recognizer = new EntityRecognizer();
        recognizer.open();
        // An Annotation is a Map and you can get and use the
        // various analyses individually. For instance, this
        // gets the parse tree of the 1st sentence in the text.
        List<CoreMap> sentences = annotation.get(
                CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                // this is the text of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                if(pos.startsWith("N")) {
                    System.out.println("NER: " + word + ", tags: " + recognizer.getType(word));
                }
            }
        }
        recognizer.close();
    }

    public static void main(String[] args) throws HTMLParseException {
        EntityRecognizer.initializeDictionaries();
        dictionaries.get(TaxonomyType.UTENSILS).forEach(System.out::println);

        experimental();
    }
}
