package uk.ac.cam.sp715.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;
import spark.Response;
import uk.ac.cam.sp715.util.HTMLParseException;
import uk.ac.cam.sp715.util.HTMLParser;
import uk.ac.cam.sp715.util.Link;

import java.util.LinkedList;
import java.util.List;

import static spark.Spark.*;

/**
 * Provides a web service interface to access BBC Recipes and
 * in the future will provide the visualisation service.
 */
public class RecipeServer {

    public static void main(String[] args) {
        port(4567);
        get("/search", RecipeServer::search);
        get("/recipes/:id", RecipeServer::getRecipe);
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
            return HTMLParser.getRecipe(request.params(":id")).toString();
        } catch (HTMLParseException e) {
            e.printStackTrace();
            response.status(400);
        }
        return "[]";
    }
}
