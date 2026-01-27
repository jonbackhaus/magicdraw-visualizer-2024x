#!/bin/bash

# Configuration
VERSION="1.0.0"
PLUGIN_ID="com.jonbackhaus.visualizer"
PLUGIN_UID="79833" # Unique ID for visualizer
JAR_NAME="magicdraw-visualizer-${VERSION}.jar"
DIST_DIR="dist"
DIST_DATE=$(date +%Y-%m-%d)
TEMP_DIR="dist/temp"
ZIP_NAME="visualizer-plugin-2024x-v${VERSION}.zip"
MDR_NAME="MDR_Plugin_Visualizer_v${VERSION}_descriptor.xml"

echo "Building Visualizer Distribution Bundle v$VERSION..."

# 1. Clean and build project
mvn clean package -DskipTests "$@"
if [ $? -ne 0 ]; then
    echo "Maven build failed!"
    exit 1
fi

# 2. Setup folder structure
mkdir -p "$DIST_DIR"
rm -rf "$DIST_DIR"/*

mkdir -p "$TEMP_DIR/data/resourcemanager"
mkdir -p "$TEMP_DIR/plugins/$PLUGIN_ID"

# 3. Copy files
cp "target/$JAR_NAME" "$TEMP_DIR/plugins/$PLUGIN_ID/"
cp "src/main/resources/META-INF/plugin.xml" "$TEMP_DIR/plugins/$PLUGIN_ID/"

# 4. Generate Resource Manager Descriptor
cat <<EOF > "$TEMP_DIR/data/resourcemanager/$MDR_NAME"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<resourceDescriptor critical="false" date="$DIST_DATE" description="Visualizer plugin for MagicDraw/Cameo. Implements custom non-symbol diagrams including Chord Diagram." id="$PLUGIN_UID" name="Visualizer Plugin" mdVersionMax="higher" mdVersionMin="17.0" restartMagicdraw="true" type="Plugin">
    <version human="$VERSION" internal="1" resource="1" />
    <provider name="Jon Backhaus" homePage="https://github.com/jonbackhaus/magicdraw-visualizer" />
    <edition>Reader</edition>
    <edition>Community</edition>
    <edition>Standard</edition>
    <edition>Professional Java</edition>
    <edition>Professional C++</edition>
    <edition>Professional</edition>
    <edition>Architect</edition>
    <edition>Enterprise</edition>
    <installation>
        <file from="plugins/$PLUGIN_ID/*.*" to="plugins/$PLUGIN_ID/*.*" />
        <file from="data/resourcemanager/$MDR_NAME" to="data/resourcemanager/$MDR_NAME" />
    </installation>
</resourceDescriptor>
EOF

# 5. Create Zip Bundle
cd "$TEMP_DIR"
# Exclude Mac specific files and directories
zip -r -X "../../$DIST_DIR/$ZIP_NAME" . -x "*.DS_Store" -x "__MACOSX*" > /dev/null
cd ../..

# 6. Cleanup
rm -rf "$TEMP_DIR"

echo "Distribution bundle created: $DIST_DIR/$ZIP_NAME"
echo "Contents of $DIST_DIR/:"
ls -F "$DIST_DIR"
