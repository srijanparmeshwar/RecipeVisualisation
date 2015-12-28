package uk.ac.cam.sp715.recipes;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Recipe class, with a set of ingredients and a description (concatenated instructions).
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class Recipe implements Serializable {
    private final String title;
    private final String summary;
    private final List<Ingredient> ingredients;
    private final List<String> instructions;

    public Recipe(String title, String summary, List<Ingredient> ingredientsList, List<String> instructions) {
        this.title = title;
        this.summary = summary;
        this.ingredients = new LinkedList<>(ingredientsList);
        this.instructions = new LinkedList<>(instructions);
    }
    public String getTitle() {return title;}
    public String getSummary() {return summary;}
    public List<Ingredient> getIngredients() {return ingredients;}
    public List<String> getInstructions() {return instructions;}
    public String getDescription() {
        return instructions
                .stream()
                .collect(Collectors.joining(" "));
    }
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for(Ingredient ingredient : ingredients) {
            builder.append(ingredient);
            builder.append("\n");
        }
        builder.append(getDescription());
        return builder.toString();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Recipe recipe = (Recipe) o;

        if (title != null ? !title.equals(recipe.title) : recipe.title != null) return false;
        if (summary != null ? !summary.equals(recipe.summary) : recipe.summary != null) return false;
        if (!ingredients.equals(recipe.ingredients)) return false;
        return !(!instructions.equals(recipe.instructions));

    }
    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (summary != null ? summary.hashCode() : 0);
        result = 31 * result + (ingredients.hashCode());
        result = 31 * result + (instructions.hashCode());
        return result;
    }
}
