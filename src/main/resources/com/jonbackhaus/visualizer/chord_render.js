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

const width = window.innerWidth;
const height = window.innerHeight;
const outerRadius = Math.min(width, height) * 0.5 - 40;
const innerRadius = outerRadius - 30;

const formatValue = d3.formatPrefix(",.0", 1e3);

const chord = d3.chord()
    .padAngle(0.05)
    .sortSubgroups(d3.descending);

const arc = d3.arc()
    .innerRadius(innerRadius)
    .outerRadius(outerRadius);

const ribbon = d3.ribbon()
    .radius(innerRadius);

const color = d3.scaleOrdinal(d3.schemeCategory10);

const svg = d3.select("#chart").append("svg")
    .attr("width", width)
    .attr("height", height)
    .attr("viewBox", [-width / 2, -height / 2, width, height])
    .attr("style", "max-width: 100%; height: auto; font: 10px sans-serif;");

/**
 * Updates the diagram with new data.
 * @param {Object} data - Adjacency matrix and labels.
 * @param {Array<Array<number>>} data.matrix - Square Adjacency Matrix.
 * @param {Array<string>} data.names - Labels for each index.
 */
window.updateDiagram = function(data) {
    console.log('updateDiagram called with data:', JSON.stringify(data).substring(0, 200) + '...');

    try {
        // Validate D3 is available
        if (typeof d3 === 'undefined') {
            throw new Error('D3.js is not available');
        }

        const { matrix, names } = data;

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

        console.log('Data validated: ' + names.length + ' elements, matrix size ' + matrix.length + 'x' + matrix[0].length);

        svg.selectAll("*").remove();

        const chords = chord(matrix);
        console.log('Chords computed: ' + chords.length + ' chords, ' + chords.groups.length + ' groups');

        const group = svg.append("g")
          .selectAll("g")
          .data(chords.groups)
          .join("g");

        group.append("path")
            .attr("fill", d => color(d.index))
            .attr("stroke", d => d3.rgb(color(d.index)).darker())
            .attr("d", arc);

        group.append("title")
            .text(d => `${names[d.index]}\n${formatValue(d.value)}`);

        const ticks = group.selectAll("g")
          .data(d => groupTicks(d, 1e3))
          .join("g")
            .attr("transform", d => `rotate(${d.angle * 180 / Math.PI - 90}) translate(${outerRadius},0)`);

        ticks.append("line")
            .attr("stroke", "currentColor")
            .attr("x2", 6);

        ticks.append("text")
            .attr("x", 8)
            .attr("dy", "0.35em")
            .attr("transform", d => d.angle > Math.PI ? "rotate(180) translate(-16)" : null)
            .attr("text-anchor", d => d.angle > Math.PI ? "end" : null)
            .text(d => formatValue(d.value));

        svg.append("g")
            .attr("fill-opacity", 0.67)
          .selectAll("path")
          .data(chords)
          .join("path")
            .attr("d", ribbon)
            .attr("fill", d => color(d.target.index))
            .attr("stroke", d => d3.rgb(color(d.target.index)).darker());

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

function groupTicks(d, step) {
  const k = (d.endAngle - d.startAngle) / d.value;
  return d3.range(0, d.value, step).map(value => {
    return {value: value, angle: value * k + d.startAngle};
  });
}

// Log when script is loaded
console.log('chord_render.js loaded successfully');

// Initial draw with sample data
const sampleData = {
    names: ["A", "B", "C", "D"],
    matrix: [
        [11975,  5871, 8916, 2868],
        [ 1951, 10048, 2060, 6171],
        [ 8010, 16145, 8090, 8045],
        [ 1013,   990,  940, 6907]
    ]
};
// window.updateDiagram(sampleData);
