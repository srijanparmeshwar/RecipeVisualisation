package uk.ac.cam.sp715.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import uk.ac.cam.sp715.recipes.Ingredient;
import uk.ac.cam.sp715.recipes.Recipe;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility functions to parse BBC search and recipe pages. This is built as an alternative to using
 * recipe APIs, most of which are not free.
 */
public class HTMLParser {
    private static final Logger logger = Logging.getLogger(HTMLParser.class.getName());

    /**
     * Utility function which takes a recipe query and returns the top search results from the
     * BBC website.
     * @param query - Natural language recipe search query.
     * @return {@link List}<{@link String}> - List of relative paths to recipes on BBC website.
     * @throws HTMLParseException
     */
    public static List<String> search(String query) throws HTMLParseException {
        try {
            Document document = Jsoup.connect("http://www.bbc.co.uk/food/recipes/search?keywords=" + JSEngine.encodeURI(query)).get();
            //Article class selection.
            Elements articles = document.select(".article");
            //Link selection.
            Elements links = articles.select("a");
            List<String> recipes = new LinkedList<>();
            for(Element link : links) {
                String href = link.attr("href");
                //Filter for actual recipe articles.
                if(href.startsWith("/food/recipes/")) {
                    String name = href.replace("/food/recipes/", "");
                    recipes.add(name);
                }
            }
            return recipes;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not connect to BBC search service for: " + query, e);
            throw new HTMLParseException(query);
        }
    }

    /**
     * Utility function to parse BBC recipe article into {@link Recipe} format.
     * @param path - Relative (to 'http://www.bbc.co.uk/food/recipes/') path of recipe on BBC website.
     * @return {@link Recipe} - Parsed recipe with ingredients and instructions separated.
     * @throws HTMLParseException
     */
    public static Recipe getRecipe(String path) throws HTMLParseException {
        try {
            //Get webpage for this recipe.
            Document document = Jsoup.connect("http://www.bbc.co.uk/food/recipes/" + path).get();
            //Ingredient class selection.
            Elements ingredientElements = document.select(".ingredient");
            //Instruction class selection.
            Elements instructionElements = document.select(".instruction");

            //Parse ingredients (remove internal 'a href' tags etc.)
            List<Ingredient> ingredients = new LinkedList<>();
            for(Element ingredientElement : ingredientElements) {
                String ingredient = ingredientElement.text();
                ingredients.add(new Ingredient(ingredient));
            }

            //Parse instructions (remove internal 'p' tags etc.)
            List<String> instructions = new LinkedList<>();
            for(Element instructionElement : instructionElements) {
                String instruction = instructionElement.text();
                instructions.add(instruction);
            }

            return new Recipe(ingredients, instructions);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not get webpage for path: " + path, e);
            throw new HTMLParseException(path);
        }
    }
    public static void main(String[] args) {
        try {
            List<String> links = search("halloween");
            Recipe recipe = getRecipe(links.get(0));
            System.out.println(recipe);
        } catch (HTMLParseException e) {
            e.printStackTrace();
        }
    }
}
