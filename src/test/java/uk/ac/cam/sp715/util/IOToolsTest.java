package uk.ac.cam.sp715.util;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for {@link IOTools} utility methods for serializable and collection
 * objects.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class IOToolsTest {

    private static final String directoryPath = "testfiles";

    @BeforeClass
    public static void beforeClass() {
        File testDirectory = new File(directoryPath);
        if(!testDirectory.exists()) testDirectory.mkdir();
    }

    @Test
    public void testCollection() {
        try {
            Path filepath = Paths.get(directoryPath, "collection.txt");
            List<String> lines = new LinkedList<>();
            lines.add("Hello");
            lines.add("World");
            IOTools.save(lines, filepath);
            assertTrue(Files.exists(filepath));

            List<String> fromFile = Files.readAllLines(filepath);
            assertEquals(lines, fromFile);
            IOTools.delete(filepath.toString());
            assertFalse(Files.exists(filepath));
        } catch (IOToolsException | IOException exception) {
            fail(exception.getMessage());
        }
    }

    @Test
    public void testSerializableArray() {
        try {
            Path filepath = Paths.get(directoryPath, "array.ser");
            String filename = filepath.toString();
            int[] array = {1, 2, 3};
            IOTools.save(array, filename);
            assertTrue(IOTools.exists(filename));

            int[] fromFile = IOTools.read(filename);
            assertArrayEquals(array, fromFile);
            IOTools.delete(filename);
            assertFalse(IOTools.exists(filename));
        } catch (IOToolsException exception) {
            fail(exception.getMessage());
        }
    }

    private static class TestClass implements Serializable {
        private final int id;
        private final String name;
        public TestClass(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestClass testClass = (TestClass) o;

            if (id != testClass.id) return false;
            return !(name != null ? !name.equals(testClass.name) : testClass.name != null);

        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "[" + id + ", " + name + "]";
        }
    }

    @Test
    public void testSerializableObject() {
        try {
            Path filepath = Paths.get(directoryPath, "object.ser");
            String filename = filepath.toString();
            TestClass object = new TestClass(1, "Hello world!");
            IOTools.save(object, filename);
            assertTrue(IOTools.exists(filename));

            TestClass fromFile = IOTools.read(filename);
            assertEquals(object, fromFile);
            IOTools.delete(filename);
            assertFalse(IOTools.exists(filename));
        } catch (IOToolsException exception) {
            fail(exception.getMessage());
        }
    }

    @AfterClass
    public static void afterClass() {
        File testDirectory = new File(directoryPath);
        if(testDirectory.exists()) testDirectory.delete();
    }
}
