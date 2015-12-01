package uk.ac.cam.sp715.recipes;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Recipe class, with a set of ingredients and a description (concatenated instructions).
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class Recipe implements Serializable {
    private final String title;
    private final String summary;
    private final List<Ingredient> ingredients;
    private final String description;

    public Recipe(String title, String summary, List<Ingredient> ingredientsList, List<String> instructions) {
        this.title = title;
        this.summary = summary;
        this.ingredients = new LinkedList<>();
        this.ingredients.addAll(ingredientsList);

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

        this.description = builder.toString();
    }
    public String getTitle() {return title;}
    public String getSummary() {return summary;}
    public List<Ingredient> getIngredients() {return ingredients;}
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

        if (title != null ? !title.equals(recipe.title) : recipe.title != null) return false;
        if (summary != null ? !summary.equals(recipe.summary) : recipe.summary != null) return false;
        if (ingredients != null ? !ingredients.equals(recipe.ingredients) : recipe.ingredients != null) return false;
        return !(description != null ? !description.equals(recipe.description) : recipe.description != null);

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (summary != null ? summary.hashCode() : 0);
        result = 31 * result + (ingredients != null ? ingredients.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
