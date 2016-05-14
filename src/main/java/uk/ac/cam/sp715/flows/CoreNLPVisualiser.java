package uk.ac.cam.sp715.flows;

import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import uk.ac.cam.sp715.ml.SRLDataHandler;
import uk.ac.cam.sp715.recipes.Recipe;
import uk.ac.cam.sp715.recognition.EntityAnnotator;
import uk.ac.cam.sp715.recognition.EntityAnnotator.AugmentedSemanticGraph;
import uk.ac.cam.sp715.recognition.TaggedWord;
import uk.ac.cam.sp715.util.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Visualiser which uses CoreNLP annotators to provide information to
 * determine dependencies. Actions are recognized using a linear classifier.
 * It is used to produce flow graphs from recipe texts.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class CoreNLPVisualiser extends Visualiser {
    private final Pipeline pipeline;
    private final Classifier<Role, String> classifier;
    private final Map<String, Action> frontiers;

    /**
     * Constructs a visualiser.
     * @param pipeline CoreNLP pipeline used to tag and parse text.
     */
    public CoreNLPVisualiser(Pipeline pipeline) {
        try {
            this.pipeline = pipeline;
            this.classifier = SRLDataHandler.getClassifier();
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
                if(frontiers.containsKey(iword.lemma())) previousActions.add(frontiers.get(iword.lemma()));
            }
            /*if(frontiers.containsKey(word.getLemma()) && word.entity() == TaxonomyType.INGREDIENTS) previousActions.add(frontiers.get(word.getLemma()));
            frontiers.put(word.getLemma(), newAction);*/
            //int num = 0;
            while(previousActions.size() > 0) {
                Action lastAction = previousActions.poll();
                flow.addEdge(lastAction, newAction);
                //num++;
            }
        }

        for (TaggedWord word : newAction.getObjects()) {
            for(IndexedWord iword : word.getTokens()) {
                frontiers.put(iword.lemma(), newAction);
            }
        }

        /*
        if(flow.inDegreeOf(newAction) == 0) {
            flow.vertexSet()
                    .stream()
                    .filter(action -> action.getID() < newAction.getID())
                    .max((actionA, actionB) ->
                            Integer.compare(actionA.getID(), actionB.getID()))
                    .ifPresent(action -> {
                        flow.addEdge(action, newAction);
                    });
        }*/
    }

    private Action lastAction;
    private static final String[] discMarkers = new String[] {"then", "and"};

    @Override
    /**
     * Parses the recipe description and produces a directed graph of actions and their dependencies.
     * The algorithm used parses the text for dependencies, and part of speech tags and then
     * uses a linear classifier to identify actions and objects.
     * This information is then used to identify dependencies using different heuristics.
     * @param recipe The recipe to be visualised as a graph.
     */
    public Flow parse(Recipe recipe) {
        Annotation annotation = pipeline.annotate(recipe);

        Flow flow = new Flow();
        frontiers.clear();

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

        int id = 0;
        SortedMap<Integer, Action> indices = new TreeMap<>();

        for (CoreMap sentence : sentences) {
            AugmentedSemanticGraph dependencies = sentence.get(EntityAnnotator.EntityAnnotations.class);

            Map<TaggedWord, Integer> candidateObjects = new HashMap<>();
            Map<TaggedWord, Role> candidateObjectRoles = new HashMap<>();
            Set<TaggedWord> processedObjects = new HashSet<>();
            //Map<Features.Role, Set<TaggedWord>> buckets = new HashMap<>();
            //for(Features.Role key : buckets.keySet()) buckets.put(key, new HashSet<>());

            int position = 0;

            List<TaggedWord> tokens = dependencies.orderedTokens();
            for (TaggedWord token : tokens) {
                List<String> features = FeatureVectors.getFeatures(token, position, dependencies, tokens);
                Role role = classifier.classOf(new BasicDatum<>(features));

                if (role == Role.DOBJECT || role == Role.IOBJECT) {
                    candidateObjects.put(token, id);
                    candidateObjectRoles.put(token, role);
                } else if (role == Role.ACTION) {
                    List<TaggedWord> dependentObjects = dependencies.outgoingEdgesOf(token)
                            .stream()
                            .filter(name -> name.getShortName().equals("dobj"))
                            .map(dependencies::getEdgeTarget)
                            .collect(Collectors.toList());

                    List<TaggedWord> indirectObjects = dependencies.outgoingEdgesOf(token)
                            .stream()
                            .filter(name -> name.getShortName().equals("iobj"))
                            .map(dependencies::getEdgeTarget)
                            .collect(Collectors.toList());

                    processedObjects.addAll(dependentObjects);
                    processedObjects.addAll(indirectObjects);

                    Action action = new Action(id, token, dependentObjects, indirectObjects);

                    indices.put(id, action);
                    flow.addVertex(action);

                    lastAction = action;
                    id++;
                }

                position++;
            }
            System.out.println();

            for (TaggedWord candidateObject : candidateObjects.keySet()) {
                if(!processedObjects.contains(candidateObject)) {
                    int index = candidateObjects.get(candidateObject);
                    if (indices.containsKey(index - 1) && indices.get(index - 1).sentIndex() == candidateObject.sentIndex())
                        indices.get(index - 1).addObject(candidateObject, candidateObjectRoles.get(candidateObject));
                    else if (indices.containsKey(index) && indices.get(index).sentIndex() == candidateObject.sentIndex())
                        indices.get(index).addObject(candidateObject, candidateObjectRoles.get(candidateObject));
                }
            }

        }

        for(Action action : indices.values()) {
            if(action.getDObjects().isEmpty()) {
                if(action.getIObjects().size() > 2) {
                    List<TaggedWord> iObjects = action.getIObjects();
                    while(iObjects.size() > 2) {
                        action.addObject(iObjects.remove(0), Role.DOBJECT);
                    }
                } else if(indices.containsKey(action.getID() - 1)) indices.get(action.getID() - 1).getDObjects().forEach(object -> action.addObject(object, Role.DOBJECT));
            }

            if(action.getIObjects().isEmpty() && !(action.description().equals("preheat") || action.description().equals("pre-heat") || action.description().equals("grease"))) {
                if(indices.containsKey(action.getID() - 1)) indices.get(action.getID() - 1).getDObjects().forEach(object -> action.addObject(object, Role.IOBJECT));
            }

            final List<TaggedWord> toRemove = new LinkedList<>();
            final List<TaggedWord> toAdd = new LinkedList<>();
            if(action.getDObjects().size() == 1) {
                for (TaggedWord word : action.getDObjects()) {
                    if (!word.isTypedEntity() || word.toString().contains("mixture")) {
                        if (indices.containsKey(action.getID() - 1)) {
                            indices.get(action.getID() - 1).getDObjects().stream().filter(TaggedWord::isTypedEntity).findFirst().ifPresent(newWord -> {
                                toAdd.add(newWord);
                                toRemove.add(word);
                            });
                        }
                    }
                }
            }

            for(TaggedWord word : toRemove) {
                action.remove(word, Role.DOBJECT);
            }
            for(TaggedWord word : toAdd) {
                action.addObject(word, Role.DOBJECT);
            }

            toRemove.clear();
            toAdd.clear();
            if(action.getIObjects().size() == 1) {
                for (TaggedWord word : action.getIObjects()) {
                    if (!word.isTypedEntity() || word.toString().contains("mixture")) {
                        if (indices.containsKey(action.getID() - 1)) {
                            indices.get(action.getID() - 1).getDObjects().stream().filter(TaggedWord::isTypedEntity).findFirst().ifPresent(newWord -> {
                                toAdd.add(newWord);
                                toRemove.add(word);
                            });
                        }
                    }
                }
            }
            for(TaggedWord word : toRemove) {
                action.remove(word, Role.IOBJECT);
            }
            for(TaggedWord word : toAdd) {
                action.addObject(word, Role.IOBJECT);
            }
        }

        for (Action action : indices.values()) {
            addDependencies(action, flow);
        }

        flow.vertexSet()
                .stream()
                .filter(action -> action.getID() != lastAction.getID() && flow.outDegreeOf(action) == 0)
                .forEach(action -> flow.addEdge(action, lastAction));

        return flow;
    }
}
