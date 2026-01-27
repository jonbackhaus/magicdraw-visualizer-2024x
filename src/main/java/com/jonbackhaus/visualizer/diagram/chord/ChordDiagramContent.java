package com.jonbackhaus.visualizer.diagram.chord;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsObject;
import com.teamdev.jxbrowser.navigation.event.NavigationFinished;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN;

/**
 * Content for the Chord Diagram.
 * Manages the UI component and its lifecycle within the diagram window.
 */
public class ChordDiagramContent implements NonSymbolDiagramContent<JComponent> {

    private static final String LOG_PREFIX = "[Visualizer] ";

    private final DiagramPresentationElement diagram;
    private DiagramConfigPanel configPanel;
    private JSplitPane splitPane;
    private Engine engine;
    private Browser browser;
    private BrowserView browserView;
    private volatile boolean htmlLoaded = false;

    public ChordDiagramContent(DiagramPresentationElement diagram) {
        System.out.println(LOG_PREFIX + "ChordDiagramContent constructor called");
        this.diagram = diagram;
        initBrowser();
        System.out.println(LOG_PREFIX + "ChordDiagramContent constructor completed");
    }

    private void initBrowser() {
        System.out.println(LOG_PREFIX + "initBrowser() called");
        String key = loadLicenseKey();
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException(
                "JxBrowser license key not configured. " +
                "Add jxbrowser.properties with license.key to the resources folder.");
        }

        engine = Engine.newInstance(
            EngineOptions.newBuilder(OFF_SCREEN)
            .licenseKey(key)
            .build()
            );
        browser = engine.newBrowser();

        // Listen for navigation completion to know when HTML is fully loaded
        browser.navigation().on(NavigationFinished.class, event -> {
            System.out.println(LOG_PREFIX + "NavigationFinished event received, URL: " + event.url());
            htmlLoaded = true;
            System.out.println(LOG_PREFIX + "HTML fully loaded, triggering initial refresh");

            // Inject Java-to-JS bridge for console message capture
            browser.mainFrame().ifPresent(frame -> {
                JsObject window = frame.executeJavaScript("window");
                if (window != null) {
                    window.putProperty("javaConsole", new JavaConsole());
                    // Override console methods to forward to Java
                    frame.executeJavaScript(
                        "var origLog = console.log; console.log = function() { " +
                        "  var msg = Array.prototype.slice.call(arguments).join(' '); " +
                        "  window.javaConsole.log(msg); origLog.apply(console, arguments); }; " +
                        "var origError = console.error; console.error = function() { " +
                        "  var msg = Array.prototype.slice.call(arguments).join(' '); " +
                        "  window.javaConsole.error(msg); origError.apply(console, arguments); };"
                    );
                }
            });

            SwingUtilities.invokeLater(this::refreshDiagram);
        });

