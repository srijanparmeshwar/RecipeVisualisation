package uk.ac.cam.sp715.ml;

import edu.stanford.nlp.classify.*;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import uk.ac.cam.sp715.flows.*;
import uk.ac.cam.sp715.recipes.Ingredient;
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
public class SRLDataHandler {
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
                        switch (label) {
                            case "A":
                                labels.put(index, Role.ACTION);
                                break;
                            case "D":
                                labels.put(index, Role.DOBJECT);
                                break;
                            case "I":
                                labels.put(index, Role.IOBJECT);
                                break;
                            case "O":
                                labels.put(index, Role.OTHER);
                                break;
                        }
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
            Files.write(getPath("srl-train2.txt"), lines);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static Classifier<Role, String> getClassifier() throws IOToolsException {
        return getClassifier(4);
    }

    public static Classifier<Role, String> getClassifier(int version) throws IOToolsException {
        return ClassifierTrainer.train(loadTrainingData(version));
    }

    public static GeneralDataset<Role, String> loadTrainingData(int version) throws IOToolsException {
        return IOTools.read(getPath("srl-train-en" + version + ".ser").toString());
    }

    private static void serializeAnnotations() throws IOToolsException {
        Pipeline pipeline = Pipeline.getMainPipeline();
        List<Recipe> recipes = IOTools.read(getPath("recipes.ser").toString());
        LinkedList<Annotation> annotations = new LinkedList<>();

        for (Recipe recipe : recipes) {
            annotations.add(pipeline.annotate(recipe));
        }

        IOTools.save(annotations, getPath("annotations2.ser").toString());
    }

    public static Map<Integer, List<String>> getFeatureMap() throws IOToolsException {
        Map<Integer, List<String>> featureMap = new HashMap<>();
        List<Annotation> annotations = IOTools.read(getPath("annotations2.ser").toString());
        int index = 0;

        for (Annotation annotation : annotations) {
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
        return featureMap;
    }

    private static void serializeTrainingData() throws IOToolsException {
        //LinkedList<Recipe> recipes = IOTools.read(getPath("recipes.ser").toString());
        GeneralDataset<Role, String> dataset = constructDataset(loadLabels("srl-train3.txt"), getFeatureMap());
        dataset.printSparseFeatureMatrix();
        IOTools.save(dataset, getPath("srl-train-en4.ser").toString());
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

    public static void migrate(String sourceFile, String newFormatFile, String destFile) {
        Map<Integer, String> currentLabels = new TreeMap<>();
        Map<Integer, String> currentTokens = new TreeMap<>();
        Map<Integer, List<String>> newLabels = new TreeMap<>();
        Map<Integer, String> newTokens = new TreeMap<>();
        Set<Integer> sentenceIndices = new HashSet<>();
        try {
            index = 0;
            Files.lines(getPath(sourceFile)).forEach(line -> {
                String[] tokens = line.split(";;");
                for(String token : tokens) {
                    String[] pair = token.split("::");
                    currentTokens.put(index, pair[0]);
                    if(pair.length == 2) {
                        String label = pair[1];
                        currentLabels.put(index, label);
                    }
                    index++;
                }
            });

            index = 0;
            Files.lines(getPath(newFormatFile)).forEach(line -> {
                sentenceIndices.add(index);
                String[] tokens = line.split(";;");
                for(String token : tokens) {
                    String[] pair = token.split("::");
                    newTokens.put(index, pair[0]);
                    index++;
                }
            });

            for(int ci = 0, ni = 0; ci<currentTokens.size() && ni<newTokens.size();) {
                String cstring = currentTokens.get(ci);
                String cl = currentLabels.get(ci);
                String nstring = newTokens.get(ni);
                List<String> nl = new LinkedList<>();
                newLabels.put(ni, nl);

                int ca = cstring.split(" ").length;
                int na = nstring.split(" ").length;

                if(ca < na) {
                    if(cl != null) nl.add(cl);
                    for(int k = 0; k < na - ca; k++) if(currentLabels.containsKey(ci + k + 1)) nl.add(currentLabels.get(ci + k + 1));
                    ci += 1 + na - ca;
                    ni++;
                } else if(na > ca) {
                    if(cl != null) nl.add(cl);
                    for(int k = 0; k < ca - na; k++) if(newTokens.containsKey(ni + k + 1)) newLabels.put(ni + k + 1, nl);
                    ci++;
                    ni += 1 + ca - na;
                } else {
                    if(cl != null) nl.add(cl);
                    ci++;
                    ni++;
                }
            }

            List<String> lines = new LinkedList<>();
            StringBuilder builder = new StringBuilder();

            for(int key : newTokens.keySet()) {
                builder.append(newTokens.get(key));
                builder.append("::");
                builder.append(newLabels.get(key).stream().collect(Collectors.joining(",")));
                if(newTokens.containsKey(key + 1)) {
                    if(sentenceIndices.contains(key + 1)) {
                        lines.add(builder.toString());
                        builder = new StringBuilder();
                    } else {
                        builder.append(";;");
                    }
                } else lines.add(builder.toString());
            }

            IOTools.save(lines, getPath(destFile));
        } catch (IOException | IOToolsException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws HTMLParseException, IOToolsException, IOException {
        serializeTrainingData();
    }
}
