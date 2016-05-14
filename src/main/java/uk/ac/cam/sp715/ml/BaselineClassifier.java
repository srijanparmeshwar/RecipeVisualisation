package uk.ac.cam.sp715.ml;

import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import uk.ac.cam.sp715.flows.Role;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Baseline classifier for determining whether a token is an
 * ACTION, DOBJECT, IOBJECT or OTHER. It uses basic heuristics,
 * labelling verbs and starting tokens as ACTION, NER ingredients as
 * DOBJECT, NER utensils and appliances as IOBJECT, any remaining nouns as
 * IOBJECT and the rest as OTHER.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class BaselineClassifier implements Classifier<Role, String> {
    @Override
    public Role classOf(Datum<Role, String> example) {
        for(Role role : Role.values()) {
            if (scoresOf(example).getCount(role) == 1) return role;
        }
        return Role.OTHER;
    }

    private void translateFeature(String featureName, String feature, Map<String, String> baselineFeatures) {
        if(feature.startsWith(featureName)) {
            baselineFeatures.put(featureName, feature.replaceAll(featureName, ""));
        }
    }

    private static Counter<Role> counter(Role role) {
        Counter<Role> counter = new ClassicCounter<>();
        for(Role roleCandidate : Role.values()) {
            if(role == roleCandidate) counter.setCount(roleCandidate, 1);
            else counter.setCount(roleCandidate, 0);
        }
        return counter;
    }

    @Override
    public Counter<Role> scoresOf(Datum<Role, String> example) {
        Collection<String> features = example.asFeatures();
        Map<String, String> baselineFeatures = new HashMap<>();
        for(String feature : features) {
            translateFeature("posTag=", feature, baselineFeatures);
            translateFeature("ner=", feature, baselineFeatures);
            translateFeature("atStart", feature, baselineFeatures);
            translateFeature("inReldobj", feature, baselineFeatures);
            translateFeature("inReliobj", feature, baselineFeatures);
        }

        if(baselineFeatures.containsKey("atStart") || baselineFeatures.get("posTag=").startsWith("V")) {
            return counter(Role.ACTION);
        } else if(baselineFeatures.containsKey("inReldobj") || baselineFeatures.get("ner=").equals("INGREDIENTS")) {
            return counter(Role.DOBJECT);
        } else if(baselineFeatures.containsKey("inReliobj") || baselineFeatures.get("ner=").equals("UTENSILS") || baselineFeatures.get("ner=").equals("APPLIANCES") || baselineFeatures.get("posTag=").startsWith("N")) {
            return counter(Role.IOBJECT);
        } else return counter(Role.OTHER);
    }

    @Override
    public Collection<Role> labels() {
        return Arrays.asList(Role.values());
    }
}
