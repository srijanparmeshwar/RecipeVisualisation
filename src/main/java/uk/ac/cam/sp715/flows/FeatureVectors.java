package uk.ac.cam.sp715.flows;

import edu.stanford.nlp.trees.GrammaticalRelation;
import uk.ac.cam.sp715.recognition.EntityAnnotator;
import uk.ac.cam.sp715.recognition.TaggedWord;
import uk.ac.cam.sp715.wordnet.Taxonomy;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Srijan on 21/12/2015.
 */
public class FeatureVectors {
    public static List<String> intermediateFeatures(String partOfSpeech, int position, Set<GrammaticalRelation> inRelations, Set<GrammaticalRelation> outRelations, Taxonomy.TaxonomyType type) {
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

    public static List<String> getFeatures(TaggedWord token, int position, EntityAnnotator.AugmentedSemanticGraph dependencies, List<TaggedWord> tokens) {
        String pos = token.tag();
        Taxonomy.TaxonomyType type = token.entity();
        Set<GrammaticalRelation> orelations = dependencies.outgoingEdgesOf(token);
        Set<GrammaticalRelation> irelations = dependencies.incomingEdgesOf(token);

        List<String> features = intermediateFeatures(pos, position, irelations, orelations, type);
        features.add("word=" + token.toString());
        if(dependencies.orderedTokens().size()>position + 2) features.add("posTagRight2=" + tokens.get(position + 2).tag());
        if(position>1) features.add("posTagLeft2=" + tokens.get(position - 2).tag());

        if(dependencies.orderedTokens().size()>position + 1) features.add("posTagRight=" + tokens.get(position + 1).tag());
        if(position>0) features.add("posTagLeft=" + tokens.get(position - 1).tag());

        return features;
    }
}
