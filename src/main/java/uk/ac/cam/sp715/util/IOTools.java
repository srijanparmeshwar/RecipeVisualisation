package uk.ac.cam.sp715.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Set of utility functions to handle common IO operations e.g. serializing
 * and deserializing objects.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class IOTools {
    private static final Logger logger = Logging.getLogger(IOTools.class);

    /**
     * Takes a collection and filepath and saves the collection to a file at
     * that location such that the collection is iterated over and each line in the file
     * is the result of calling each object's toString function.
     * @param data Lines to be converted to strings and saved.
     * @param path Filepath.
     * @throws IOToolsException - Thrown when IO errors occur.
     */
    public static void save(Collection<?> data, Path path) throws IOToolsException {
        try {
            Files.write(path, data.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList()));
        } catch(IOException ioException) {
            logger.log(Level.SEVERE, "Could not write collection to file.", ioException);
            throw new IOToolsException();
        }
    }

    /**
     * Saves a serializable object to the given filename.
     * @param serializable Object to be serialized.
     * @param filename Filename.
     * @throws IOToolsException - Thrown when IO errors occur.
     */
    public static void save(Serializable serializable, String filename) throws IOToolsException {
        try(FileOutputStream file = new FileOutputStream(filename);
                  BufferedOutputStream buffer = new BufferedOutputStream(file);
                  ObjectOutputStream output = new ObjectOutputStream(buffer)) {
            output.writeObject(serializable);
            output.flush();
        } catch(IOException ioException) {
            logger.log(Level.SEVERE, "Could not write object to file.", ioException);
            throw new IOToolsException();
        }
    }

    /**
     * Reads a given serialized file and converts it back to the given object type.
     * @param filename File to be deserialized.
     * @param <T> Type of object (must be {@link Serializable}).
     * @return The deserialized object.
     * @throws IOToolsException
     */
    public static <T extends Serializable> T read(String filename) throws IOToolsException {
        try(FileInputStream file = new FileInputStream(filename);
            BufferedInputStream buffer = new BufferedInputStream(file);
            ObjectInputStream input = new ObjectInputStream(buffer)) {
            return (T) input.readObject();
        } catch(IOException ioException) {
            logger.log(Level.SEVERE, "Could not read object from file.", ioException);
            throw new IOToolsException();
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.SEVERE, "Could not find class.", cnfe);
            throw new IOToolsException();
        } catch(ClassCastException cce) {
            logger.log(Level.SEVERE, "Class cast exception.", cce);
            throw new IOToolsException();
        }
    }

    /**
     * Checks whether a file at the given location exists.
     * @param filename Filename.
     * @return True if the file exists, false if it does not exist.
     */
    public static boolean exists(String filename) {
        return Files.exists(Paths.get(filename));
    }

    /**
     * Deletes the file at the given location.
     * @param filename Filename.
     * @throws IOToolsException - Thrown if an IO error occurs.
     */
    public static void delete(String filename) throws IOToolsException {
        try {
            Files.delete(Paths.get(filename));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not delete serialized file.", e);
            throw new IOToolsException();
        }
    }
}
