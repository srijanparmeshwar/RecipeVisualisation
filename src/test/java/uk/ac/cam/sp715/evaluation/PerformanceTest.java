package uk.ac.cam.sp715.evaluation;

import edu.stanford.nlp.pipeline.Annotation;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.cam.sp715.flows.CoreNLPVisualiser;
import uk.ac.cam.sp715.flows.HybridVisualiser;
import uk.ac.cam.sp715.flows.Visualiser;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.util.Pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.nio.file.Paths.get;
import static uk.ac.cam.sp715.util.IOTools.read;

/**
 * Created by Srijan on 08/04/2016.
 */
public class PerformanceTest {

    private static List<Recipe> recipes;
    private static final Pipeline pipeline = Pipeline.getPipeline("tokenize, ssplit, pos, lemma, ner, depparse");
    private static final Visualiser visualiser = new HybridVisualiser(new CoreNLPVisualiser(Pipeline.getMainPipeline()));

    @BeforeClass
    public static void setUp() throws Exception {
        recipes = read(get("data", "recipes.ser").toString());
        System.out.println(recipes.size());
    }

    @Test
    public void CoreNLPTest() {
        long start = 0;
        long time = 0;
        for(Recipe recipe : recipes) {
            Annotation annotation = new Annotation(recipe.getDescription());
            pipeline.annotate(annotation);
        }
        for(int i = 0; i < 8; i++) {
            for(Recipe recipe : recipes) {
                Annotation annotation = new Annotation(recipe.getDescription());
                start = System.nanoTime();
                pipeline.annotate(annotation);
                time += System.nanoTime() - start;
            }
        }
        System.out.println(time / 1E9);
        System.out.println((time / (recipes.size() * 8)) / 1E9);
    }

    @Test
    public void visualiserTest() throws IOException {
        long start = 0;
        long time = 0;
        Map<Integer, List<Long>> times = new HashMap<>();
        for(Recipe recipe : recipes) {
            visualiser.parse(recipe);
        }
        for(int i = 0; i < 8; i++) {
            for(Recipe recipe : recipes) {
                start = System.nanoTime();
                visualiser.parse(recipe);
                time += System.nanoTime() - start;
                if(!times.containsKey(recipe.getDescription().length())) times.put(recipe.getDescription().length(), new LinkedList<>());
                times.get(recipe.getDescription().length()).add(time);
            }
        }
        System.out.println(time / 1E9);
        System.out.println((time / (recipes.size() * 8)) / 1E9);
        List<String> timeList = new LinkedList<>();
        for(int key : times.keySet()) {
            double mean = times.get(key).stream().mapToLong(x -> x).average().getAsDouble();
            timeList.add(key + "," + mean);
        }
        Files.write(Paths.get("data", "times.csv"), timeList);
    }
}
