package com.jonbackhaus.visualizer.diagram.chord;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsObject;
import com.teamdev.jxbrowser.navigation.event.NavigationFinished;
import com.teamdev.jxbrowser.view.swing.BrowserView;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.magicdraw.ui.dialogs.SelectElementInfo;
import com.nomagic.magicdraw.ui.dialogs.SelectElementTypes;
import com.nomagic.magicdraw.ui.dialogs.selection.ElementSelectionDlg;
import com.nomagic.magicdraw.ui.dialogs.selection.ElementSelectionDlgFactory;
import com.nomagic.magicdraw.uml.RepresentationTextCreator;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Namespace;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Relationship;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DirectedRelationship;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Type;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdinterfaces.Interface;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN;

/**
 * Content for the Chord Diagram.
 * Manages the UI component and its lifecycle within the diagram window.
 */
public class ChordDiagramContent implements NonSymbolDiagramContent<JComponent> {

    private static final String LOG_PREFIX = "[Visualizer] ";
    private static final String SETTINGS_COMMENT_PREFIX = "CHORD_DIAGRAM_SETTINGS:";

    private final DiagramPresentationElement diagram;
    private DiagramConfigPanel configPanel;
    private JSplitPane splitPane;
    private Engine engine;
    private Browser browser;
    private BrowserView browserView;
    private volatile boolean htmlLoaded = false;
    private volatile String preparedHtml = null;

    // Store elements for navigation
    private List<Element> currentElements = new ArrayList<>();
    // Store relationships for navigation (key: "sourceIndex-targetIndex")
    private Map<String, List<Relationship>> currentRelationships = new HashMap<>();

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
            System.out.println(LOG_PREFIX + "HTML fully loaded, injecting bridges and triggering refresh");

