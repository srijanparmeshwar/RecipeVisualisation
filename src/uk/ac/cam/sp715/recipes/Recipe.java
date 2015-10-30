package uk.ac.cam.sp715.recipes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Srijan on 30/10/2015.
 */
public class Recipe {
    private final Set<Ingredient> ingredients;
    private final String description;
    public Recipe(List<Ingredient> ingredientsList, List<String> instructions) {
        ingredients = new HashSet<>();
        ingredients.addAll(ingredientsList.stream().collect(Collectors.toList()));

        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for(String instruction : instructions) {
            if(first) {
                builder.append(instruction);
                first = false;
            } else {
                builder.append(" ");
                builder.append(instruction);
            }
        }
        description = builder.toString();
    }
    public Set<Ingredient> getIngredients() {return ingredients;}
    public String getDescription() {return description;}
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for(Ingredient ingredient : ingredients) {
            builder.append(ingredient);
            builder.append("\n");
        }
        builder.append(description);
        return builder.toString();
    }
}