        System.out.println(LOG_PREFIX + "initBrowser() completed");
    }

    /**
     * Bridge class for capturing JavaScript console messages in Java.
     */
    public static class JavaConsole {
        @JsAccessible
        public void log(String message) {
            System.out.println("[Visualizer-JS] LOG: " + message);
        }

        @JsAccessible
        public void error(String message) {
            System.out.println("[Visualizer-JS] ERROR: " + message);
        }
    }

    private String loadLicenseKey() {
        // First try system property (allows override)
        String key = System.getProperty("jxbrowser.license.key");
        if (key != null && !key.isEmpty()) {
            return key;
        }

        // Then try bundled properties file
        try (InputStream is = getClass().getResourceAsStream("/jxbrowser.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("license.key");
            }
        } catch (IOException e) {
            // Fall through to return null
        }
        return null;
    }

    @Override
    public JComponent createComponent() {
        System.out.println(LOG_PREFIX + "createComponent() called");
        if (splitPane == null) {
            System.out.println(LOG_PREFIX + "Creating new UI components");
            configPanel = new DiagramConfigPanel();
            browserView = BrowserView.newInstance(browser);

            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, configPanel, browserView);
            splitPane.setDividerLocation(300);

            configPanel.addRefreshListener(e -> {
                System.out.println(LOG_PREFIX + "Refresh button clicked");
                refreshDiagram();
            });

            loadHtml();
            System.out.println(LOG_PREFIX + "createComponent() completed");
        }
        return splitPane;
    }

    private void refreshDiagram() {
        System.out.println(LOG_PREFIX + "refreshDiagram() called, htmlLoaded=" + htmlLoaded);

        // Guard: Skip if HTML not yet loaded (race condition fix)
        if (!htmlLoaded) {
            System.out.println(LOG_PREFIX + "Skipping refresh - HTML not yet loaded");
            return;
        }

        Namespace container = diagram.getDiagram().getOwner() instanceof Namespace
                ? (Namespace) diagram.getDiagram().getOwner()
                : null;
        if (container == null) {
            System.out.println(LOG_PREFIX + "Container is null, cannot refresh");
            showMessageInBrowser("No valid container found for this diagram.");
            return;
        }

        System.out.println(LOG_PREFIX + "Container: " + container.getName());

        String elementType = configPanel.getElementType();
        System.out.println(LOG_PREFIX + "Element type filter: " + elementType);
        // TODO: Use criteria for relationship filtering via MetacrawlerService
        // String criteria = configPanel.getDependencyCriteria();

        // 1. Collect elements of the specified type in the container
        // Snapshot collection to avoid ConcurrentModificationException
        Object[] ownedElements = container.getOwnedElement().toArray();
        List<Element> elements = java.util.Arrays.stream(ownedElements)
                .filter(e -> e instanceof Element)
                .map(e -> (Element) e)
                .filter(e -> {
                    if ("Any".equals(elementType))
                        return true;
                    // Use getHumanType() from BaseElement
                    return ((BaseElement) e).getHumanType().contains(elementType);
                })
                .collect(Collectors.toList());

        System.out.println(LOG_PREFIX + "Found " + elements.size() + " elements matching filter");

        if (elements.isEmpty()) {
            System.out.println(LOG_PREFIX + "No elements found, showing message");
            showMessageInBrowser("No elements of type '" + elementType + "' found in container '" + container.getName() + "'.");
            return;
        }

        // 2. Map elements to indices
        int size = elements.size();
        List<String> names = elements.stream()
                .map((Element e) -> RepresentationTextCreator.getRepresentedText((BaseElement) e))
                .collect(Collectors.toList());

        System.out.println(LOG_PREFIX + "Element names: " + names);

        // 3. Build Adjacency Matrix
        double[][] matrix = new double[size][size];
        int totalRelationships = 0;
        for (int i = 0; i < size; i++) {
            Element node = elements.get(i);
            // Snapshot collection to avoid ConcurrentModificationException
            Object[] relationships = node.get_relationshipOfRelatedElement().toArray();
            for (Object relObj : relationships) {
                if (!(relObj instanceof Relationship)) continue;
                Relationship rel = (Relationship) relObj;
                // Snapshot related elements as well
                Object[] relatedArray = rel.getRelatedElement().toArray();
                for (Object targetObj : relatedArray) {
                    if (!(targetObj instanceof Element)) continue;
                    Element target = (Element) targetObj;
                    if (target == node)
                        continue;
                    int j = elements.indexOf(target);
                    if (j != -1) {
                        matrix[i][j] += 1.0;
                        totalRelationships++;
                    }
                }
            }
        }

        System.out.println(LOG_PREFIX + "Built adjacency matrix with " + totalRelationships + " relationships");

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
        System.out.println(LOG_PREFIX + "Sending JSON to browser, length=" + json.length());

        browser.mainFrame().ifPresentOrElse(
            frame -> {
                System.out.println(LOG_PREFIX + "Executing JavaScript: window.updateDiagram(...)");
                frame.executeJavaScript("window.updateDiagram(" + json + ");");
            },
            () -> System.out.println(LOG_PREFIX + "WARNING: Main frame not available!")
        );
    }

    private void showMessageInBrowser(String message) {
        browser.mainFrame().ifPresentOrElse(
            frame -> {
                String escapedMessage = message.replace("'", "\\'").replace("\n", "\\n");
                frame.executeJavaScript(
                    "document.getElementById('chart').innerHTML = '<div style=\"padding: 20px; color: #666; font-size: 14px;\">" + escapedMessage + "</div>';"
                );
            },
            () -> System.out.println(LOG_PREFIX + "WARNING: Cannot show message, main frame not available")
        );
    }

    private void loadHtml() {
        System.out.println(LOG_PREFIX + "loadHtml() called");
        URL url = getClass().getResource("/com/jonbackhaus/visualizer/chord_diagram.html");
        if (url != null) {
            System.out.println(LOG_PREFIX + "Loading HTML from: " + url.toString());
            browser.navigation().loadUrl(url.toString());
        } else {
            System.out.println(LOG_PREFIX + "ERROR: chord_diagram.html resource not found!");
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
