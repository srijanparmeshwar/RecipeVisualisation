package uk.ac.cam.sp715.recipes;

/**
 * Created by Srijan on 30/10/2015.
 */
public class Ingredient {
    private final String name;
    public Ingredient(String name) {
        this.name = name;
    }
    public String getName() {return name;}
    public String toString() {
        return name;
    }
}
