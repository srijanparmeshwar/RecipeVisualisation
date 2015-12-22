package uk.ac.cam.sp715.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import uk.ac.cam.sp715.caching.CacheException;
import uk.ac.cam.sp715.caching.PersistentCache;
import uk.ac.cam.sp715.recipes.Ingredient;
import uk.ac.cam.sp715.recipes.Recipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility functions to parse BBC search and recipe pages. This is built as an alternative to using
 * recipe APIs, most of which are not free.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class HTMLParser {
    private static final Logger logger = Logging.getLogger(HTMLParser.class.getName());
    private static PersistentCache<Recipe> recipeCache;
    static {
        try {
            if(!PersistentCache.exists("cache")) recipeCache = new PersistentCache<>(60);
            else recipeCache = PersistentCache.read("cache");
        } catch (IOToolsException iote) {
            logger.log(Level.SEVERE, "Could not read recipe cache.", iote);
        }
    }

    /**
     * Utility function which takes a recipe query and returns the top (15) search results from the
     * BBC website.
     * @param query Natural language recipe search query.
     * @return {@link List}<{@link Link}> - List of relative paths to recipes on BBC website.
     * @throws HTMLParseException
     */
    public static List<Link> search(String query) throws HTMLParseException {
        try {
            Document document = Jsoup.connect("http://www.bbc.co.uk/food/recipes/search?keywords=" + JSEngine.encodeURI(query)).get();
            //Article class selection.
            Elements articles = document.select(".article");
            //Link selection.
            Elements links = articles.select("a");
            List<Link> recipes = new LinkedList<>();
            for(Element link : links) {
                //Remove the thumbnail if it exists.
                for(Element img : link.getElementsByTag("img")) {
                    img.remove();
                }

                String href = link.attr("href");
                //Filter for actual recipe articles.
                if(href.startsWith("/food/recipes/")) {
                    String path = href.replace("/food/recipes/", "");
                    String name = link.text();
                    recipes.add(new Link(path, name));
                }
            }
            return recipes;
        } catch (IOException e) {
            try {
                return search(query);
            } catch(HTMLParseException ioe) {
                logger.log(Level.SEVERE, "Could not connect to BBC search service for: " + query, e);
                throw ioe;
            }
        }
    }

    /**
     * Utility function to retrieve BBC recipe.
     * Recipe is either retrieved from the cache or from the BBC website.
     * @param path Relative (to 'http://www.bbc.co.uk/food/recipes/') path of recipe on BBC website.
     * @return {@link Recipe} - Parsed recipe with ingredients and instructions separated.
     * @throws HTMLParseException
     */
    public static Recipe getRecipe(String path) throws HTMLParseException {
        PersistentCache.Key key = PersistentCache.Key.get(path);
        try {
            if (recipeCache != null && recipeCache.containsKey(key)) {
                Recipe recipe = recipeCache.get(key);
                return recipe;
            } else {
                Recipe recipe = retrieveRecipe(path);
                if (recipeCache != null) {
                    recipeCache.add(key, recipe);
                }
                return recipe;
            }
        } catch (CacheException cacheException) {
            logger.log(Level.SEVERE, "Error occurred accessing cache for: " + path, cacheException);
            throw new HTMLParseException(path);
        }
    }

    /**
     * Utility function to parse BBC recipe article into {@link Recipe} format.
     * Recipes are retrieved from the BBC website.
     * @param path Relative (to 'http://www.bbc.co.uk/food/recipes/') path of recipe on BBC website.
     * @return {@link Recipe} - Parsed recipe with ingredients and instructions separated.
     * @throws HTMLParseException
     */
    public static Recipe retrieveRecipe(String path) throws HTMLParseException {
        try {
            //Get webpage for this recipe.
            Document document = Jsoup.connect("http://www.bbc.co.uk/food/recipes/" + path).get();
            //Title selection.
            Elements titleElements = document.select("h1");
            //Summary selection.
            Elements summaryElements = document.getElementsByAttributeValue("itemprop", "description");
            if(summaryElements.size() == 0) summaryElements = document.select(".summary");
            //Ingredient class selection.
            Elements ingredientElements = document.getElementsByAttributeValue("itemprop", "ingredients");
            if(ingredientElements.size() == 0) ingredientElements = document.select(".ingredient");
            //Instruction class selection.
            Elements instructionElements = document.getElementsByAttributeValue("itemprop", "recipeInstructions");
            if(instructionElements.size() == 0) instructionElements = document.select(".instruction");

            //Parse ingredients (remove internal 'a href' tags etc. and remove Unicode fractions).
            List<Ingredient> ingredients = new LinkedList<>();
            for(Element ingredientElement : ingredientElements) {
                String ingredient = ingredientElement.text();
                for(String code : UnicodeFractions.fractions.keySet()) {
                    ingredient = ingredient.replaceAll(code, UnicodeFractions.fractions.get(code));
                }
                ingredients.add(new Ingredient(ingredient));
            }

            //Parse instructions (remove internal 'p' tags etc. and remove Unicode fractions).
            List<String> instructions = new LinkedList<>();
            for(Element instructionElement : instructionElements) {
                Elements children = instructionElement.children();
                String instruction = "";
                if(children.size()>0) {
                    Elements paragraphs = instructionElement.select("p");
                    if(paragraphs.size()>0) instruction = paragraphs.get(0).text();
                } else instruction = instructionElement.text();

                for (String code : UnicodeFractions.fractions.keySet()) {
                    instruction = instruction.replaceAll(code, UnicodeFractions.fractions.get(code));
                }

                if(instruction.length()>0) instructions.add(instruction);
            }

            //Parse title and summary of recipe.
            if(titleElements.size()>0) {
                String title = titleElements.get(0).text();
                if(summaryElements.size()>0) {
                    String summary = summaryElements.get(0).text();
                    return new Recipe(title, summary, ingredients, instructions);
                } else return new Recipe(title, "", ingredients, instructions);
            } else {
                if(summaryElements.size()>0) {
                    String summary = summaryElements.get(0).text();
                    return new Recipe("Recipe", summary, ingredients, instructions);
                } else return new Recipe("Recipe", "", ingredients, instructions);
            }
        } catch (IOException e) {
            try {
                return getRecipe(path);
            } catch (HTMLParseException ioe) {
                logger.log(Level.SEVERE, "Could not get webpage for path: " + path, e);
                throw ioe;
            }
        }
    }
}
