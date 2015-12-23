package uk.ac.cam.sp715.recognition;

import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;
import org.jgrapht.graph.DefaultDirectedGraph;
import uk.ac.cam.sp715.recipes.Ingredient;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.util.HTMLParseException;
import uk.ac.cam.sp715.util.HTMLParser;
import uk.ac.cam.sp715.util.Pipeline;
import uk.ac.cam.sp715.wordnet.Explorer;
import uk.ac.cam.sp715.wordnet.Taxonomy;
import uk.ac.cam.sp715.wordnet.Taxonomy.TaxonomyType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dictionary based entity recognizer which uses WordNet taxonomies
 * to produce lemmatized dictionaries for the each {@link TaxonomyType}.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class EntityAnnotator implements Annotator {
    private static Explorer explorer;
    private static Map<TaxonomyType, Set<String>> dictionaries;
    public static final String NAME = "entities";
    public static final class EntityAnnotations implements CoreAnnotation<AugmentedSemanticGraph> {
        @Override
        public Class<AugmentedSemanticGraph> getType() {
            return AugmentedSemanticGraph.class;
        }
    }

    public EntityAnnotator(String string, Properties props) {
        if(explorer == null) {
            explorer = new Explorer();
            explorer.open();
        }
        if(dictionaries == null) initializeDictionaries();
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

    public static class AugmentedSemanticGraph extends DefaultDirectedGraph<TaggedWord, GrammaticalRelation> {
        public AugmentedSemanticGraph() {
            super(GrammaticalRelation.class);
        }
        public List<TaggedWord> orderedTokens() {
            return this.vertexSet().parallelStream().sorted((o1, o2) -> {
                if(o1.beginPosition() < o2.beginPosition()) return -1;
                else if(o1.beginPosition() > o2.beginPosition()) return 1;
                else {
                    if(o1.endPosition() < o2.endPosition()) return -1;
                    else if(o1.beginPosition() > o2.endPosition()) return 1;
                    else return 0;
                }
            }).collect(Collectors.toList());
        }
    }

    @Override
    public void annotate(Annotation annotation) {
        for(CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
            List<IndexedWord> originalSentence = dependencies.vertexListSorted();
            Stack<TaggedWord> modifiedTokens = new Stack<>();
            AugmentedSemanticGraph newDependencies = new AugmentedSemanticGraph();
            Map<IndexedWord, TaggedWord> updateMap = new HashMap<>();
            for (IndexedWord token : originalSentence) {
                String word = token.word();
                String pos = token.tag();
                TaxonomyType tag = pos.startsWith("N") ? getType(word) : TaxonomyType.OTHER;
                TaggedWord taggedToken = new TaggedWord(token, tag);

                if(!modifiedTokens.empty()) {
                    TaggedWord top = modifiedTokens.peek();
                    if(top.isTypedEntity() || taggedToken.isTypedEntity()) {
                        if(top.isNoun() && taggedToken.isNoun()) {
                            if(top.isTypedEntity()) top.addToken(token);
                            else {
                                modifiedTokens.pop();
                                taggedToken.addAll(top.getTokens());
                                modifiedTokens.push(taggedToken);
                            }
                        } else modifiedTokens.push(taggedToken);
                    } else modifiedTokens.push(taggedToken);
                } else modifiedTokens.push(taggedToken);

                modifiedTokens.peek().getTokens().forEach((key) -> updateMap.put(key, modifiedTokens.peek()));
            }

            while(!modifiedTokens.empty()) newDependencies.addVertex(modifiedTokens.pop());
            for(SemanticGraphEdge relation : dependencies.edgeIterable()) {
                newDependencies.addEdge(updateMap.get(relation.getSource()), updateMap.get(relation.getTarget()), relation.getRelation());
            }

            sentence.set(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class, null);
            sentence.set(EntityAnnotations.class, newDependencies);
        }
    }

    @Override
    public Set<Requirement> requirementsSatisfied() {
        return Collections.singleton(new Requirement(NAME));
    }

    @Override
    public Set<Requirement> requires() {
        return Collections.singleton(Annotator.DEPENDENCY_REQUIREMENT);
    }

    private static class IngredientFinder {
        private static final StanfordCoreNLP pipeline = Pipeline.getLemmaPipeline();
        /**
         * Constructs a simple finder to recognize candidate nouns using the
         * given pipeline. This should include tokenization, lemmatization and POS tagging.
         */
        public IngredientFinder() {}
        /**
         * Extracts all noun tokens from the ingredient description
         * @param ingredient Ingredient details, from which the candidate strings are extracted.
         * @return {@link List}<{@link String}> - The candidate strings which have been tagged as nouns.
         */
        public List<String> possibleIngredientNames(Ingredient ingredient) {
            String ingredientString = ingredient.getName();
            Annotation annotation = new Annotation(ingredientString);
            pipeline.annotate(annotation);
            List<CoreMap> sentences = annotation.get(
                    CoreAnnotations.SentencesAnnotation.class);
            List<String> possibleIngredientNames = new LinkedList<>();

            for(CoreMap sentence : sentences) {
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    String word = token.word();
                    String pos = token.tag();
                    //Add only nouns.
                    if(pos.startsWith("N") && !word.equals("oz") && word.matches("[a-zA-Z]+")) {
                        possibleIngredientNames.add(word);
                    }
                }
            }
            return possibleIngredientNames;
        }
    }

    public static void augmentIngredientDictionary(Recipe recipe) {
        IngredientFinder finder = new IngredientFinder();
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
     * @return {@link TaxonomyType} - the type of this entity.
     */
    public TaxonomyType getType(String noun) {
        IIndexWord indexWord = explorer.getIndexNoun(noun);
        if(indexWord != null) {
            for (IWordID wordID : indexWord.getWordIDs()) {
                String lemma = explorer.getWord(wordID).getLemma();
                for (TaxonomyType type : dictionaries.keySet()) if (dictionaries.get(type).contains(lemma)) return type;
            }
        } else for (TaxonomyType type : dictionaries.keySet()) if (dictionaries.get(type).contains(noun)) return type;

        if(noun.endsWith("s")) {
            return getType(noun.substring(0, noun.length() - 1));
        } else return TaxonomyType.OTHER;
    }

    public static void main(String[] args) throws HTMLParseException {
        //dictionaries.get(TaxonomyType.UTENSILS).forEach(System.out::println);
        StanfordCoreNLP pipeline = Pipeline.getLemmaPipeline();
        StanfordCoreNLP mainPipeline = Pipeline.getMainPipeline();
        Recipe recipe = HTMLParser.getRecipe(HTMLParser.search("chocolate").get(1).getLink());
        EntityAnnotator.augmentIngredientDictionary(recipe);
        Annotation annotation = new Annotation(recipe.getDescription());
        mainPipeline.annotate(annotation);
        for(CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            System.out.println();
            AugmentedSemanticGraph dependencies = sentence.get(EntityAnnotations.class);
            /*dependencies.edgeSet().forEach((relation) -> {
                    System.out.println("Rel: " + relation.getShortName() + ", Gov: " + dependencies.getEdgeSource(relation) + ", Dep: " + dependencies.getEdgeTarget(relation));
            });*/
            dependencies.orderedTokens().forEach(System.out::println);
        }



        //experimental();
    }
}
