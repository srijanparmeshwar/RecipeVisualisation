package uk.ac.cam.sp715.flows;

import org.jgrapht.ext.DOTExporter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import uk.ac.cam.sp715.util.Logging;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Represents the flow chart for a recipe. Nodes are actions and edges are dependencies.
 * @author Srijan Parmeshwar <sp715@cam.ac.uk>
 */
public class Flow extends DefaultDirectedGraph<Action, DefaultEdge> {
    private static final String GRHOME = System.getenv("GRHOME");
    private static final Logger logger = Logging.getLogger(Flow.class);

    public Flow() {
        super(DefaultEdge.class);
    }

    /**
     * Converts the graph to a string in DOT format.
     * @return DOT format of graph as a string.
     */
    public String toDOT() {
        DOTExporter<Action, DefaultEdge> exporter = new DOTExporter<>(Action::id, Action::toString, defaultEdge -> "");
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
        DOTExporter<Action, DefaultEdge> exporter = new DOTExporter<>(Action::id, Action::toString, defaultEdge -> "", action -> {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("shape", "box");
            return attributes;
        }, defaultEdge -> new HashMap<>());
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
}
