/**
 * D3 Chord Diagram Renderer
 */

// Check if D3 is available
if (typeof d3 === 'undefined') {
    console.error('D3.js is not loaded! Chart will not render.');
    document.getElementById('chart').innerHTML =
        '<div style="padding: 20px; color: #c00; font-size: 14px;">' +
        'Error: D3.js library failed to load. Please check your network connection.' +
        '</div>';
} else {
    console.log('D3.js loaded successfully, version: ' + d3.version);
}

const color = d3.scaleOrdinal(d3.schemeCategory10);

/**
 * Navigate to an element in MagicDraw's containment tree.
 */
function navigateToElement(index) {
    if (window.javaNavigation && typeof window.javaNavigation.selectElement === 'function') {
        console.log('Navigating to element index: ' + index);
        window.javaNavigation.selectElement(index);
    } else {
        console.log('Java navigation bridge not available');
    }
}

/**
 * Navigate to a relationship in MagicDraw's containment tree.
 */
function navigateToRelationship(sourceIndex, targetIndex) {
    if (window.javaNavigation && typeof window.javaNavigation.selectRelationship === 'function') {
        console.log('Navigating to relationship: ' + sourceIndex + ' -> ' + targetIndex);
        window.javaNavigation.selectRelationship(sourceIndex, targetIndex);
    } else {
        console.log('Java navigation bridge not available');
    }
}

/**
 * Updates the diagram with new data.
 * @param {Object} data - Adjacency matrix and labels.
 * @param {Array<Array<number>>} data.matrix - Square Adjacency Matrix.
 * @param {Array<string>} data.names - Labels for each index.
 * @param {Object} data.options - Display options.
 * @param {boolean} data.options.showLabels - Whether to show labels around arcs.
 * @param {boolean} data.options.showLegend - Whether to show the legend.
 */
