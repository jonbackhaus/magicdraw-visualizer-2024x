package com.jonbackhaus.visualizer;

import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.uml.DiagramDescriptor;
import com.jonbackhaus.visualizer.diagram.chord.ChordDiagramDescriptor;

/**
 * Main plugin class for Visualizer.
 */
public class VisualizerPlugin extends Plugin {

    @Override
    public void init() {
        registerDiagrams();
    }

    private void registerDiagrams() {
        // Register the Chord Diagram
        DiagramDescriptor chordDescriptor = new ChordDiagramDescriptor();
        Application.getInstance().addNewDiagramType(chordDescriptor);
    }

    @Override
    public boolean close() {
        return true;
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
