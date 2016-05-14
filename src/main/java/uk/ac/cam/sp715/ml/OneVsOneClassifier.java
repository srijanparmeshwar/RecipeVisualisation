package uk.ac.cam.sp715.ml;

import edu.stanford.nlp.classify.*;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Srijan on 22/03/2016.
 */
public class OneVsOneClassifier<L, F> implements Classifier<L, F> {
    private static class BinaryClassifier<L, F> implements Classifier<L, F> {
        private final L labelA;
        private final L labelB;
        private final Classifier<Boolean, F> classifier;
        public BinaryClassifier(L labelA, L labelB, Classifier<Boolean, F> classifier) {
            this.labelA = labelA;
            this.labelB = labelB;
            this.classifier = classifier;
        }

        @Override
        public L classOf(Datum<L, F> example) {
            return Counters.argmax(scoresOf(example));
        }

        @Override
        public Counter<L> scoresOf(Datum<L, F> example) {
            Datum<Boolean, F> proxyExample = new BasicDatum<>(example.asFeatures());
            Counter<Boolean> proxyCounter = classifier.scoresOf(proxyExample);
            Counter<L> counter = new ClassicCounter<>();
            counter.incrementCount(labelA, proxyCounter.getCount(true));
            counter.incrementCount(labelB, proxyCounter.getCount(false));
            return counter;
        }

        @Override
        public Collection<L> labels() {
            return Arrays.asList(labelA, labelB);
        }
    }
    private final List<BinaryClassifier<L, F>> classifiers;
    private final List<L> labels;

    public OneVsOneClassifier(GeneralDataset<L, F> dataset, List<L> labels) {
        this.classifiers = new LinkedList<>();
        this.labels = labels;
        trainClassifiers(dataset, this.labels);
    }

    private void trainClassifiers(GeneralDataset<L, F> dataset, List<L> labels) {
        LogisticClassifierFactory<Boolean, F> factory = new LogisticClassifierFactory<>();
        Map<L, Collection<Datum<L, F>>> subsets = new HashMap<>();

        for(int i = 0; i < dataset.size(); i++) {
            Datum<L, F> datum = dataset.getDatum(i);
            if(!subsets.containsKey(datum.label())) subsets.put(datum.label(), new LinkedList<>());
            subsets.get(datum.label()).add(datum);
        }

        for(int i = 0; i < labels.size(); i++) {
            L labelA = labels.get(i);
            for(int j = i + 1; j < labels.size(); j++) {
                L labelB = labels.get(j);
                GeneralDataset<Boolean, F> subset = new Dataset<>();
                Collection<Datum<Boolean, F>> subsetA = subsets.get(labelA)
                        .stream()
                        .map(datum -> new BasicDatum<Boolean, F>(datum.asFeatures(), true))
                        .collect(Collectors.toList());
                Collection<Datum<Boolean, F>> subsetB = subsets.get(labelB)
                        .stream()
                        .map(datum -> new BasicDatum<Boolean, F>(datum.asFeatures(), false))
                        .collect(Collectors.toList());
                subset.addAll(subsetA);
                subset.addAll(subsetB);
                classifiers.add(new BinaryClassifier<>(labelA, labelB, factory.trainClassifier(subset)));
            }
        }
    }

    @Override
    public L classOf(Datum<L, F> example) {
        return Counters.argmax(scoresOf(example));
    }

    @Override
    public Counter<L> scoresOf(Datum<L, F> example) {
        Counter<L> counter = new ClassicCounter<>();
        for(Classifier<L, F> classifier : classifiers) {
            counter.incrementCount(classifier.classOf(example));
        }
        return counter;
    }

    @Override
    public Collection<L> labels() {
        return labels;
    }

}
