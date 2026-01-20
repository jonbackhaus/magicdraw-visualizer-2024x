package com.jonbackhaus.visualizer.diagram;

import com.nomagic.magicdraw.uml.diagrams.NonSymbolDiagramDescriptor;
import javax.swing.JComponent;

/**
 * Base descriptor for all Visualizer diagrams.
 */
public abstract class VisualizerDiagramDescriptor extends NonSymbolDiagramDescriptor<JComponent> {

    protected VisualizerDiagramDescriptor() {
        super();
    }
}
