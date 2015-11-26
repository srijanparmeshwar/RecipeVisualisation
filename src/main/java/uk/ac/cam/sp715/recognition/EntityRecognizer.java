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
import uk.ac.cam.sp715.recipes.Ingredient;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.util.HTMLParseException;
import uk.ac.cam.sp715.util.HTMLParser;
import uk.ac.cam.sp715.wordnet.Explorer;
import uk.ac.cam.sp715.wordnet.Taxonomy;
import uk.ac.cam.sp715.wordnet.Taxonomy.TaxonomyType;

import java.util.*;

/**
 * Dictionary based entity recognizer which uses WordNet taxonomies
 * to produce lemmatised dictionaries for the each {@link TaxonomyType}.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class EntityRecognizer implements AutoCloseable {
    private final Explorer explorer;
    private static Map<TaxonomyType, Set<String>> dictionaries;

    /**
     * Constructs a new recognizer. Open should be called
     * before using the recognizer, and close once finished with
     * the recognizer.
     */
    public EntityRecognizer() {
        explorer = new Explorer();
        if(dictionaries == null) initializeDictionaries();
    }

    /**
     * This opens the underlying resources required for
     * processing and needs to be called before any methods are
     * used. Close should be called when the recognizer
     * is no longer needed.
     */
    public void open() {
        explorer.open();
    }

    @Override
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

    private static class IngredientFinder {
        private final StanfordCoreNLP pipeline;
        /**
         * Constructs a simple finder to recognize candidate nouns using the
         * given pipeline. This should include tokenization, lemmatization and POS tagging.
         * @param pipeline Pipeline which will be used to annotate ingredient descriptions and tag nouns.
         */
        public IngredientFinder(StanfordCoreNLP pipeline) {
            this.pipeline = pipeline;
        }
        /**
         * Extracts all noun tokens from the ingredient description
         * @param ingredient Ingredient details, from which the candidate strings are extracted.
         * @return {@link List}<{@link String}> - The candidate strings which have been tagged as nouns.
         */
        public List<String> possibleIngredientNames(Ingredient ingredient) {
            String ingredientString = ingredient.getName();
            ingredientString = ingredientString.replaceAll("/", " ");
            ingredientString = ingredientString.replaceAll("oz", "");
            Annotation annotation = new Annotation(ingredientString);
            this.pipeline.annotate(annotation);
            List<CoreMap> sentences = annotation.get(
                    CoreAnnotations.SentencesAnnotation.class);
            List<String> possibleIngredientNames = new LinkedList<>();

            for(CoreMap sentence : sentences) {
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    String word = token.get(CoreAnnotations.TextAnnotation.class);
                    String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    //Add nouns.
                    if(pos.startsWith("N")) {
                        possibleIngredientNames.add(word);
                    }
                }
            }
            return possibleIngredientNames;
        }
    }

    private void augmentIngredientDictionary(StanfordCoreNLP pipeline, Recipe recipe) {
        IngredientFinder finder = new IngredientFinder(pipeline);
        for(Ingredient ingredient : recipe.getIngredients()) {
            for(String noun : finder.possibleIngredientNames(ingredient)) {
                IIndexWord indexWord = explorer.getIndexNoun(noun);
                if(indexWord != null) {
                    for (IWordID wordID : indexWord.getWordIDs()) {
                        String lemma = explorer.getWord(wordID).getLemma();
                        dictionaries.get(TaxonomyType.INGREDIENTS).add(lemma);
                    }
                }
            }
        }
    }

    /**
     * Recognizing whether a given noun is a certain type of entity.
     * @param noun Noun to be recognized.
     * @return {link TaxonomyType} - the type of this entity.
     */
    public TaxonomyType getType(String noun) {
        IIndexWord indexWord = explorer.getIndexNoun(noun);
        if(indexWord != null) {
            for (IWordID wordID : indexWord.getWordIDs()) {
                String lemma = explorer.getWord(wordID).getLemma();
                for (TaxonomyType type : dictionaries.keySet()) if (dictionaries.get(type).contains(lemma)) return type;
            }
        } else for (TaxonomyType type : dictionaries.keySet()) if (dictionaries.get(type).contains(noun)) return type;
        return TaxonomyType.OTHER;
    }

    public static void experimental() throws HTMLParseException {
        Properties props = new Properties();
        props.setProperty("annotators",
                "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Recipe recipe = HTMLParser.getRecipe(HTMLParser.search("chocolate").get(0).getLink());
        Annotation annotation = new Annotation(
                recipe.getDescription());
        pipeline.annotate(annotation);
        Taxonomy appliances = Taxonomy.getTaxonomy(TaxonomyType.APPLIANCES);
        Taxonomy utensils = Taxonomy.getTaxonomy(TaxonomyType.UTENSILS);
        Taxonomy ingredients = Taxonomy.getTaxonomy(TaxonomyType.INGREDIENTS);
        EntityRecognizer recognizer = new EntityRecognizer();
        recognizer.open();
        recognizer.augmentIngredientDictionary(pipeline, recipe);
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
