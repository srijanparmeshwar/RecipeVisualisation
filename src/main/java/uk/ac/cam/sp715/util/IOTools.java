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
 * Created by Srijan on 17/12/2015.
 */
public class IOTools {
    private static final Logger logger = Logging.getLogger(IOTools.class.getName());

    public static void save(Collection<? extends Object> data, Path path) throws IOToolsException {
        try {
            Files.write(path, data.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList()));
        } catch(IOException ioException) {
            logger.log(Level.SEVERE, "Could not write collection to file.", ioException);
            throw new IOToolsException();
        }
    }

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

    public static boolean exists(String filename) {
        return Files.exists(Paths.get(filename));
    }

    public static void delete(String filename) throws IOToolsException {
        try {
            Files.delete(Paths.get(filename));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not delete serialized file.", e);
            throw new IOToolsException();
        }
    }
}
