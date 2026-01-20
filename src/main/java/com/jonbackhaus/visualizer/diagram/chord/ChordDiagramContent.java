package com.jonbackhaus.visualizer.diagram.chord;

import com.nomagic.magicdraw.uml.diagrams.NonSymbolDiagramContent;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.jonbackhaus.visualizer.ui.DiagramConfigPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Content for the Chord Diagram.
 * Manages the UI component and its lifecycle within the diagram window.
 */
public class ChordDiagramContent implements NonSymbolDiagramContent<JComponent> {

    private final DiagramPresentationElement diagram;
    private DiagramConfigPanel configPanel;

    public ChordDiagramContent(DiagramPresentationElement diagram) {
        this.diagram = diagram;
    }

    @Override
    public JComponent createComponent() {
        if (configPanel == null) {
            configPanel = new DiagramConfigPanel();
        }
        return configPanel;
    }

    @Override
    public void activate() {
        // Called when the diagram tab becomes active
    }

    @Override
    public void dispose() {
        // Cleanup resources
        configPanel = null;
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
