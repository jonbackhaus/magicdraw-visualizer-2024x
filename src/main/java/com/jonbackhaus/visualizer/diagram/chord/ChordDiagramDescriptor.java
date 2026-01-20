package com.jonbackhaus.visualizer.diagram.chord;

import com.nomagic.actions.AMConfigurator;
import com.nomagic.magicdraw.uml.diagrams.NonSymbolDiagramContent;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.ui.ResizableIcon;
import com.jonbackhaus.visualizer.diagram.VisualizerDiagramDescriptor;

import javax.swing.*;
import java.net.URL;

/**
 * Descriptor for the Chord Diagram.
 */
public class ChordDiagramDescriptor extends VisualizerDiagramDescriptor {

    public static final String DIAGRAM_TYPE = "Chord Diagram";
    public static final String DIAGRAM_ID = "CHORD_DIAGRAM";

    public ChordDiagramDescriptor() {
        super();
    }

    @Override
    public String getDiagramTypeId() {
        return DIAGRAM_ID;
    }

    @Override
    public String getCategory() {
        return "Visualizer Diagrams";
    }

    @Override
    public String getSingularDiagramTypeHumanName() {
        return DIAGRAM_TYPE;
    }

    @Override
    public String getPluralDiagramTypeHumanName() {
        return DIAGRAM_TYPE + "s";
    }

    @Override
    public boolean isCreatable() {
        return true;
    }

    @Override
    public ResizableIcon getSVGIcon() {
        return null;
    }

    @Override
    public URL getSmallIconURL() {
        return null;
    }

    @Override
    public AMConfigurator getDiagramShortcutsConfigurator() {
        return null;
    }

    @Override
    public AMConfigurator getDiagramCommandBarConfigurator() {
        return null;
    }

    @Override
    public NonSymbolDiagramContent<JComponent> createDiagramContent(DiagramPresentationElement diagram) {
        return new ChordDiagramContent(diagram);
    }
}
