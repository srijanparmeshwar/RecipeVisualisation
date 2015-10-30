package uk.ac.cam.sp715.util;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by Srijan on 30/10/2015.
 */
public class Logging {
    public static Logger getLogger(String s) {
        try {
            Logger logger = Logger.getLogger(s);
            FileHandler fileHandler = new FileHandler("log.txt");
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
            return logger;
        } catch (IOException e) {
            throw new LoggingException();
        }
    }
}
