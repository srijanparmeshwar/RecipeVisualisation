package uk.ac.cam.sp715.evaluation;

/**
 * Created by Srijan on 21/02/2016.
 */
public class Vertex {
    private final int id;
    private final String label;
    public Vertex(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getID() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Vertex vertex = (Vertex) o;

        return id == vertex.id && (label != null ? label.equals(vertex.label) : vertex.label == null);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (label != null ? label.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "[" + id + ", " + label + "]";
    }
}
