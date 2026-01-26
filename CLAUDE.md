# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MagicDraw Visualizer is a Java plugin for MagicDraw/Cameo 2024x that provides custom non-symbol diagrams. The initial implementation includes a Chord Diagram for visualizing element relationships.

## Build Commands

```bash
# Build the JAR
mvn clean package

# Build distribution bundle (JAR + Resource Manager descriptor ZIP)
./build_dist.sh
```

## Development Setup

- **Java Version:** 11
- **MagicDraw Installation Path:** `/Applications/MagicDraw 2024xR3` (configured in pom.xml `md.path` property)
- **JxBrowser License:** Copy `src/main/resources/jxbrowser.properties.example` to `jxbrowser.properties` and add your license key

## Architecture

### Plugin Lifecycle
`VisualizerPlugin` (extends `com.nomagic.magicdraw.plugins.Plugin`) is the entry point. On `init()`, it:
1. Registers custom diagram types via `Application.getInstance().addNewDiagramType()`
2. Configures diagram ownership contexts (what containers can hold the diagram)

### Diagram System
The plugin uses MagicDraw's **NonSymbolDiagram** architecture (diagrams without UML symbol manipulation):

- `VisualizerDiagramDescriptor` - Abstract base extending `NonSymbolDiagramDescriptor<JComponent>`
- `ChordDiagramDescriptor` - Concrete descriptor that creates `ChordDiagramContent`
- `ChordDiagramContent` - Implements `NonSymbolDiagramContent<JComponent>`, manages:
  - JxBrowser instance for D3.js rendering
  - `DiagramConfigPanel` for user configuration
  - Adjacency matrix computation from UML relationships

### Adding New Diagram Types
1. Create a descriptor class extending `VisualizerDiagramDescriptor`
2. Create a content class implementing `NonSymbolDiagramContent<JComponent>`
3. Register in `VisualizerPlugin.registerDiagrams()`
4. Add context types in `VisualizerContextTypesConfigurator.configure()`

### Metacrawler Service
`MetacrawlerService` provides JMI-based metamodel traversal to discover element properties and relationships. Uses caching to avoid repeated metamodel introspection.

## Key MagicDraw API Patterns

- Access elements via `RefObject.refGetValue(propertyName)` for JMI property access
- Snapshot collections with `toArray()` before iteration (MagicDraw collections can change in background threads)
- Use `RepresentationTextCreator.getRepresentedText()` for human-readable element names
- Register diagrams on plugin init, not lazily

## Distribution

The plugin uses MagicDraw's Resource Manager format:
- Plugin ID: `com.jonbackhaus.visualizer`
- Output: `dist/visualizer-plugin-v{VERSION}.zip`