window.updateDiagram = function(data) {
    console.log('updateDiagram called with data:', JSON.stringify(data).substring(0, 200) + '...');

    try {
        // Validate D3 is available
        if (typeof d3 === 'undefined') {
            throw new Error('D3.js is not available');
        }

        const { matrix, names, options = {} } = data;
        const showLabels = options.showLabels !== false; // Default true
        const showLegend = options.showLegend === true;  // Default false

        // Validate data
        if (!matrix || !Array.isArray(matrix)) {
            throw new Error('Invalid matrix data: ' + typeof matrix);
        }
        if (!names || !Array.isArray(names)) {
            throw new Error('Invalid names data: ' + typeof names);
        }
        if (matrix.length === 0) {
            throw new Error('Matrix is empty');
        }
        if (matrix.length !== names.length) {
            throw new Error('Matrix size (' + matrix.length + ') does not match names length (' + names.length + ')');
        }

        console.log('Data validated: ' + names.length + ' elements, showLabels=' + showLabels + ', showLegend=' + showLegend);

        // Clear the chart div completely (removes loading message)
        const chartDiv = document.getElementById('chart');
        chartDiv.innerHTML = '';

        // Calculate dimensions
        const width = chartDiv.clientWidth || window.innerWidth;
        const height = chartDiv.clientHeight || window.innerHeight;
        const legendWidth = showLegend ? 200 : 0;
        const diagramWidth = width - legendWidth;
        const labelPadding = showLabels ? 80 : 20;
        const outerRadius = Math.min(diagramWidth, height) * 0.5 - labelPadding;
        const innerRadius = outerRadius - 30;

        // Create chord layout
        const chord = d3.chord()
            .padAngle(0.05)
            .sortSubgroups(d3.descending);

        const arc = d3.arc()
            .innerRadius(innerRadius)
            .outerRadius(outerRadius);

        const ribbon = d3.ribbon()
            .radius(innerRadius);

        // Create SVG
        const svg = d3.select("#chart").append("svg")
            .attr("width", width)
            .attr("height", height)
            .attr("style", "font: 11px sans-serif;");

        // Create diagram group (centered in diagram area)
        const diagramG = svg.append("g")
            .attr("transform", `translate(${diagramWidth / 2}, ${height / 2})`);

        const chords = chord(matrix);
        console.log('Chords computed: ' + chords.length + ' chords, ' + chords.groups.length + ' groups');

        // Draw arc groups
        const group = diagramG.append("g")
          .selectAll("g")
          .data(chords.groups)
          .join("g");

        group.append("path")
            .attr("fill", d => color(d.index))
            .attr("stroke", d => d3.rgb(color(d.index)).darker())
            .attr("d", arc)
            .style("cursor", "pointer")
            .on("click", function(event, d) {
                navigateToElement(d.index);
            });

        group.append("title")
            .text(d => `${names[d.index]}: ${d.value} connections\nClick to navigate`);

        // Add labels around the arcs (if enabled)
        if (showLabels) {
            group.append("text")
                .each(d => { d.angle = (d.startAngle + d.endAngle) / 2; })
                .attr("dy", "0.35em")
                .attr("transform", d => `
                    rotate(${(d.angle * 180 / Math.PI - 90)})
                    translate(${outerRadius + 10})
                    ${d.angle > Math.PI ? "rotate(180)" : ""}
                `)
                .attr("text-anchor", d => d.angle > Math.PI ? "end" : "start")
                .text(d => names[d.index].length > 20 ? names[d.index].substring(0, 17) + '...' : names[d.index])
                .style("font-size", "11px")
                .style("fill", "#333")
                .style("cursor", "pointer")
                .on("click", function(event, d) {
                    navigateToElement(d.index);
                })
              .append("title")
                .text(d => `${names[d.index]}\nClick to navigate`);
        }

        // Draw ribbons (chords)
        diagramG.append("g")
            .attr("fill-opacity", 0.67)
          .selectAll("path")
          .data(chords)
          .join("path")
            .attr("d", ribbon)
            .attr("fill", d => color(d.source.index))
            .attr("stroke", d => d3.rgb(color(d.source.index)).darker())
            .style("cursor", "pointer")
            .on("click", function(event, d) {
                // Navigate to the relationship itself
                navigateToRelationship(d.source.index, d.target.index);
            })
          .append("title")
            .text(d => `${names[d.source.index]} \u2194 ${names[d.target.index]}: ${d.source.value}\nClick to navigate to relationship`);

        // Create legend (if enabled)
        if (showLegend) {
            const legend = svg.append("g")
                .attr("transform", `translate(${diagramWidth + 20}, 30)`);

            legend.append("text")
                .attr("x", 0)
                .attr("y", 0)
                .style("font-weight", "bold")
                .style("font-size", "12px")
                .text("Legend");

            const legendItems = legend.selectAll(".legend-item")
                .data(names)
                .join("g")
                .attr("class", "legend-item")
                .attr("transform", (d, i) => `translate(0, ${20 + i * 20})`)
                .style("cursor", "pointer")
                .on("click", function(event, d) {
                    const index = names.indexOf(d);
                    navigateToElement(index);
                });

            legendItems.append("rect")
                .attr("width", 14)
                .attr("height", 14)
                .attr("fill", (d, i) => color(i))
                .attr("stroke", (d, i) => d3.rgb(color(i)).darker());

            legendItems.append("text")
                .attr("x", 20)
                .attr("y", 11)
                .style("font-size", "11px")
                .text(d => d.length > 25 ? d.substring(0, 22) + '...' : d);

            legendItems.append("title")
                .text(d => d + '\nClick to navigate');
        }

        console.log('Diagram rendered successfully');
    } catch (error) {
        console.error('Error rendering diagram:', error.message);
        console.error('Stack trace:', error.stack);
        // Display error in the chart div
        document.getElementById('chart').innerHTML =
            '<div style="padding: 20px; color: #c00; font-size: 14px;">' +
            '<strong>Rendering Error:</strong> ' + error.message +
            '</div>';
    }
};

// Log when script is loaded
console.log('chord_render.js loaded successfully');
