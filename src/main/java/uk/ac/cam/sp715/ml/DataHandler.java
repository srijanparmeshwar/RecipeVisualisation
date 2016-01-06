package uk.ac.cam.sp715.ml;

import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.GeneralDataset;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import uk.ac.cam.sp715.flows.FeatureVectors;
import uk.ac.cam.sp715.flows.Role;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.recognition.TaggedWord;
import uk.ac.cam.sp715.util.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static uk.ac.cam.sp715.recognition.EntityAnnotator.*;

/**
 * Created by Srijan on 18/12/2015.
 */
public class DataHandler {
    private static final String path = "data";
    private static int index = 0;
    private static Path getPath(String name) {
        return Paths.get(path, name);
    }

    /**
     * Requires file of format:
     * token1::label;;token2::label;;...
     * e.g. Grease::A;;and::O;;line::A;;...
     * @param name - filename
     * @return Map of indices to labels from the training file.
     */
    public static Map<Integer, Role> loadLabels(String name) {
        index = 0;
        Map<Integer, Role> labels = new TreeMap<>();
        try {
            Files.lines(getPath(name)).forEach(line -> {
                String[] tokens = line.split(";;");
                for(String token : tokens) {
                    String[] pair = token.split("::");
                    if(pair.length == 2) {
                        String label = pair[1];
                        if(label.equals("A")) labels.put(index, Role.ACTION);
                        else if(label.equals("D")) labels.put(index, Role.DOBJECT);
                        else if(label.equals("I")) labels.put(index, Role.IOBJECT);
                        else if(label.equals("O")) labels.put(index, Role.OTHER);
                    }
                    index++;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return labels;
    }

    public static GeneralDataset<Role, String> constructDataset(Map<Integer, Role> labels, Map<Integer, List<String>> features) {
        GeneralDataset<Role, String> dataset = new Dataset<>();
        for(int key : labels.keySet()) {
            Datum<Role, String> datum = new BasicDatum<>(features.get(key), labels.get(key));
            dataset.add(datum);
        }
        dataset.summaryStatistics();
        return dataset;
    }

    public static void prepareTrainingFile(List<Recipe> recipes) {
        Pipeline pipeline = Pipeline.getMainPipeline();
        List<String> lines = recipes.stream().map(recipe -> {
            Annotation annotation = pipeline.annotate(recipe);
            return annotation.get(CoreAnnotations.SentencesAnnotation.class)
                    .stream()
                    .map(sentence -> sentence.get(EntityAnnotations.class)
                            .orderedTokens()
                            .stream()
                            .map(token -> token + "::")
                            .collect(Collectors.joining(";;"))
                    ).collect(Collectors.joining("\n"));
        }).collect(Collectors.toList());

        try {
            Files.write(getPath("srl-train.txt"), lines);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static Classifier<Role, String> getClassifier(Pipeline pipeline) throws IOToolsException {
        LinkedList<Recipe> recipes = IOTools.read(getPath("recipes.ser").toString());

        Map<Integer, List<String>> featureMap = new HashMap<>();
        index = 0;

        for (Recipe recipe : recipes) {
            Annotation annotation = pipeline.annotate(recipe);

            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                AugmentedSemanticGraph dependencies = sentence.get(EntityAnnotations.class);
                List<TaggedWord> tokens = dependencies.orderedTokens();
                int position = 0;
                for (TaggedWord token : tokens) {
                    List<String> features = FeatureVectors.getFeatures(token, position, dependencies, tokens);
                    featureMap.put(index, features);
                    position++;
                    index++;
                }
            }
        }

        return ClassifierTrainer.train(constructDataset(loadLabels("srl-train.txt"), featureMap));
    }

    private static void trainAndTest() throws IOToolsException {
        LinkedList<Recipe> recipes = IOTools.read(getPath("recipes.ser").toString());

        Pipeline pipeline = Pipeline.getMainPipeline();

        Classifier<Role, String> classifier = getClassifier(pipeline);

        for(Recipe recipe : recipes) {
            Annotation annotation = pipeline.annotate(recipe);

            for(CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                AugmentedSemanticGraph dependencies = sentence.get(EntityAnnotations.class);
                List<TaggedWord> tokens = dependencies.orderedTokens();
                int position = 0;
                for(TaggedWord token : tokens) {
                    List<String> features = FeatureVectors.getFeatures(token, position, dependencies, tokens);

                    System.out.print(token);
                    System.out.print("_" + classifier.classOf(new BasicDatum<>(features)) + " ");

                    position++;
                }
                System.out.println();
            }
        }
    }

    private static void runPreparation() throws HTMLParseException, IOToolsException {
        //List<Link> links = new LinkedList<>();
        //LinkedList<Recipe> recipes = new LinkedList<>();
        LinkedList<Recipe> recipes = IOTools.read(getPath("recipes.ser").toString());

        //for(String query : new String[] {"chocolate", "halloween", "pizza", "tea"}) links.addAll(HTMLParser.search(query));
        //for(Link link : links) recipes.add(HTMLParser.getRecipe(link.getLink()));

        //IOTools.save(links, getPath("links.txt"));
        //IOTools.save(recipes, getPath("recipes.ser").toString());

        prepareTrainingFile(recipes);
    }

    private static void testCurrentLabeling() throws HTMLParseException, IOToolsException {
        System.out.println(loadLabels("srl-train.txt"));
    }

    public static void main(String[] args) throws HTMLParseException, IOToolsException {
        testCurrentLabeling();
    }
}
