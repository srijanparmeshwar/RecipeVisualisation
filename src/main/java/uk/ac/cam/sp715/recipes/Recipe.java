package uk.ac.cam.sp715.recipes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recipe class, with a set of ingredients and a description (concatenated instructions).
 */
public class Recipe {
    private final Set<Ingredient> ingredients;
    private final String description;

    public Recipe(List<Ingredient> ingredientsList, List<String> instructions) {
        ingredients = new HashSet<>();
        ingredients.addAll(ingredientsList);

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Recipe recipe = (Recipe) o;

        if (ingredients != null ? !ingredients.equals(recipe.ingredients) : recipe.ingredients != null) return false;
        return !(description != null ? !description.equals(recipe.description) : recipe.description != null);
    }

    @Override
    public int hashCode() {
        int result = ingredients != null ? ingredients.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
