package uk.ac.cam.sp715.distsim;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.recognition.EntityAnnotator.AugmentedSemanticGraph;
import uk.ac.cam.sp715.recognition.TaggedWord;
import uk.ac.cam.sp715.util.*;
import uk.ac.cam.sp715.wordnet.Taxonomy;

import java.nio.file.Paths;
import java.util.*;

import static uk.ac.cam.sp715.recognition.EntityAnnotator.*;

public class Word2Vec {
    private static final int maxDistance = 5;
    private static List<String> getRightNeighbours(String word, List<TaggedWord> tokens, int position) {
        List<String> rightNeighbours = new ArrayList<>();
        for(int k = position + 1; k<tokens.size() && rightNeighbours.size()<maxDistance; k++) {
            if(isValidWord(word, tokens.get(k))) rightNeighbours.add(tokens.get(k).toString().toLowerCase());
        }
        return rightNeighbours;
    }
    private static List<String> getLeftNeighbours(String word, List<TaggedWord> tokens, int position) {
        List<String> leftNeighbours = new ArrayList<>();
        for(int k = position - 1; k>0 && leftNeighbours.size()<maxDistance; k--) {
            if(isValidWord(word, tokens.get(k))) leftNeighbours.add(tokens.get(k).toString().toLowerCase());
        }
        return leftNeighbours;
    }
    //Filters neighbour words for those of a specific part of speech.
    public static boolean isValidWord(String initialWord, TaggedWord token) {
        String word = token.toString().toLowerCase();
        String pos = token.tag();
        return (pos.startsWith("JJ") || pos.startsWith("NN")
                || pos.startsWith("RB") || pos.startsWith("VB")) && !word.equalsIgnoreCase(initialWord);
    }
    public static void evaluate() throws IOToolsException, HTMLParseException {
        Set<Recipe> recipes = IOTools.read(Paths.get("data", "wordvecrecipes.ser").toString());

        Pipeline pipeline = Pipeline.getMainPipeline();
        List<AugmentedSemanticGraph> sentences = new LinkedList<>();
        for(Recipe recipe : recipes) {
            Annotation annotation = pipeline.annotate(recipe);
            for(CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                sentences.add(sentence.get(EntityAnnotations.class));
            }
        }

        Map<String, Vector> vectors = new HashMap<>();
        Map<Taxonomy.TaxonomyType, Set<String>> sets = new HashMap<>();
        for(Taxonomy.TaxonomyType type : Taxonomy.TaxonomyType.values()) sets.put(type, new HashSet<>());

        Dictionary dictionary = new Dictionary(sentences);
        Map<String, Integer> counts = new HashMap<>();
        double count = 0;
        for(AugmentedSemanticGraph sentence : sentences) {

            List<TaggedWord> tokens = sentence.orderedTokens();
            int i = 0;
            for (TaggedWord token : tokens) {
                String tword = token.toString().toLowerCase();

                if(counts.containsKey(tword)) counts.put(tword, counts.get(tword) + 1);
                else counts.put(tword, 1);
                count++;

                if(token.isTypedEntity()) {
                    String word = token.toString().toLowerCase();
                    List<String> leftNeighbours = getLeftNeighbours(word, tokens, i);
                    List<String> rightNeighbours = getRightNeighbours(word, tokens, i);
                    Vector v = new Vector();
                    v = v.addAt(dictionary.index(word), 1);
                    for(int k = 0; k<leftNeighbours.size(); k++) {
                        String neighbour = leftNeighbours.get(k);
                        v = v.addAt(dictionary.index(neighbour), 1);
                    }
                    for(int k = 0; k<rightNeighbours.size(); k++) {
                        String neighbour = rightNeighbours.get(k);
                        v = v.addAt(dictionary.index(neighbour), 1);
                    }
                    if(!vectors.containsKey(word)) vectors.put(word, new Vector());
                    vectors.put(word, vectors.get(word).add(v));
                    sets.get(token.entity()).add(word);
                }
                i++;
            }
        }

        for(String key : vectors.keySet()) {
            vectors.put(key, vectors.get(key).pmi(dictionary, counts, count, key));
        }

        Map<Taxonomy.TaxonomyType, Vector> averages = new HashMap<>();

        for(Taxonomy.TaxonomyType type : new Taxonomy.TaxonomyType[] {Taxonomy.TaxonomyType.APPLIANCES, Taxonomy.TaxonomyType.UTENSILS, Taxonomy.TaxonomyType.INGREDIENTS}) {
            double n = 0;
            Vector v = new Vector();
            for(String key : sets.get(type)) {
                v = v.add(vectors.get(key));
                n++;
            }
            averages.put(type, v.divide(n));
        }

        /*
        for(Taxonomy.TaxonomyType type1 : new Taxonomy.TaxonomyType[] {Taxonomy.TaxonomyType.APPLIANCES, Taxonomy.TaxonomyType.UTENSILS, Taxonomy.TaxonomyType.INGREDIENTS}) {
            for(Taxonomy.TaxonomyType type2 : new Taxonomy.TaxonomyType[] {Taxonomy.TaxonomyType.APPLIANCES, Taxonomy.TaxonomyType.UTENSILS, Taxonomy.TaxonomyType.INGREDIENTS}) {
                Vector v1 = averages.get(type1);
                Vector v2 = averages.get(type2);
                double nom = v1.dot(v2);
                double denom = v1.length()*v2.length();
                //nom/denom is cosine similarity as cos(t) = v1*v2/(length(v1)*length(v2)).
                System.out.println(type1 + " ~ " + type2 + ": " + nom/denom);
            }
        }

        for(Taxonomy.TaxonomyType type1 : new Taxonomy.TaxonomyType[] {Taxonomy.TaxonomyType.APPLIANCES, Taxonomy.TaxonomyType.UTENSILS, Taxonomy.TaxonomyType.INGREDIENTS}) {
            System.out.println();
            System.out.println();
            for(Taxonomy.TaxonomyType type2 : new Taxonomy.TaxonomyType[] {Taxonomy.TaxonomyType.APPLIANCES, Taxonomy.TaxonomyType.UTENSILS, Taxonomy.TaxonomyType.INGREDIENTS}) {
                System.out.println();
                System.out.println(type1 + " ~ " + type2);
                System.out.println();
                double typeSum = 0;
                long j = 0;
                for(String key1 : sets.get(type1)) {
                    double sum = 0;
                    long i = 0;
                    for(String key2: sets.get(type2)) {
                        Vector v1 = vectors.get(key1);
                        Vector v2 = vectors.get(key2);
                        double num = v1.dot(v2);
                        double denom = v1.length()*v2.length();
                        //num/denom is cosine similarity as cos(t) = v1*v2/(length(v1)*length(v2)).
                        sum += num/denom;
                        i++;
                    }

                    typeSum += sum;
                    j += i;
                }
                System.out.println(typeSum/j);
            }
        }*/

        for(Taxonomy.TaxonomyType type1 : new Taxonomy.TaxonomyType[] {Taxonomy.TaxonomyType.APPLIANCES, Taxonomy.TaxonomyType.UTENSILS, Taxonomy.TaxonomyType.INGREDIENTS}) {
            System.out.println();
            System.out.println();
            for(Taxonomy.TaxonomyType type2 : new Taxonomy.TaxonomyType[] {Taxonomy.TaxonomyType.APPLIANCES, Taxonomy.TaxonomyType.UTENSILS, Taxonomy.TaxonomyType.INGREDIENTS}) {
                System.out.println();
                System.out.println(type1 + " ~ " + type2);
                System.out.println();
                double typeSum = 0;
                long j = 0;
                for(String key1 : sets.get(type1)) {
                    double sum = 0;
                    long i = 0;
                    Vector v1 = vectors.get(key1);
                    Vector v2 = averages.get(type2);
                    double num = v1.dot(v2);
                    double denom = v1.length()*v2.length();
                    sum = num/denom;
                    i++;

                    typeSum += sum;
                    j += i;
                }
                System.out.println(typeSum/j);
            }
        }
    }
    public static void main(String[] args) throws IOToolsException, HTMLParseException {
        evaluate();
    }
}
