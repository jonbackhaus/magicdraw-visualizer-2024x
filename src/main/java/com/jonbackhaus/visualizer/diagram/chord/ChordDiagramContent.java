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
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdinterfaces.Interface;
import com.nomagic.uml2.ext.magicdraw.components.mdbasiccomponents.Component;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.uml.diagrams.NonSymbolDiagramContent;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.jonbackhaus.visualizer.ui.DiagramConfigPanel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    private volatile String preparedHtml = null;

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
            String url = event.url();
            // Truncate data URLs to avoid logging entire HTML content
            String logUrl = url.startsWith("data:") ? "data:text/html... (" + url.length() + " chars)" : url;
            System.out.println(LOG_PREFIX + "NavigationFinished event received, URL: " + logUrl);

            // Check if this is just the about:blank page (initial frame setup)
            if ("about:blank".equals(url)) {
                System.out.println(LOG_PREFIX + "about:blank loaded, now loading prepared HTML");
                // Now that frame is ready, load our prepared HTML
                if (preparedHtml != null) {
                    browser.mainFrame().ifPresent(frame -> {
                        System.out.println(LOG_PREFIX + "Loading prepared HTML into frame");
                        frame.loadHtml(preparedHtml);
                    });
                }
                return;
            }

            // This is the actual content loaded
            htmlLoaded = true;
            System.out.println(LOG_PREFIX + "HTML fully loaded, injecting console bridge and triggering refresh");

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

                // Diagnostic: Check if D3 and updateDiagram are available
                Object d3Check = frame.executeJavaScript("typeof d3");
                Object fnCheck = frame.executeJavaScript("typeof window.updateDiagram");
                System.out.println(LOG_PREFIX + "Diagnostic - d3 type: " + d3Check + ", updateDiagram type: " + fnCheck);
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
        boolean includeSubtypes = configPanel.isIncludeSubtypes();
        System.out.println(LOG_PREFIX + "Element type filter: " + elementType + ", includeSubtypes: " + includeSubtypes);

        // 1. Collect elements of the specified type in the container
        // Snapshot collection to avoid ConcurrentModificationException
        Object[] ownedElements = container.getOwnedElement().toArray();
        List<Element> elements = java.util.Arrays.stream(ownedElements)
                .filter(e -> e instanceof Element)
                .map(e -> (Element) e)
                .filter(e -> matchesElementType(e, elementType, includeSubtypes))
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

            // Diagnostic logging for relationship discovery
            System.out.println(LOG_PREFIX + "Element '" + names.get(i) + "' has " +
                relationships.length + " relationships via get_relationshipOfRelatedElement()");

            int elementRelCount = 0;
            for (Object relObj : relationships) {
                if (!(relObj instanceof Relationship)) {
                    System.out.println(LOG_PREFIX + "  - Skipping non-Relationship object: " +
                        (relObj != null ? relObj.getClass().getSimpleName() : "null"));
                    continue;
                }
                Relationship rel = (Relationship) relObj;
                String relType = ((BaseElement) rel).getHumanType();
                // Snapshot related elements as well
                Object[] relatedArray = rel.getRelatedElement().toArray();
                System.out.println(LOG_PREFIX + "  - Relationship type: " + relType +
                    ", related elements: " + relatedArray.length);

                for (Object targetObj : relatedArray) {
                    if (!(targetObj instanceof Element)) continue;
                    Element target = (Element) targetObj;
                    if (target == node)
                        continue;
                    int j = elements.indexOf(target);
                    if (j != -1) {
                        matrix[i][j] += 1.0;
                        totalRelationships++;
                        elementRelCount++;
                        System.out.println(LOG_PREFIX + "    -> Found connection to '" + names.get(j) + "'");
                    } else {
                        // Target exists but is not in our filtered element list
                        String targetName = RepresentationTextCreator.getRepresentedText((BaseElement) target);
                        System.out.println(LOG_PREFIX + "    -> Target '" + targetName +
                            "' not in filtered elements (not counted)");
                    }
                }
            }
            if (elementRelCount == 0 && relationships.length > 0) {
                System.out.println(LOG_PREFIX + "  (no connections to other elements in the filtered set)");
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

    /**
     * Check if an element matches the specified element type filter.
     * When includeSubtypes is true, also matches stereotyped subtypes
     * (e.g., SysML Blocks are stereotyped Classes).
     */
    private boolean matchesElementType(Element e, String elementType, boolean includeSubtypes) {
        if ("Any".equals(elementType)) {
            return true;
        }

        String humanType = ((BaseElement) e).getHumanType();

        // Exact match on humanType
        if (humanType.equals(elementType)) {
            return true;
        }

        // If includeSubtypes, check metaclass hierarchy
        if (includeSubtypes) {
            switch (elementType) {
                case "Class":
                    // SysML Blocks, ConstraintBlocks, etc. are stereotyped Classes
                    return e instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
                case "Package":
                    // Profiles, Models are stereotyped Packages
                    return e instanceof Package;
                case "Interface":
                    return e instanceof Interface;
                case "Component":
                    return e instanceof com.nomagic.uml2.ext.magicdraw.components.mdbasiccomponents.Component;
                default:
                    // For other types, fall back to humanType contains check
                    return humanType.contains(elementType);
            }
        }

        return false;
    }

    private void loadHtml() {
        System.out.println(LOG_PREFIX + "loadHtml() called");
        try {
            // Read all resources as strings
            String html = readResource("/com/jonbackhaus/visualizer/chord_diagram.html");
            String d3js = readResource("/com/jonbackhaus/visualizer/d3.v7.min.js");
            String chordRenderJs = readResource("/com/jonbackhaus/visualizer/chord_render.js");

            if (html == null) {
                System.out.println(LOG_PREFIX + "ERROR: chord_diagram.html resource not found!");
                return;
            }
            if (d3js == null) {
                System.out.println(LOG_PREFIX + "ERROR: d3.v7.min.js resource not found!");
                return;
            }
            if (chordRenderJs == null) {
                System.out.println(LOG_PREFIX + "ERROR: chord_render.js resource not found!");
                return;
            }

            System.out.println(LOG_PREFIX + "Resources loaded - HTML: " + html.length() +
                " bytes, D3: " + d3js.length() + " bytes, ChordRender: " + chordRenderJs.length() + " bytes");

            // Replace placeholders with inline scripts
            String d3Script = "<script>\n" + d3js + "\n</script>";
            String chordRenderScript = "<script>\n" + chordRenderJs + "\n</script>";

            html = html.replace("<!-- D3_SCRIPT_PLACEHOLDER -->", d3Script);
            html = html.replace("<!-- CHORD_RENDER_SCRIPT_PLACEHOLDER -->", chordRenderScript);

            System.out.println(LOG_PREFIX + "Final HTML size: " + html.length() + " bytes");

            // Store the prepared HTML
            this.preparedHtml = html;

            // Load the HTML content directly (not via URL)
            browser.mainFrame().ifPresentOrElse(
                frame -> {
                    System.out.println(LOG_PREFIX + "Loading HTML content into browser frame");
                    frame.loadHtml(preparedHtml);
                },
                () -> {
                    // If main frame not available yet, load about:blank first to create the frame
                    // The NavigationFinished handler will then load our prepared HTML
                    System.out.println(LOG_PREFIX + "Main frame not available, loading about:blank first");
                    browser.navigation().loadUrl("about:blank");
                }
            );
        } catch (Exception e) {
            System.out.println(LOG_PREFIX + "ERROR loading HTML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String readResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            System.out.println(LOG_PREFIX + "ERROR reading resource " + path + ": " + e.getMessage());
            return null;
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
