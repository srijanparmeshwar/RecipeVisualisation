package uk.ac.cam.sp715.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import spark.Request;
import spark.Response;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.recognition.EntityRecognizer;
import uk.ac.cam.sp715.util.HTMLParseException;
import uk.ac.cam.sp715.util.HTMLParser;
import uk.ac.cam.sp715.util.Link;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Stack;

import static spark.Spark.*;

/**
 * Provides a web service interface to access BBC Recipes and
 * in the future will provide the visualisation service.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class RecipeServer {
    private static final StanfordCoreNLP pipeline;
    private static final EntityRecognizer recognizer;
    static {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        pipeline = new StanfordCoreNLP(props);
        recognizer = new EntityRecognizer();
        recognizer.open();
    }
    public static void main(String[] args) {
        port(4567);
        get("/search", RecipeServer::search);
        get("/recipes/:id", RecipeServer::getRecipe);
    }

    private static Recipe labelEntities(Recipe recipe) {
        List<String> instructions = new LinkedList<>();
        synchronized (recognizer) {
            instructions.add(recognizer.annotate(pipeline, recipe));
        }
        return new Recipe(recipe.getTitle(), recipe.getSummary(), recipe.getIngredients(), instructions);
    }

    private static String search(Request request, Response response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            //List<String> paths = HTMLParser.search(request.queryParams("q"));
            List<Link> recipes = HTMLParser.search(request.queryParams("q"));
            List<String> jsonRecipes = new LinkedList<>();
            recipes.forEach((recipe) -> jsonRecipes.add(recipe.toJSON()));
            return mapper.writeValueAsString(jsonRecipes);
        } catch (HTMLParseException | JsonProcessingException e) {
            e.printStackTrace();
            response.status(400);
        }
        return "[]";
    }

    private static String getRecipe(Request request, Response response) {
        try {
            return labelEntities(HTMLParser.getRecipe(request.params(":id"))).toString();
        } catch (HTMLParseException e) {
            e.printStackTrace();
            response.status(400);
        }
        return "[]";
    }
}
