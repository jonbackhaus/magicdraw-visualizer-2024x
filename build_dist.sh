#!/bin/bash

# Configuration
PLUGIN_ID="com.jonbackhaus.visualizer"
JAR_NAME="magicdraw-visualizer-1.0.0.jar"
DIST_DIR="dist"
TEMP_DIR="temp_dist"

# Build the project
echo "Building project with Maven..."
mvn clean package -DskipTests

# Check if build succeeded
if [ $? -ne 0 ]; then
    echo "Maven build failed!"
    exit 1
fi

# Prepare distribution structure
echo "Preparing distribution structure..."
rm -rf $DIST_DIR $TEMP_DIR
mkdir -p $TEMP_DIR/plugins/$PLUGIN_ID
mkdir -p $TEMP_DIR/data/resourcemanager

# Copy plugin files
cp target/$JAR_NAME $TEMP_DIR/plugins/$PLUGIN_ID/
cp src/main/resources/META-INF/plugin.xml $TEMP_DIR/plugins/$PLUGIN_ID/
cp src/main/resources/com/jonbackhaus/visualizer/resource_manager.xml $TEMP_DIR/data/resourcemanager/

# Create the zip bundle
echo "Creating distribution zip..."
cd $TEMP_DIR
zip -r ../$DIST_DIR/MagicDraw_Visualizer_Plugin.zip .
cd ..

# Cleanup
# rm -rf $TEMP_DIR

echo "Build complete! Distribution zip available in $DIST_DIR/"
