package uk.ac.cam.sp715.distsim;

import java.util.HashMap;
import java.util.Map;

/**
 * HashMap based sparse Vector class with usual operations.
 */
public class Vector {
    private final Map<Integer, Double> values;
    public Vector() {values = new HashMap<>();}
    public double get(int index) {
        if(values.containsKey(index)) return values.get(index);
        else return 0;
    }
    public Vector add(Vector v) {
        Vector result = new Vector();
        for (int key : values.keySet()) result.values.put(key, result.get(key) + get(key));
        for (int key : v.values.keySet()) result.values.put(key, result.get(key) + v.get(key));
        return result;
    }
    public Vector addAt(int index, double value) {
        Vector result = new Vector();
        for (int key : values.keySet()) result.values.put(key, get(key));
        result.values.put(index, value + get(index));
        return result;
    }
    public Vector subtract(Vector v) {
        Vector result = new Vector();
        for (int key : values.keySet()) result.values.put(key, result.get(key) + get(key));
        for (int key : v.values.keySet()) result.values.put(key, result.get(key) - v.get(key));
        return result;
    }
    public Vector multiply(double factor) {
        Vector result = new Vector();
        for (int key : values.keySet()) result.values.put(key, get(key)*factor);
        return result;
    }
    public Vector divide(double factor) {
        return multiply(1/factor);
    }
    public double dot(Vector v) {
        double sum = 0;
        for(int key : values.keySet()) {
            sum += get(key)*v.get(key);
        }
        return sum;
    }
    public double length() {
        double sum = 0;
        for(int key : values.keySet()) {
            double value = get(key);
            sum += value*value;
        }
        return Math.sqrt(sum);
    }
    public Vector pmi(Dictionary dictionary, Map<String, Integer> counts, double ftotal, String contextWord) {
        Vector result = new Vector();
        double fc = get(dictionary.index(contextWord));
        for(Integer key : values.keySet()) {
            String w = dictionary.word(key);
            if(counts.containsKey(w)) {
                int fw = counts.get(w);
                double fwc = get(key);
                result.values.put(key, Math.log(fwc*ftotal/(fw*fc))/Math.log(2));
            }
        }
        return result;
    }
    public String toString() {
        return values.toString();
    }
    public void print(Dictionary dictionary) {
        for (Integer key : values.keySet()) {
            System.out.println(dictionary.word(key) + " : " + values.get(key));
        }
    }
}
