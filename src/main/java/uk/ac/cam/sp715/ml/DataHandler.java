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
public class DataHandler {
    private static final String path = "data";
    private static Path getPath(String name) {
        return Paths.get(path, name);
    }

    private static void addDependencies(SortedMap<Integer, Set<Integer>> dependencies, int dst, int src) {
        Set<Integer> newDeps = dependencies.get(src);
        dependencies.get(dst).addAll(newDeps);
    }

    private static void serializeFlows() throws IOToolsException {
        Visualiser visualiser = new CoreNLPVisualiser(Pipeline.getMainPipeline());
        List<Recipe> recipes = IOTools.read(getPath("recipes.ser").toString());
        LinkedList<Flow> flows = new LinkedList<>();

        for(Recipe recipe : recipes) {
            flows.add(visualiser.parse(recipe));
        }

        IOTools.save(flows, getPath("heuristic-flows.ser").toString());
    }

    public static Classifier<Boolean, String> getDependencyClassifier() throws IOException, IOToolsException {
        GeneralDataset<Boolean, String> dataset = new Dataset<>();
        List<String> lines = Files.lines(getPath("action-dep2.txt")).collect(Collectors.toList());
        List<Annotation> annotations = IOTools.read(getPath("annotations2.ser").toString());
        List<Recipe> recipes = IOTools.read(getPath("recipes.ser").toString());
        List<Flow> flows = IOTools.read(getPath("heuristic-flows.ser").toString());

        int count = 0;
        int recipeIndex = 0;
        int lineIndex = 0;
        for(;recipeIndex < 8;) {
            SortedMap<Integer, Set<Integer>> dependencies = new TreeMap<>();
            SortedMap<Integer, Action> actionSortedMap = new TreeMap<>();
            int bias = count;
            Flow flow = flows.get(recipeIndex);
            /*List<String> lineList = recipeIndex == 0 ? lines.subList(0, 28) :
                    recipeIndex == 1 ? lines.subList(29, 67) :
                            recipeIndex == 2 ? lines.subList(68, 71) :
                                    recipeIndex == 3 ? lines.subList(72, 79) :
                                            recipeIndex == 4 ? lines.subList(80, 88) : lines.subList(0, 0);*/
            recipeIndex++;

            for(;;) {
                String line = lines.get(lineIndex);
                lineIndex++;
                if(line.equals("------------------")) break;
                String[] posts = line.split("<");
                for(String post : posts) {
                    if(post.contains(">")) {
                        String data = post.split(">")[0];
                        String[] parts = data.split(":");
                        String text = parts[0];
                        String sid = parts[1];
                        int id = Integer.parseInt(sid) - bias;
                        String[] sdeps = {};
                        if(parts.length > 2) {
                            sdeps = parts[2].split(",");
                        }
                        dependencies.put(id, Arrays
                                .asList(sdeps)
                                .stream()
                                .map(Integer::parseInt)
                                .map(x -> x - bias)
                                .collect(Collectors.toSet()));
                        count++;
                    }
                }

            }

            /*
            for(int key : dependencies.keySet()) {
                for(int src : dependencies.get(key).stream().collect(Collectors.toSet())) addDependencies(dependencies, key, src);
            }*/

            int k = 0;
            List<Action> actions = new ArrayList<>(flow.vertexSet());
            Collections.sort(actions, (o1, o2) -> Integer.compare(o1.getID(), o2.getID()));
            for(Action action : actions) {
                actionSortedMap.put(k, action);
                k++;
            }

            for(int z = 0; z < 1; z++) {
                for (int srcKey : dependencies.subMap(dependencies.firstKey(), dependencies.lastKey()).keySet()) {
                    for (int dstKey : dependencies.subMap(srcKey + 1, dependencies.lastKey() + 1).keySet()) {
                        Action src = actionSortedMap.get(srcKey);
                        Action dst = actionSortedMap.get(dstKey);
                        dataset.add(FeatureVectors.getDatum(src, dst, flow, dependencies.get(dstKey).contains(srcKey)));
                    }
                }
            }
        }

        dataset.summaryStatistics();
        LogisticClassifierFactory<Boolean, String> factory = new LogisticClassifierFactory<>();
        return factory.trainClassifier(dataset);
    }

    private static void runDepTrainingPrep() throws IOToolsException {
        List<Annotation> annotations = IOTools.read(getPath("annotations2.ser").toString());
        Classifier<Role, String> classifier = SRLDataHandler.getClassifier(3);
        int index = 0;

        List<String> lines = new LinkedList<>();
        for (Annotation annotation : annotations) {
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                AugmentedSemanticGraph dependencies = sentence.get(EntityAnnotations.class);
                List<TaggedWord> tokens = dependencies.orderedTokens();
                int position = 0;
                List<String> line = new LinkedList<>();
                for (TaggedWord token : tokens) {
                    List<String> features = FeatureVectors.getFeatures(token, position, dependencies, tokens);
                    if(classifier.classOf(new BasicDatum<>(features)) == Role.ACTION) {
                        line.add("<" + token + ":" + index + ":>");
                        index++;
                    } else line.add(token.toString());
                    position++;
                }
                lines.add(line
                        .stream()
                        .collect(Collectors.joining(" ")));
            }
            lines.add("------------------");
        }
        IOTools.save(lines, getPath("action-dep3.txt"));
    }

    public static void main(String[] args) throws HTMLParseException, IOToolsException, IOException {
        /*CoreNLPVisualiser visualiser = new CoreNLPVisualiser(Pipeline.getMainPipeline());
        Classifier<Boolean, String> depClassifier = getDependencyClassifier(visualiser);
        HybridVisualiser hybridVisualiser = new HybridVisualiser(visualiser);
        List<Recipe> recipes = IOTools.read(getPath("recipes.ser").toString());

        for (int i = 0; i<3; i++) {
            Flow flow = hybridVisualiser.parse(recipes.get(i));
            System.out.println(flow.toDOT());
        }*/
        //merge();
        /*
        Annotation annotation = Pipeline.getLemmaPipeline().annotate(new Recipe("", "", new LinkedList<>(), Arrays.asList("Stir the lavender and Lady Grey tea leaves into the flour mixture.")));
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sentence : sentences) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for(int i = 0; i < tokens.size(); i++) {
                CoreLabel previous = i > 0 ? tokens.get(i - 1) : null;
                CoreLabel token = tokens.get(i);
                CoreLabel next = i < tokens.size() - 1 ? tokens.get(i + 1) : null;
                if(previous == null || previous.toString().equals(",")) {
                    if(token.toString().length() > 0
                            && Character.isUpperCase(token.toString().charAt(0))
                            && (next == null || !Character.isUpperCase(next.toString().charAt(0)))
                            && (previous == null || !Character.isUpperCase(previous.toString().charAt(0)))) toLowerCase(token);
                }
            }
            System.out.println(sentence);
        }*/
        serializeFlows();
    }
}
