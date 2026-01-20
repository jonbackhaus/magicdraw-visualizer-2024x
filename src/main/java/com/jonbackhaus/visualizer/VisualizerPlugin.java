package com.jonbackhaus.visualizer;

import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.DiagramDescriptor;
import com.nomagic.magicdraw.uml.DiagramType;
import com.jonbackhaus.visualizer.diagram.chord.ChordDiagramDescriptor;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;

import java.util.Collection;

/**
 * Main plugin class for Visualizer.
 */
public class VisualizerPlugin extends Plugin {

    @Override
    public void init() {
        registerDiagrams();
        registerOwnership();
    }

    private void registerOwnership() {
        DiagramType.registerContextTypeByDiagramTypeConfigurator(new VisualizerContextTypesConfigurator());
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

    /**
     * Configurator for diagram context types (owners).
     */
    private static class VisualizerContextTypesConfigurator implements DiagramType.ContextTypesConfigurator {
        @Override
        public void configure(Project project, String diagramType, Collection<?> types) {
            if (ChordDiagramDescriptor.DIAGRAM_ID.equals(diagramType)) {
                // Allow creation in Packages
                ((Collection<Class<?>>) types).add(Package.class);
            }
        }
    }
}
