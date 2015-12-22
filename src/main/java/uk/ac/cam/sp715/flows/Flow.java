package uk.ac.cam.sp715.flows;

import org.jgrapht.ext.ComponentAttributeProvider;
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
 * Created by Srijan on 19/11/2015.
 */
public class Flow extends DefaultDirectedGraph<Action, DefaultEdge> {
    private static final String GRHOME = System.getenv("GRHOME");
    private static final Logger logger = Logging.getLogger(Flow.class.getName());
    public Flow() {
        super(DefaultEdge.class);
    }
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
    private void writeDOT(Writer writer) throws IOException {
        DOTExporter<Action, DefaultEdge> exporter = new DOTExporter<>(Action::id, Action::toString, defaultEdge -> "", action -> {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("shape", "box");
            return attributes;
        }, defaultEdge -> new HashMap<>());
        exporter.export(writer, this);
        writer.flush();
    }
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
