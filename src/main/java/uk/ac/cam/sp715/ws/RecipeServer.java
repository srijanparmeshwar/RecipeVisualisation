package uk.ac.cam.sp715.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;
import spark.Response;
import uk.ac.cam.sp715.flows.CoreNLPVisualiser;
import uk.ac.cam.sp715.flows.Flow;
import uk.ac.cam.sp715.flows.HybridVisualiser;
import uk.ac.cam.sp715.recipes.Ingredient;
import uk.ac.cam.sp715.recipes.Recipe;
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
    private static final Pipeline pipeline = Pipeline.getMainPipeline();
    private static final CoreNLPVisualiser visualiser = new CoreNLPVisualiser(pipeline);
    private static final HybridVisualiser hybridVisualiser = new HybridVisualiser(visualiser);
    private static final Logger logger = Logging.getLogger(RecipeServer.class);

    public static void main(String[] args) {
        port(4567);
        before((request, response) -> System.gc());
        get("/search", RecipeServer::search);
        get("/recipes/:id", RecipeServer::getRecipe);
        post("/upload", RecipeServer::processUpload);
    }

    private static String parse(Recipe recipe) throws IOException {
        Flow flow;
        synchronized (pipeline) {
            flow = hybridVisualiser.parse(recipe);
        }
        return flow.toDOT();
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
        private final String data;

        public RecipeResult(@JsonProperty("title") String title, @JsonProperty("summary") String summary,
                            @JsonProperty("ingredients") List<String> ingredients, @JsonProperty("instructions") List<String> instructions,
                            @JsonProperty("data") String data) {
            this.title = title;
            this.summary = summary;
            this.ingredients = ingredients;
            this.instructions = instructions;
            this.data = data;
        }

        public RecipeResult(Recipe recipe, String data) {
            this.title = recipe.getTitle();
            this.summary = recipe.getSummary();
            this.ingredients = recipe.getIngredients()
                    .stream()
                    .map(Ingredient::getName)
                    .collect(Collectors.toList());
            this.instructions = recipe.getInstructions();
            this.data = data;
        }
        private static String JSStringArray(List<String> list) {
            return "[" + list
                    .stream()
                    .map(element -> "\"" + element + "\"")
                    .collect(Collectors.joining(", ")) + "]";
        }
        public String toJSON() {
            StringJoiner joiner = new StringJoiner(", ");
            joiner.add("\"title\": \"" + title + "\"");
            joiner.add("\"summary\": \"" + summary + "\"");
            joiner.add("\"ingredients\": " + JSStringArray(ingredients));
            joiner.add("\"instructions\": " + JSStringArray(instructions));

            return "{" + joiner + "}" + ";;;" + data;
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
            logger.log(Level.SEVERE, "Error occurred converting graph to DOT format.", e);
            response.status(400);
        }
        return "";
    }

    private static String processUpload(Request request, Response response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            RecipeResult recipeUpload = objectMapper.readValue(request.body(), RecipeResult.class);
            Recipe recipe = new Recipe(recipeUpload.title, recipeUpload.summary,
                    recipeUpload.ingredients
                            .stream()
                            .map(Ingredient::new)
                            .collect(Collectors.toList()), recipeUpload.instructions);
            RecipeResult result = new RecipeResult(recipe, parse(recipe));
            return result.toJSON();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error occurred either reading JSON or writing to DOT.", e);
            response.status(400);
        }
        return "";
    }
}
