package uk.ac.cam.sp715.recipes;

/**
 * Ingredient class.
 */
public class Ingredient {
    private final String name;
    public Ingredient(String name) {
        this.name = name;
    }
    public String getName() {return name;}
    public String toString() {
        return name + ".";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ingredient that = (Ingredient) o;

        return !(name != null ? !name.equals(that.name) : that.name != null);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}