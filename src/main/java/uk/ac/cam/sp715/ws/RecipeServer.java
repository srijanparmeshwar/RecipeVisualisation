package uk.ac.cam.sp715.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import spark.Filter;
import spark.Request;
import spark.Response;
import uk.ac.cam.sp715.flows.CoreNLPVisualiser;
import uk.ac.cam.sp715.flows.Flow;
import uk.ac.cam.sp715.recipes.Ingredient;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.recognition.EntityAnnotator;
import uk.ac.cam.sp715.util.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static spark.Spark.*;

/**
 * Provides a web service interface to access BBC Recipes and
 * in the future will provide the visualisation service.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class RecipeServer {
    private static final StanfordCoreNLP pipeline = Pipeline.getMainPipeline();
    private static final CoreNLPVisualiser visualiser = new CoreNLPVisualiser(pipeline);
    private static final Logger logger = Logging.getLogger(RecipeServer.class);

    public static void main(String[] args) {
        port(4567);
        before((request, response) -> System.gc());
        get("/search", RecipeServer::search);
        get("/recipes/:id", RecipeServer::getRecipe);
    }

    private static String parse(Recipe recipe) throws IOException {
        Flow flow;
        synchronized (pipeline) {
            EntityAnnotator.augmentIngredientDictionary(recipe);
            flow = visualiser.parse(recipe);
        }
        return flow.toSVG();
    }

    private static String search(Request request, Response response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Link> links = HTMLParser.search(request.queryParams("q"));
            List<String> jsonRecipes = new LinkedList<>();
            links.forEach((recipe) -> jsonRecipes.add(recipe.toJSON()));
            return mapper.writeValueAsString(jsonRecipes);
        } catch (HTMLParseException | JsonProcessingException e) {
            logger.log(Level.SEVERE, "Error occurred searching for recipes.", e);
            response.status(400);
        }
        return "[]";
    }

    private static class RecipeResult {
        private final String title;
        private final String summary;
        private final List<String> ingredients;
        private final List<String> instructions;
        private final String svg;
        public RecipeResult(Recipe recipe, String svg) {
            this.title = recipe.getTitle();
            this.summary = recipe.getSummary();
            this.ingredients = recipe.getIngredients()
                    .stream()
                    .map(Ingredient::getName)
                    .collect(Collectors.toList());
            this.instructions = recipe.getInstructions();
            this.svg = svg;
        }
        private static String JSStringArray(List<String> list) {
            return "[" + list
                    .stream()
                    .map(ingredient -> "\"" + ingredient + "\"")
                    .collect(Collectors.joining(", ")) + "]";
        }
        public String toJSON() {
            StringJoiner joiner = new StringJoiner(", ");
            joiner.add("\"title\": \"" + title + "\"");
            joiner.add("\"summary\": \"" + summary + "\"");
            joiner.add("\"ingredients\": " + JSStringArray(ingredients));
            joiner.add("\"instructions\": " + JSStringArray(instructions));

            return "{" + joiner + "}" + ";;;" + svg;
        }
    }

    private static String getRecipe(Request request, Response response) {
        try {
            Recipe recipe = HTMLParser.getRecipe(request.params(":id"));
            RecipeResult result = new RecipeResult(recipe, parse(recipe));
            return result.toJSON();
        } catch (HTMLParseException e) {
            logger.log(Level.SEVERE, "Error occurred searching for recipes.", e);
            response.status(400);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error occurred converting graph to SVG format.", e);
            response.status(400);
        }
        return "";
    }
}
