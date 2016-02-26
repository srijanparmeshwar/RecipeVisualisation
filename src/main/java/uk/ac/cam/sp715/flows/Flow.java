package uk.ac.cam.sp715.flows;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.process.SerializableFunction;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import uk.ac.cam.sp715.recognition.TaggedWord;
import uk.ac.cam.sp715.util.Logging;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Represents the flow chart for a recipe. Nodes are actions and edges are dependencies.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class Flow extends DefaultDirectedGraph<Action, DefaultEdge> implements Serializable {
    private static final String GRHOME = System.getenv("GRHOME");
    private static final Logger logger = Logging.getLogger(Flow.class);
    private static final DOTExporter<Action, DefaultEdge> exporter = new DOTExporter<>(Action::id, Action::toString, defaultEdge -> "", action -> new HashMap<>(), defaultEdge -> new HashMap<>());

    public Flow() {
        super(DefaultEdge.class);
    }

    /**
     * Converts the graph to a string in DOT format.
     * @return DOT format of graph as a string.
     */
    public String toDOT() {
        StringWriter writer = new StringWriter();
        exporter.export(writer, this);
        writer.flush();
        String value = writer.toString();
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return value;
    }

    /**
     * Writes the graph to the given writer in DOT format.
     * @param writer The writer which will be used when exporting the graph.
     * @throws IOException Thrown when the graph cannot be written to the writer.
     */
    private void writeDOT(Writer writer) throws IOException {
        exporter.export(writer, this);
        writer.flush();
    }

    /**
     * Converts the graph to SVG format as a string using Graphviz.
     * The function uses a system call to do the conversion from DOT format to SVG.
     * @return String representation of SVG graph.
     * @throws IOException Thrown when either errors occur with the system call, or
     * if there is an error with converting the graph to DOT format.
     */
    public String toSVG() throws IOException {
        try {
            Process process = Runtime.getRuntime().exec(Paths.get(GRHOME, "dot -Tsvg").toString());

            try(OutputStream stdin = process.getOutputStream();
                OutputStreamWriter bufferedOutputStream = new OutputStreamWriter(stdin);
                BufferedWriter writer = new BufferedWriter(bufferedOutputStream)) {
                writeDOT(writer);
            }

            try(InputStream stdout = process.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(stdout);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                return bufferedReader
                        .lines()
                        .collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not convert graph to SVG stream.", e);
            throw new IOException();
        }
    }

    public boolean pathExists(Action source, Action target) {
        if(this.containsEdge(source, target)) return true;
        else {
            for (Action action : this.outgoingEdgesOf(source)
                    .stream()
                    .map(this::getEdgeTarget)
                    .collect(Collectors.toSet())) {
                if(pathExists(action, target)) return true;
            }
        }
        return false;
    }

    @Override
    public DefaultEdge addEdge(Action source, Action target) {
        this.incomingEdgesOf(target)
                .stream()
                .map(this::getEdgeSource)
                .collect(Collectors.toSet())
                .stream()
                .filter(action -> pathExists(action, source))
                .forEach(action -> this.removeEdge(action, target));
        if(!pathExists(source, target)) return super.addEdge(source, target);
        else return null;
    }

    private void removeSingletons() {
        Set<Action> singletons = this
                .vertexSet()
                .stream()
                .filter(action -> outDegreeOf(action) + inDegreeOf(action) == 0)
                .collect(Collectors.toSet());
        for(Action removable : singletons) this.removeVertex(removable);
    }

    public Set<Action> getLeaves() {
        return vertexSet()
                .stream()
                .filter(action -> outDegreeOf(action) == 0)
                .collect(Collectors.toSet());
    }

    public void mergeFlows(Flow other) {
        Set<Action> endNodes = getLeaves();

        for(Action endNode : endNodes) {
            other.outgoingEdgesOf(endNode)
                    .stream()
                    .map(other::getEdgeTarget)
                    .forEach(target -> this.addEdge(endNode, target));
        }

        endNodes = getLeaves();

        for(Action endNode : endNodes) {
            vertexSet().stream().filter(node -> node.getID() > endNode.getID()).filter(node -> {
                for(TaggedWord srcWord : endNode.getObjects()) {
                    for(IndexedWord srcIndexedWord : srcWord.getTokens()) {
                        for(TaggedWord dstWord : node.getObjects()) {
                            for(IndexedWord dstIndexedWord : dstWord.getTokens()) {
                                if(srcIndexedWord.lemma().equals(dstIndexedWord.lemma())) return true;
                            }
                        }
                    }
                }
                return false;
            }).forEach(target -> this.addEdge(endNode, target));
        }

        /*Set<Action> endNodes = this
                .vertexSet()
                .stream()
                .filter(action -> outDegreeOf(action) == 0 && inDegreeOf(action) > 0)
                .collect(Collectors.toSet());

        int count = 0;
        while(endNodes.size() > 1 && count < 2) {

            for (Action endNode : endNodes) {
                Set<String> words = new HashSet<>();
                this.incomingEdgesOf(endNode)
                        .stream()
                        .map(this::getEdgeSource)
                        .map(Action::getObjects)
                        .forEach(list -> {
                            for(TaggedWord word : list) {
                                for(IndexedWord indexedWord : word.getTokens()) words.add(indexedWord.lemma());
                            }
                        });


                SortedMap<Integer, Action> sortedMap = new TreeMap<>();
                this.vertexSet()
                        .stream()
                        .filter(vertex -> endNode.getID() < vertex.getID())
                        .forEach(vertex -> sortedMap.put(vertex.getID(), vertex));

                for(Action node : sortedMap.values()) {
                    boolean check = true;
                    for(TaggedWord word : node.getObjects()) {
                        for(IndexedWord indexedWord :  word.getTokens()) {
                            if(words.contains(indexedWord.lemma())) {
                                this.addEdge(endNode, node);
                                check = false;
                                break;
                            }
                        }
                    }
                    if(check && other.containsEdge(endNode, node)) this.addEdge(endNode, node);
                }
            }

            removeSingletons();

            endNodes = this
                    .vertexSet()
                    .stream()
                    .filter(action -> outDegreeOf(action) == 0 && inDegreeOf(action) > 0)
                    .collect(Collectors.toSet());
            count++;
        }*/
    }

    private List<Action> nextLevel(List<Action> actions) {
        List<Action> newActions = new LinkedList<>();
        for(Action action : actions) {
            outgoingEdgesOf(action)
                    .stream()
                    .map(this::getEdgeTarget)
                    .forEach(newActions::add);
        }
        return newActions;
    }

    /*
    private static boolean match2(Action src, Action dst) {
        for(TaggedWord word : src.getObjects()) {
            for(IndexedWord iword : word.getTokens()) {
                String srcLemma = iword.lemma();
                for(TaggedWord dstWord : dst.getObjects()) {
                    for(IndexedWord dstiword : dstWord.getTokens()) if(srcLemma.equals(dstiword.lemma())) return true;
                }
            }
        }
        return false;
    }

    private static boolean match(Action src, Action dst) {
        for(TaggedWord word : src.getObjects()) {
            for(TaggedWord dstWord : dst.getObjects()) {
                if(word.toString().equalsIgnoreCase(dstWord.toString())) return true;
            }
        }
        return false;
    }

    private void m1(Flow other) {
        Set<Action> endNodes = this
                .vertexSet()
                .stream()
                .filter(action -> outDegreeOf(action) == 0)
                .collect(Collectors.toSet());

        for (Action endNode : endNodes) {
            List<Action> levelActions = other.nextLevel(Collections.singletonList(endNode));
            while(levelActions.size() > 0) {
                List<Action> inputActions = new LinkedList<>();
                for(Action target : levelActions) {
                    if(this.containsVertex(target) && match(endNode, target)) this.addEdge(endNode, target);
                    else inputActions.add(target);
                }
                levelActions = new LinkedList<>();//other.nextLevel(inputActions);
            }
        }
    }

    private void m2(Flow other) {
        Set<Action> endNodes = this
                .vertexSet()
                .stream()
                .filter(action -> outDegreeOf(action) == 0)
                .collect(Collectors.toSet());

        for (Action endNode : endNodes) {
            List<Action> levelActions = other.nextLevel(Collections.singletonList(endNode));
            while(levelActions.size() > 0) {
                List<Action> inputActions = new LinkedList<>();
                for(Action target : levelActions) {
                    if(this.containsVertex(target) && match2(endNode, target)) this.addEdge(endNode, target);
                    else inputActions.add(target);
                }
                levelActions = new LinkedList<>();//other.nextLevel(inputActions);
            }
        }
    }

    public void mergeFlows(Flow other) {
        m1(other);
        removeSingletons();
        m2(other);
    }*/
}
