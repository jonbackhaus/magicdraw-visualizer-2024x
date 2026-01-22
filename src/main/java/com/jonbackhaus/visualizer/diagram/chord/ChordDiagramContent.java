package com.jonbackhaus.visualizer.diagram.chord;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.view.swing.BrowserView;
import com.nomagic.magicdraw.uml.RepresentationTextCreator;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Namespace;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Relationship;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.uml.diagrams.NonSymbolDiagramContent;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.jonbackhaus.visualizer.ui.DiagramConfigPanel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN;

/**
 * Content for the Chord Diagram.
 * Manages the UI component and its lifecycle within the diagram window.
 */
public class ChordDiagramContent implements NonSymbolDiagramContent<JComponent> {

    private final DiagramPresentationElement diagram;
    private DiagramConfigPanel configPanel;
    private JSplitPane splitPane;
    private Engine engine;
    private Browser browser;
    private BrowserView browserView;

    public ChordDiagramContent(DiagramPresentationElement diagram) {
        this.diagram = diagram;
        initBrowser();
    }

    private void initBrowser() {
        // Load JxBrowser license key from system properties or environment
        String key = System.getProperty("jxbrowser.license.key");
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException(
                "JxBrowser license key not configured. " +
                "Set -Djxbrowser.license.key or configure via the host app.");
        }

        engine = Engine.newInstance(
            EngineOptions.newBuilder(OFF_SCREEN)
            .licenseKey(key)
            .build()
            );
        browser = engine.newBrowser();
    }

    @Override
    public JComponent createComponent() {
        if (splitPane == null) {
            configPanel = new DiagramConfigPanel();
            browserView = BrowserView.newInstance(browser);

            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, configPanel, browserView);
            splitPane.setDividerLocation(300);

            configPanel.addRefreshListener(e -> refreshDiagram());

            loadHtml();
        }
        return splitPane;
    }

    private void refreshDiagram() {
        Namespace container = diagram.getDiagram().getOwner() instanceof Namespace
                ? (Namespace) diagram.getDiagram().getOwner()
                : null;
        if (container == null)
            return;

        String elementType = configPanel.getElementType();
        // TODO: Use criteria for relationship filtering via MetacrawlerService
        // String criteria = configPanel.getDependencyCriteria();

        // 1. Collect elements of the specified type in the container
        List<Element> elements = container.getOwnedElement().stream()
                .filter(e -> {
                    if ("Any".equals(elementType))
                        return true;
                    // Use getHumanType() from BaseElement
                    return ((BaseElement) e).getHumanType().contains(elementType);
                })
                .collect(Collectors.toList());

        if (elements.isEmpty())
            return;

        // 2. Map elements to indices
        int size = elements.size();
        List<String> names = elements.stream()
                .map((Element e) -> RepresentationTextCreator.getRepresentedText((BaseElement) e))
                .collect(Collectors.toList());

        // 3. Build Adjacency Matrix
        double[][] matrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            Element node = elements.get(i);
            Collection<Relationship> relationships = node.get_relationshipOfRelatedElement();
            for (Relationship rel : relationships) {
                Collection<Element> related = rel.getRelatedElement();
                for (Element target : related) {
                    if (target == node)
                        continue;
                    int j = elements.indexOf(target);
                    if (j != -1) {
                        matrix[i][j] += 1.0;
                    }
                }
            }
        }

        // 4. Send to Browser
        Gson gson = new Gson();
        JsonObject data = new JsonObject();
        JsonArray namesArray = new JsonArray();
        names.forEach(namesArray::add);
        data.add("names", namesArray);

        JsonArray matrixArray = new JsonArray();
        for (double[] row : matrix) {
            JsonArray rowArray = new JsonArray();
            for (double val : row)
                rowArray.add(val);
            matrixArray.add(rowArray);
        }
        data.add("matrix", matrixArray);

        String json = gson.toJson(data);
        browser.mainFrame().ifPresent(frame -> {
            frame.executeJavaScript("window.updateDiagram(" + json + ");");
        });
    }

    private void loadHtml() {
        URL url = getClass().getResource("/com/jonbackhaus/visualizer/chord_diagram.html");
        if (url != null) {
            browser.navigation().loadUrl(url.toString());
        }
    }

    @Override
    public void activate() {
        // Called when the diagram tab becomes active
    }

    @Override
    public void dispose() {
        // Cleanup resources
        if (browser != null) {
            browser.close();
        }
        if (engine != null) {
            engine.close();
        }
        configPanel = null;
        splitPane = null;
    }

    @Override
    public Dimension getComponentFullSize(JComponent component) {
        return component.getPreferredSize();
    }

    @Override
    public Rectangle getPaintableBounds(JComponent component) {
        return component.getBounds();
    }
}
