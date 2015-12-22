package uk.ac.cam.sp715.util;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

/**
 * Created by Srijan on 15/12/2015.
 */
public class Pipeline {
    public static StanfordCoreNLP getMainPipeline() {
        Properties mainProps = new Properties();
        mainProps.setProperty("customAnnotatorClass.entities", "uk.ac.cam.sp715.recognition.EntityAnnotator");
        mainProps.setProperty("annotators",
                "tokenize, ssplit, pos, lemma, ner, depparse, entities");
        return new StanfordCoreNLP(mainProps);
    }
    public static StanfordCoreNLP getLemmaPipeline() {
        Properties props = new Properties();
        props.setProperty("annotators",
                "tokenize, ssplit, pos, lemma");
        return new StanfordCoreNLP(props);
    }
}