            // Inject Java-to-JS bridges
            browser.mainFrame().ifPresent(frame -> {
                JsObject window = frame.executeJavaScript("window");
                if (window != null) {
                    window.putProperty("javaConsole", new JavaConsole());
                    window.putProperty("javaNavigation", new JavaNavigation(this));

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

    /**
     * Bridge class for element navigation from JavaScript.
     */
    public static class JavaNavigation {
        private final ChordDiagramContent content;

        public JavaNavigation(ChordDiagramContent content) {
            this.content = content;
        }

        @JsAccessible
        public void selectElement(int index) {
            System.out.println(LOG_PREFIX + "JavaScript requested navigation to element index: " + index);
            SwingUtilities.invokeLater(() -> content.navigateToElement(index));
        }

        @JsAccessible
        public void selectRelationship(int sourceIndex, int targetIndex) {
            System.out.println(LOG_PREFIX + "JavaScript requested navigation to relationship: " + sourceIndex + " -> " + targetIndex);
            SwingUtilities.invokeLater(() -> content.navigateToRelationship(sourceIndex, targetIndex));
        }
    }

    /**
     * Navigate to an element in the containment tree.
     */
    private void navigateToElement(int index) {
        if (index < 0 || index >= currentElements.size()) {
            System.out.println(LOG_PREFIX + "Invalid element index: " + index);
            return;
        }

        Element element = currentElements.get(index);
        String name = RepresentationTextCreator.getRepresentedText((BaseElement) element);
        System.out.println(LOG_PREFIX + "Navigating to element: " + name);

        Project project = Application.getInstance().getProject();
        if (project != null && project.getBrowser() != null) {
            project.getBrowser().getContainmentTree().openNode(element, true);
        }
    }

    /**
     * Navigate to a relationship in the containment tree.
     */
    private void navigateToRelationship(int sourceIndex, int targetIndex) {
        String key = sourceIndex + "-" + targetIndex;
        List<Relationship> rels = currentRelationships.get(key);

        if (rels == null || rels.isEmpty()) {
            // Try reverse direction
            key = targetIndex + "-" + sourceIndex;
            rels = currentRelationships.get(key);
        }

        if (rels == null || rels.isEmpty()) {
            System.out.println(LOG_PREFIX + "No relationships found for: " + sourceIndex + " <-> " + targetIndex);
            return;
        }

        // Navigate to the first relationship (could enhance to show a list if multiple)
        Relationship rel = rels.get(0);
        String name = RepresentationTextCreator.getRepresentedText((BaseElement) rel);
        System.out.println(LOG_PREFIX + "Navigating to relationship: " + name);

        Project project = Application.getInstance().getProject();
        if (project != null && project.getBrowser() != null) {
            project.getBrowser().getContainmentTree().openNode(rel, true);
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

            // Set default context
            Namespace defaultContext = diagram.getDiagram().getOwner() instanceof Namespace
                    ? (Namespace) diagram.getDiagram().getOwner()
                    : null;
            configPanel.setDefaultContext(defaultContext);

            // Set up context selection dialog
            configPanel.setContextSelectAction(e -> showContextSelectionDialog());

            // Load saved settings before creating the split pane
            loadSettings();

            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, configPanel, browserView);
            splitPane.setDividerLocation(280);

            configPanel.addRefreshListener(e -> {
                System.out.println(LOG_PREFIX + "Refresh button clicked");
                refreshDiagram();
            });

            loadHtml();
            System.out.println(LOG_PREFIX + "createComponent() completed");
        }
        return splitPane;
    }

    /**
     * Show dialog to select context element.
     */
    private void showContextSelectionDialog() {
        Project project = Application.getInstance().getProject();
        if (project == null) return;

        java.awt.Window parent = MDDialogParentProvider.getProvider().getDialogOwner();
        ElementSelectionDlg dlg = ElementSelectionDlgFactory.create(parent);

        // Set up element types filter (allow Packages and Namespaces)
        java.util.List<java.lang.Class<?>> includedTypes = new ArrayList<>();
        includedTypes.add(Package.class);
        includedTypes.add(Namespace.class);
        SelectElementTypes types = new SelectElementTypes(null, includedTypes, null, null);
        SelectElementInfo info = new SelectElementInfo(true, false, project.getPrimaryModel(), true);
        ElementSelectionDlgFactory.initSingle(dlg, types, info, project.getPrimaryModel());

        dlg.setVisible(true);

        if (dlg.isOkClicked()) {
            BaseElement selected = dlg.getSelectedElement();
            if (selected instanceof Namespace) {
                configPanel.setContextElement((Namespace) selected);
            }
        }
    }

    /**
     * Save current settings to a Comment element owned by the diagram.
     */
    private void saveSettings() {
        Project project = Application.getInstance().getProject();
        if (project == null || configPanel == null) return;

        try {
            // Build settings JSON
            JsonObject settings = new JsonObject();

            // Context element ID (if overridden)
            if (configPanel.isContextOverridden()) {
                Namespace ctx = configPanel.getContextElement();
                if (ctx != null) {
                    settings.addProperty("contextElementId", ctx.getID());
                }
            }

            settings.addProperty("recursive", configPanel.isRecursive());
            settings.addProperty("elementType", configPanel.getElementType());
            settings.addProperty("includeSubtypes", configPanel.isIncludeSubtypes());
            settings.addProperty("relationCriteria", configPanel.getRelationCriteria());
            settings.addProperty("showImplied", configPanel.isShowImplied());
            settings.addProperty("depth", configPanel.getDepth());
            settings.addProperty("showOrphans", configPanel.isShowOrphans());
            settings.addProperty("showLabels", configPanel.isShowLabels());
            settings.addProperty("showLegend", configPanel.isShowLegend());

            String json = new Gson().toJson(settings);
            System.out.println(LOG_PREFIX + "Saving settings: " + json);

            // Find or create the settings comment
            Element diagramElement = diagram.getDiagram();
            Comment settingsComment = findSettingsComment(diagramElement);

            SessionManager.getInstance().createSession(project, "Save Chord Diagram Settings");
            try {
                if (settingsComment == null) {
                    // Create new comment
                    settingsComment = project.getElementsFactory().createCommentInstance();
                    settingsComment.setOwner(diagramElement);
                }
                // Store with prefix to identify this as a settings comment
                settingsComment.setBody(SETTINGS_COMMENT_PREFIX + json);
                SessionManager.getInstance().closeSession(project);
                System.out.println(LOG_PREFIX + "Settings saved successfully");
            } catch (Exception e) {
                SessionManager.getInstance().cancelSession(project);
                System.out.println(LOG_PREFIX + "Error saving settings: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println(LOG_PREFIX + "Error in saveSettings: " + e.getMessage());
        }
    }

    /**
     * Load settings from the diagram's Comment element.
     */
    private void loadSettings() {
        if (configPanel == null) return;

        try {
            Element diagramElement = diagram.getDiagram();
            Comment settingsComment = findSettingsComment(diagramElement);

            if (settingsComment == null || settingsComment.getBody() == null) {
                System.out.println(LOG_PREFIX + "No saved settings found");
                return;
            }

            String body = settingsComment.getBody();
            // Strip the prefix to get the JSON
            String json = body.substring(SETTINGS_COMMENT_PREFIX.length());
            System.out.println(LOG_PREFIX + "Loading settings: " + json);

            JsonObject settings = new Gson().fromJson(json, JsonObject.class);

            // Restore context element
            if (settings.has("contextElementId")) {
                String contextId = settings.get("contextElementId").getAsString();
                Project project = Application.getInstance().getProject();
                if (project != null) {
                    BaseElement ctx = project.getElementByID(contextId);
                    if (ctx instanceof Namespace) {
                        configPanel.setContextElement((Namespace) ctx);
                    }
                }
            }

            // Restore other settings
            if (settings.has("recursive")) {
                configPanel.setRecursive(settings.get("recursive").getAsBoolean());
            }
            if (settings.has("elementType")) {
                configPanel.setElementType(settings.get("elementType").getAsString());
            }
            if (settings.has("includeSubtypes")) {
                configPanel.setIncludeSubtypes(settings.get("includeSubtypes").getAsBoolean());
            }
            if (settings.has("relationCriteria")) {
                configPanel.setRelationCriteria(settings.get("relationCriteria").getAsString());
            }
            if (settings.has("showImplied")) {
                configPanel.setShowImplied(settings.get("showImplied").getAsBoolean());
            }
            if (settings.has("depth")) {
                configPanel.setDepth(settings.get("depth").getAsInt());
            }
            if (settings.has("showOrphans")) {
                configPanel.setShowOrphans(settings.get("showOrphans").getAsBoolean());
            }
            if (settings.has("showLabels")) {
                configPanel.setShowLabels(settings.get("showLabels").getAsBoolean());
            }
            if (settings.has("showLegend")) {
                configPanel.setShowLegend(settings.get("showLegend").getAsBoolean());
            }

            System.out.println(LOG_PREFIX + "Settings loaded successfully");
        } catch (Exception e) {
            System.out.println(LOG_PREFIX + "Error loading settings: " + e.getMessage());
        }
    }

    /**
     * Find the settings Comment element owned by the diagram.
     */
    private Comment findSettingsComment(Element diagramElement) {
        if (diagramElement == null) return null;

        for (Object obj : diagramElement.getOwnedComment().toArray()) {
            if (obj instanceof Comment) {
                Comment c = (Comment) obj;
                String body = c.getBody();
                if (body != null && body.startsWith(SETTINGS_COMMENT_PREFIX)) {
                    return c;
                }
            }
        }
        return null;
    }

    private void refreshDiagram() {
        System.out.println(LOG_PREFIX + "refreshDiagram() called, htmlLoaded=" + htmlLoaded);

        // Guard: Skip if HTML not yet loaded (race condition fix)
        if (!htmlLoaded) {
            System.out.println(LOG_PREFIX + "Skipping refresh - HTML not yet loaded");
            return;
        }

        Namespace container = configPanel.getContextElement();
        if (container == null) {
            System.out.println(LOG_PREFIX + "Container is null, cannot refresh");
            showMessageInBrowser("No valid container found for this diagram.");
            return;
        }

        System.out.println(LOG_PREFIX + "Container: " + container.getName());

        String elementType = configPanel.getElementType();
        boolean includeSubtypes = configPanel.isIncludeSubtypes();
        boolean recursive = configPanel.isRecursive();
        boolean showLabels = configPanel.isShowLabels();
        boolean showLegend = configPanel.isShowLegend();
        boolean showOrphans = configPanel.isShowOrphans();
        String relationCriteria = configPanel.getRelationCriteria();

        System.out.println(LOG_PREFIX + "Element type filter: " + elementType +
            ", includeSubtypes: " + includeSubtypes + ", recursive: " + recursive +
            ", relationCriteria: " + relationCriteria + ", showOrphans: " + showOrphans);

        // 1. Collect elements of the specified type in the container
        List<Element> elements = collectElements(container, elementType, includeSubtypes, recursive);

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

        // 3. Build Adjacency Matrix and track relationships
        double[][] matrix = new double[size][size];
        currentRelationships.clear();
        int totalRelationships = 0;
        for (int i = 0; i < size; i++) {
            Element node = elements.get(i);
            // Snapshot collection to avoid ConcurrentModificationException
            Object[] relationships = node.get_relationshipOfRelatedElement().toArray();

            for (Object relObj : relationships) {
                if (!(relObj instanceof Relationship)) continue;
                Relationship rel = (Relationship) relObj;

                // Filter by relation criteria
                if (!matchesRelationCriteria(rel, relationCriteria)) continue;

                // Determine source and target based on relationship type
                Element source = null;
                Element target = null;

                if (rel instanceof Association) {
                    // For Associations, use memberEnds to determine direction
                    Association assoc = (Association) rel;
                    List<?> memberEnds = assoc.getMemberEnd();
                    if (memberEnds.size() >= 2) {
                        Property firstEnd = (Property) memberEnds.get(0);
                        Property secondEnd = (Property) memberEnds.get(1);
                        Type firstType = firstEnd.getType();
                        Type secondType = secondEnd.getType();
                        // Source is the type of the first memberEnd, target is the type of the second
                        if (firstType instanceof Element && secondType instanceof Element) {
                            source = (Element) firstType;
                            target = (Element) secondType;
                        }
                    }
                } else if (rel instanceof DirectedRelationship) {
                    // For directed relationships, use source/target collections
                    DirectedRelationship dirRel = (DirectedRelationship) rel;
                    Object[] sources = dirRel.getSource().toArray();
                    Object[] targets = dirRel.getTarget().toArray();
                    if (sources.length > 0 && targets.length > 0) {
                        source = (Element) sources[0];
                        target = (Element) targets[0];
                    }
                } else {
                    // For other relationships, use related elements (bidirectional)
                    Object[] relatedArray = rel.getRelatedElement().toArray();
                    for (Object targetObj : relatedArray) {
                        if (!(targetObj instanceof Element)) continue;
                        Element relatedEl = (Element) targetObj;
                        if (relatedEl == node) continue;
                        int j = elements.indexOf(relatedEl);
                        if (j != -1) {
                            matrix[i][j] += 1.0;
                            totalRelationships++;
                            String key = i + "-" + j;
                            currentRelationships.computeIfAbsent(key, k -> new ArrayList<>()).add(rel);
                        }
                    }
                    continue; // Already processed
                }

                // Only process if this node is the source (to avoid double-counting)
                if (source != null && target != null && source == node) {
                    int j = elements.indexOf(target);
                    if (j != -1) {
                        // Add in both directions for proper chord arc sizing
                        // The source->target direction determines ribbon coloring
                        matrix[i][j] += 1.0;  // Source -> Target
                        matrix[j][i] += 1.0;  // Target -> Source (for symmetric arc sizing)
                        totalRelationships++;

                        // Store relationship for navigation (source->target is primary)
                        String key = i + "-" + j;
                        currentRelationships.computeIfAbsent(key, k -> new ArrayList<>()).add(rel);
                        // Also store reverse for navigation from either end
                        String reverseKey = j + "-" + i;
                        currentRelationships.computeIfAbsent(reverseKey, k -> new ArrayList<>()).add(rel);
                    }
                }
            }
        }

        System.out.println(LOG_PREFIX + "Built adjacency matrix with " + totalRelationships + " relationships");

        // 3b. Filter out orphans if showOrphans is false
        if (!showOrphans) {
            List<Integer> connectedIndices = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                boolean hasConnection = false;
                for (int j = 0; j < size; j++) {
                    if (matrix[i][j] > 0 || matrix[j][i] > 0) {
                        hasConnection = true;
                        break;
                    }
                }
                if (hasConnection) {
                    connectedIndices.add(i);
                }
            }

            if (connectedIndices.size() < size) {
                System.out.println(LOG_PREFIX + "Filtering orphans: " + size + " -> " + connectedIndices.size() + " elements");

                // Rebuild filtered lists
                List<Element> filteredElements = new ArrayList<>();
                List<String> filteredNames = new ArrayList<>();
                for (int idx : connectedIndices) {
                    filteredElements.add(elements.get(idx));
                    filteredNames.add(names.get(idx));
                }

                // Rebuild matrix with new indices
                int newSize = connectedIndices.size();
                double[][] newMatrix = new double[newSize][newSize];
                Map<String, List<Relationship>> newRelationships = new HashMap<>();

                for (int newI = 0; newI < newSize; newI++) {
                    int oldI = connectedIndices.get(newI);
                    for (int newJ = 0; newJ < newSize; newJ++) {
                        int oldJ = connectedIndices.get(newJ);
                        newMatrix[newI][newJ] = matrix[oldI][oldJ];
                        String oldKey = oldI + "-" + oldJ;
                        if (currentRelationships.containsKey(oldKey)) {
                            String newKey = newI + "-" + newJ;
                            newRelationships.put(newKey, currentRelationships.get(oldKey));
                        }
                    }
                }

                // Replace with filtered data
                elements = filteredElements;
                names = filteredNames;
                matrix = newMatrix;
                size = newSize;
                currentRelationships = newRelationships;
            }
        }

        currentElements = elements; // Update stored elements (may have been filtered)

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

        // Add display options
        JsonObject options = new JsonObject();
        options.addProperty("showLabels", showLabels);
        options.addProperty("showLegend", showLegend);
        data.add("options", options);

        String json = gson.toJson(data);
        System.out.println(LOG_PREFIX + "Sending JSON to browser, length=" + json.length());

        browser.mainFrame().ifPresentOrElse(
            frame -> {
                System.out.println(LOG_PREFIX + "Executing JavaScript: window.updateDiagram(...)");
                frame.executeJavaScript("window.updateDiagram(" + json + ");");
            },
            () -> System.out.println(LOG_PREFIX + "WARNING: Main frame not available!")
        );

        // Save settings after successful refresh
        saveSettings();
    }

    /**
     * Collect elements from the container, optionally recursively.
     */
    private List<Element> collectElements(Namespace container, String elementType,
            boolean includeSubtypes, boolean recursive) {
        List<Element> result = new ArrayList<>();
        collectElementsRecursive(container, elementType, includeSubtypes, recursive, result);
        return result;
    }

    private void collectElementsRecursive(Namespace container, String elementType,
            boolean includeSubtypes, boolean recursive, List<Element> result) {
        // Snapshot collection to avoid ConcurrentModificationException
        Object[] ownedElements = container.getOwnedElement().toArray();

        for (Object obj : ownedElements) {
            if (!(obj instanceof Element)) continue;
            Element e = (Element) obj;

            if (matchesElementType(e, elementType, includeSubtypes)) {
                result.add(e);
            }

            // Recurse into nested namespaces
            if (recursive && e instanceof Namespace) {
                collectElementsRecursive((Namespace) e, elementType, includeSubtypes, true, result);
            }
        }
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
                    return e instanceof Component;
                default:
                    // For other types, fall back to humanType contains check
                    return humanType.contains(elementType);
            }
        }

        return false;
    }

    /**
     * Check if a relationship matches the specified relation criteria filter.
     * Uses humanType for matching to handle stereotyped relationships correctly.
     */
    private boolean matchesRelationCriteria(Relationship rel, String criteria) {
        if ("Any".equals(criteria)) {
            return true;
        }

        String humanType = ((BaseElement) rel).getHumanType();

        switch (criteria) {
            case "Dependency":
                // Match Dependency but not its subtypes like Usage or Realization
                return "Dependency".equals(humanType);
            case "Association":
                // Match Association and its variations
                return humanType.contains("Association");
            case "Generalization":
                return "Generalization".equals(humanType);
            case "Realization":
                // Match Interface Realization, Realization, etc.
                return humanType.contains("Realization");
            case "Usage":
                return "Usage".equals(humanType);
            default:
                // Unknown criteria, show all
                return true;
        }
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
        currentElements = null;
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
