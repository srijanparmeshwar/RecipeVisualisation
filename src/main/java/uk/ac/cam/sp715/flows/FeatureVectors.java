package uk.ac.cam.sp715.flows;

import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.trees.GrammaticalRelation;
import uk.ac.cam.sp715.ml.Feature;
import uk.ac.cam.sp715.recognition.EntityAnnotator;
import uk.ac.cam.sp715.recognition.TaggedWord;
import uk.ac.cam.sp715.wordnet.Taxonomy;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class to obtain features to feed into classifier.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class FeatureVectors {
    private static List<String> intermediateFeatures(String partOfSpeech, int position, Set<GrammaticalRelation> inRelations, Set<GrammaticalRelation> outRelations, Taxonomy.TaxonomyType type) {
        List<String> features = new LinkedList<>();
        features.add("posTag=" + partOfSpeech);
        if(position == 0) features.add("atStart");

        Set<String> inNames = inRelations
                .stream()
                .map(GrammaticalRelation::getShortName)
                .collect(Collectors.toSet());

        Set<String> outNames = outRelations
                .stream()
                .map(GrammaticalRelation::getShortName)
                .collect(Collectors.toSet());

        for(String inName : inNames) features.add("inRel" + inName);
        for(String outName : outNames) features.add("outRel" + outName);

        features.add("ner=" + type);
        return features;
    }

    /**
     * Retrieves and converts required features to strings.
     * @param token Current token.
     * @param position Position in sentence.
     * @param dependencies Dependencies in the whole text.
     * @param tokens Sentence containing the current token.
     * @return List of features as strings.
     */
    public static List<String> getFeatures(TaggedWord token, int position, EntityAnnotator.AugmentedSemanticGraph dependencies, List<TaggedWord> tokens) {
        return getFeaturesOriginal(token, position, dependencies, tokens);
    }

    private static List<String> getFeaturesOriginal(TaggedWord token, int position, EntityAnnotator.AugmentedSemanticGraph dependencies, List<TaggedWord> tokens) {
        String pos = token.tag();
        Taxonomy.TaxonomyType type = token.entity();
        Set<GrammaticalRelation> orelations = dependencies.outgoingEdgesOf(token);
        Set<GrammaticalRelation> irelations = dependencies.incomingEdgesOf(token);

        List<String> features = intermediateFeatures(pos, position, irelations, orelations, type);
        features.add("word=" + token.toString().toLowerCase());

        if(dependencies.orderedTokens().size() > position + 1) features.add("posTagRight=" + tokens.get(position + 1).tag());
        if(position > 0) features.add("posTagLeft=" + tokens.get(position - 1).tag());

        if(dependencies.orderedTokens().size() > position + 2) features.add("posTagRight2=" + tokens.get(position + 2).tag());
        if(position > 1) features.add("posTagLeft2=" + tokens.get(position - 2).tag());

        /*if(dependencies.orderedTokens().size()>position + 3) features.add("posTagRight3=" + tokens.get(position + 3).tag());
        if(position>2) features.add("posTagLeft3=" + tokens.get(position - 3).tag());*/

        return features;
    }

    private static String trim(String string, int length) {
        return string.substring(0, Math.min(length, string.length()));
    }

    private static String trim(String string) {
        return trim(string, 2);
    }

    private static List<String> getFeaturesAdv(TaggedWord token, int position, EntityAnnotator.AugmentedSemanticGraph dependencies, List<TaggedWord> tokens) {
        String pos = token.tag();
        Taxonomy.TaxonomyType type = token.entity();
        Set<GrammaticalRelation> orelations = dependencies.outgoingEdgesOf(token);
        Set<GrammaticalRelation> irelations = dependencies.incomingEdgesOf(token);

        List<String> features = intermediateFeatures(pos, position, irelations, orelations, type);
        features.add("word=" + token.getLemma().toLowerCase());

        if(dependencies.orderedTokens().size() > position + 1 && position > 0) {
            features.add("posTagSeq=" + trim(tokens.get(position - 1).tag()) + trim(token.tag()) + trim(tokens.get(position + 1).tag()));
        }

        if(position > 2) {
            features.add("posTagSeqLeft=" + trim(tokens.get(position - 3).tag()) + trim(tokens.get(position - 2).tag()) + trim(tokens.get(position - 1).tag()));
        }

        return features;
    }

    private static List<String> getLemmas(Stream<TaggedWord> stream) {
        return stream.flatMap(word -> word.getTokens().stream())
                .map(IndexedWord::lemma)
                .collect(Collectors.toList());
    }

    private static List<String> features(Action src, Action dst, Flow flow, Map<Integer, TaggedWord> possibleDiscourseMarkers) {
        List<String> features = new LinkedList<>();
        features.add("srcdst=" + src.description().toLowerCase() + dst.description().toLowerCase());
        features.add("src=" + src.description().toLowerCase());
        features.add("dst=" + dst.description().toLowerCase());
        double dobjIobjMatches = 0;
        double dobjDobjMatches = 0;
        for(TaggedWord dobj : src.getDObjects()) {
            for(IndexedWord word : dobj.getTokens()) {
                List<String> iobjLemmas = getLemmas(dst.getIObjects().stream());
                dobjIobjMatches += iobjLemmas
                        .stream()
                        .filter(lemma -> lemma.equals(word.lemma()))
                        .count();

                List<String> dobjLemmas = getLemmas(dst.getDObjects().stream());
                dobjDobjMatches += dobjLemmas
                        .stream()
                        .filter(lemma -> lemma.equals(word.lemma()))
                        .count();
            }
        }
        //dobjMatches = dobjSize > 0 ? dobjMatches/dobjSize : 0;

        if(src.getID() - dst.getID() < 3) {
            for(int i = 0; i < 2; i++) {
                if(possibleDiscourseMarkers.containsKey(src.getID() + i)) features.add("discMarker=" + possibleDiscourseMarkers.get(src.getID() + i));
            }
        }

        double iobjIobjMatches = 0;
        double iobjDobjMatches = 0;
        for(TaggedWord iobj : src.getIObjects()) {
            for(IndexedWord word : iobj.getTokens()) {
                List<String> iobjLemmas = getLemmas(dst.getIObjects().stream());
                iobjIobjMatches += iobjLemmas
                        .stream()
                        .filter(lemma -> lemma.equals(word.lemma()))
                        .count();

                List<String> dobjLemmas = getLemmas(dst.getDObjects().stream());
                iobjDobjMatches += dobjLemmas
                        .stream()
                        .filter(lemma -> lemma.equals(word.lemma()))
                        .count();
            }
        }
        //iobjMatches = iobjSize > 0 ? iobjMatches/iobjSize : 0;
        if(dobjIobjMatches>0) features.add("dobjIobjMatch");
        if(dobjDobjMatches>0) features.add("dobjDobjMatch");
        if(iobjIobjMatches>0) features.add("iobjIobjMatch");
        if(iobjDobjMatches>0) features.add("iobjDobjMatch");
        if(dobjDobjMatches == 0 && dobjIobjMatches == 0 && iobjDobjMatches == 0 && iobjIobjMatches == 0) features.add("noMatch");
        if(dst.getID() - src.getID() < 2) features.add("adjacent");
        else features.add("distant");
        if(dst.sentIndex() - src.sentIndex() == 0) features.add("sameSent");
        else features.add("diffSent");
        if(flow.containsEdge(src, dst)) features.add("heuristicEdge");
        else features.add("noHeuristicEdge");
        return features;
    }

    public static Datum<Boolean, String> getDatum(Action src, Action dst, Flow flow) {
        return new BasicDatum<>(features(src, dst, flow, new HashMap<>()));
    }

    public static Datum<Boolean, String> getDatum(Action src, Action dst, Flow flow, boolean label) {
        return new BasicDatum<>(features(src, dst, flow, new HashMap<>()), label);
    }

    /*
    public static Datum<Boolean, String> getDatum(Action src, Action dst) {
        return new RVFDatum<>(getCounter(src, dst));
    }

    public static Datum<Boolean, String> getDatum(Action src, Action dst, boolean label) {
        return new RVFDatum<>(getCounter(src, dst), label);
    }


    private static Counter<String> getCounter(Action src, Action dst) {
        Counter<String> counter = new ClassicCounter<>();
        //counter.setCount(Feature.src + src.description().toLowerCase(), 1);
        //counter.setCount(Feature.dst + dst.description().toLowerCase(), 1);
        counter.setCount(Feature.srcdst + src.description().toLowerCase() + dst.description().toLowerCase(), 1);
        double dobjMatches = 0;
        double dobjSize = 0;
        for(TaggedWord dobj : src.getDObjects()) {
            for(IndexedWord word : dobj.getTokens()) {
                List<String> lemmas = dst.getDObjects()
                        .stream()
                        .flatMap(dstDobj -> dstDobj.getTokens().stream())
                        .map(IndexedWord::lemma)
                        .collect(Collectors.toList());
                dobjMatches += lemmas
                        .stream()
                        .filter(lemma -> lemma.equals(word.lemma()))
                        .count();
                dobjSize += lemmas
                        .stream()
                        .count();
            }
        }
        //dobjMatches = dobjSize > 0 ? dobjMatches/dobjSize : 0;


        double iobjMatches = 0;
        double iobjSize = 0;
        for(TaggedWord iobj : src.getIObjects()) {
            for(IndexedWord word : iobj.getTokens()) {
                List<String> lemmas = dst.getIObjects()
                        .stream()
                        .flatMap(dstIobj -> dstIobj.getTokens().stream())
                        .map(IndexedWord::lemma)
                        .collect(Collectors.toList());
                iobjMatches += lemmas
                        .stream()
                        .filter(lemma -> lemma.equals(word.lemma()))
                        .count();
                iobjSize += lemmas
                        .stream()
                        .count();
            }
        }
        //iobjMatches = iobjSize > 0 ? iobjMatches/iobjSize : 0;
        counter.setCount(Feature.dobjMatches.toString(), dobjMatches);
        counter.setCount(Feature.iobjMatches.toString(), iobjMatches);
        counter.setCount(Feature.tokDist.toString(), dst.getID() - src.getID());
        counter.setCount(Feature.adjacent.toString(), dst.getID() - src.getID() < 3 ? 1 : 0);
        counter.setCount(Feature.sentDist.toString(), dst.sentIndex() - src.sentIndex());
        System.out.println(counter);
        return counter;
    }*/
}
