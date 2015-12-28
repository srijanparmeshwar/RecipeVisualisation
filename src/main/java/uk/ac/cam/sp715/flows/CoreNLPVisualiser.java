package uk.ac.cam.sp715.flows;

import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import uk.ac.cam.sp715.ml.DataHandler;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.recognition.EntityAnnotator;
import uk.ac.cam.sp715.recognition.EntityAnnotator.AugmentedSemanticGraph;
import uk.ac.cam.sp715.recognition.TaggedWord;
import uk.ac.cam.sp715.util.*;
import uk.ac.cam.sp715.wordnet.Taxonomy.TaxonomyType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by Srijan on 26/11/2015.
 */
public class CoreNLPVisualiser extends Visualiser {
    private final StanfordCoreNLP pipeline;
    private final Classifier<Role, String> classifier;
    private final Map<String, Action> frontiers;

    public CoreNLPVisualiser(StanfordCoreNLP pipeline) {
        try {
            this.pipeline = pipeline;
            this.classifier = DataHandler.getClassifier(pipeline);
            this.frontiers = new HashMap<>();
        } catch(IOToolsException iote) {
            throw new RuntimeException();
        }
    }

    private static <T> List<T> filter(Collection<T> relations, Predicate<? super T> predicate) {
        return relations.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private void addDependencies(Action newAction, Flow flow) {
        PriorityQueue<Action> previousActions = new PriorityQueue<>((o1, o2) -> {
            return Integer.compare(o2.getID(), o1.getID());
        });

        for (TaggedWord word : newAction.getObjects()) {
            for(IndexedWord iword : word.getTokens()) {
                if(frontiers.containsKey(iword.lemma()) && word.entity() != TaxonomyType.APPLIANCES) previousActions.add(frontiers.get(iword.lemma()));
            }
            /*if(frontiers.containsKey(word.getLemma()) && word.entity() == TaxonomyType.INGREDIENTS) previousActions.add(frontiers.get(word.getLemma()));
            frontiers.put(word.getLemma(), newAction);*/
            if(previousActions.size()>0) {
                Action lastAction = previousActions.poll();
                flow.addEdge(lastAction, newAction);
            }
        }

        for (TaggedWord word : newAction.getObjects()) {
            for(IndexedWord iword : word.getTokens()) {
                frontiers.put(iword.lemma(), newAction);
            }
        }

        if(flow.inDegreeOf(newAction) == 0) {
            flow.vertexSet()
                    .stream()
                    .filter(action -> action.getID() < newAction.getID())
                    .max((actionA, actionB) ->
                            Integer.compare(actionA.getID(), actionB.getID()))
                    .ifPresent(action -> {
                        flow.addEdge(action, newAction);
                    });
        }
    }

    private Action lastAction;
    private int id = 0;

    @Override
    public Flow parse(Recipe recipe) {
        Annotation annotation = new Annotation(recipe.getDescription());
        pipeline.annotate(annotation);

        Flow flow = new Flow();
        frontiers.clear();

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

        id = 0;

        for (CoreMap sentence : sentences) {
            AugmentedSemanticGraph dependencies = sentence.get(EntityAnnotator.EntityAnnotations.class);

            SortedMap<Integer, Action> indices = new TreeMap<>();
            Set<TaggedWord> processedObjects = new HashSet<>();
            Map<TaggedWord, Integer> candidateObjects = new HashMap<>();
            //Map<Features.Role, Set<TaggedWord>> buckets = new HashMap<>();
            //for(Features.Role key : buckets.keySet()) buckets.put(key, new HashSet<>());

            int position = 0;

            List<TaggedWord> tokens = dependencies.orderedTokens();
            for (TaggedWord token : tokens) {
                List<String> features = FeatureVectors.getFeatures(token, position, dependencies, tokens);

                Role role = classifier.classOf(new BasicDatum<>(features));

                if (role == Role.DOBJECT || role == Role.IOBJECT) {
                    candidateObjects.put(token, id);
                } else if (role == Role.ACTION) {
                    List<TaggedWord> dependentObjects = dependencies.outgoingEdgesOf(token)
                            .stream()
                            .map(dependencies::getEdgeTarget)
                            .filter(TaggedWord::isTypedEntity)
                            .collect(Collectors.toList());

                    processedObjects.addAll(dependentObjects);

                    Action action = new Action(id, token, dependentObjects);

                    indices.put(id, action);
                    flow.addVertex(action);

                    lastAction = action;
                    id++;
                }

                position++;
            }

            for (TaggedWord candidateObject : candidateObjects.keySet()) {
                if (!processedObjects.contains(candidateObject)) {
                    int index = candidateObjects.get(candidateObject);
                    if (indices.containsKey(index - 1)) indices.get(index - 1).addObject(candidateObject);
                    else if (indices.containsKey(index)) indices.get(index).addObject(candidateObject);
                }
            }

            for (Action action : indices.values()) {
                addDependencies(action, flow);
            }
        }

        flow.vertexSet()
                .stream()
                .filter(action -> action.getID() != lastAction.getID() && flow.outDegreeOf(action) == 0)
                .forEach(action -> flow.addEdge(action, lastAction));

        return flow;
    }

    public static void main(String[] args) throws HTMLParseException, IOToolsException {
        StanfordCoreNLP pipeline = Pipeline.getMainPipeline();
        CoreNLPVisualiser visualiser = new CoreNLPVisualiser(pipeline);
        Recipe recipe = HTMLParser.getRecipe(HTMLParser.search("chocolate").get(0).getLink());
        visualiser.parse(recipe);
    }
}
