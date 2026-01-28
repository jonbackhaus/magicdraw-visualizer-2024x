package com.jonbackhaus.visualizer.ui;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Namespace;
import com.nomagic.magicdraw.uml.RepresentationTextCreator;
import com.nomagic.magicdraw.uml.BaseElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Configuration panel for Chord Diagram, styled after MagicDraw's Relation Map.
 */
public class DiagramConfigPanel extends JPanel {

    private JTextField contextField;
    private JButton selectContextButton;
    private JCheckBox recursiveCheckbox;
    private JComboBox<String> elementTypeCombo;
    private JCheckBox includeSubtypesCheckbox;
    private JComboBox<String> relationCriteriaCombo;
    private JCheckBox showImpliedCheckbox;
    private JSpinner depthSpinner;
    private JCheckBox showOrphansCheckbox;
    private JCheckBox showLabelsCheckbox;
    private JCheckBox showLegendCheckbox;
    private JButton refreshButton;

    private Namespace contextElement;
    private Namespace defaultContext;

    public DiagramConfigPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(280, 500));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Section: Context
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        JLabel contextLabel = new JLabel("Context");
        contextLabel.setFont(contextLabel.getFont().deriveFont(Font.BOLD));
        add(contextLabel, gbc);
        gbc.gridwidth = 1;

        // Context selector
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        contextField = new JTextField();
        contextField.setEditable(false);
        contextField.setText("(diagram owner)");
        add(contextField, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0;
        selectContextButton = new JButton("...");
        selectContextButton.setPreferredSize(new Dimension(30, 22));
        selectContextButton.setToolTipText("Select context element");
        add(selectContextButton, gbc);
        row++;

        // Recursive checkbox
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        recursiveCheckbox = new JCheckBox("Include nested elements");
        recursiveCheckbox.setSelected(false);
        add(recursiveCheckbox, gbc);
        gbc.gridwidth = 1;
        row++;

        // Spacer
        gbc.gridy = row++;
        add(Box.createVerticalStrut(10), gbc);

        // Section: Elements
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        JLabel elementsLabel = new JLabel("Elements");
        elementsLabel.setFont(elementsLabel.getFont().deriveFont(Font.BOLD));
        add(elementsLabel, gbc);
        gbc.gridwidth = 1;

        // Element Type
        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel("Element Type:"), gbc);
        gbc.gridx = 1;
        elementTypeCombo = new JComboBox<>(new String[] {
            "Any", "Class", "Block", "Component", "Requirement", "Package", "Interface"
        });
        elementTypeCombo.setSelectedItem("Any");
        add(elementTypeCombo, gbc);
        row++;

        // Include Subtypes
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        includeSubtypesCheckbox = new JCheckBox("Include Subtypes");
        includeSubtypesCheckbox.setSelected(true);
        add(includeSubtypesCheckbox, gbc);
        gbc.gridwidth = 1;
        row++;

        // Spacer
        gbc.gridy = row++;
        add(Box.createVerticalStrut(10), gbc);

        // Section: Relations
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        JLabel relationsLabel = new JLabel("Relations");
        relationsLabel.setFont(relationsLabel.getFont().deriveFont(Font.BOLD));
        add(relationsLabel, gbc);
        gbc.gridwidth = 1;

        // Relation Criteria
        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel("Criteria:"), gbc);
        gbc.gridx = 1;
        relationCriteriaCombo = new JComboBox<>(new String[] {
            "Any", "Dependency", "Association", "Generalization", "Realization", "Usage"
        });
        relationCriteriaCombo.setSelectedItem("Any");
        add(relationCriteriaCombo, gbc);
        row++;

        // Show Implied
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        showImpliedCheckbox = new JCheckBox("Show Implied Relationships");
        showImpliedCheckbox.setSelected(false);
        add(showImpliedCheckbox, gbc);
        gbc.gridwidth = 1;
        row++;

        // Spacer
        gbc.gridy = row++;
        add(Box.createVerticalStrut(10), gbc);

        // Section: Display
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        JLabel displayLabel = new JLabel("Display");
        displayLabel.setFont(displayLabel.getFont().deriveFont(Font.BOLD));
        add(displayLabel, gbc);
        gbc.gridwidth = 1;

        // Depth
        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel("Depth:"), gbc);
        gbc.gridx = 1;
        depthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        add(depthSpinner, gbc);
        row++;

        // Show Orphans
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        showOrphansCheckbox = new JCheckBox("Show Orphans");
        showOrphansCheckbox.setSelected(true);
        showOrphansCheckbox.setToolTipText("Show elements with no relationships");
        add(showOrphansCheckbox, gbc);
        gbc.gridwidth = 1;
        row++;

        // Show Labels
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        showLabelsCheckbox = new JCheckBox("Show Labels");
        showLabelsCheckbox.setSelected(true);
        add(showLabelsCheckbox, gbc);
        gbc.gridwidth = 1;
        row++;

        // Show Legend
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        showLegendCheckbox = new JCheckBox("Show Legend");
        showLegendCheckbox.setSelected(false);
        add(showLegendCheckbox, gbc);
        gbc.gridwidth = 1;
        row++;

        // Spacer
        gbc.gridy = row++;
        add(Box.createVerticalStrut(15), gbc);

        // Refresh Button
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        refreshButton = new JButton("Refresh");
        refreshButton.setPreferredSize(new Dimension(100, 28));
        add(refreshButton, gbc);
        row++;

        // Push everything up
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(new JPanel(), gbc);
    }

    /**
     * Set the default context (diagram owner).
     */
    public void setDefaultContext(Namespace context) {
        this.defaultContext = context;
        if (contextElement == null) {
            updateContextDisplay();
        }
    }

    /**
     * Set the context select button action.
     */
    public void setContextSelectAction(ActionListener listener) {
        selectContextButton.addActionListener(listener);
    }

    /**
     * Set the selected context element.
     */
    public void setContextElement(Namespace element) {
        this.contextElement = element;
        updateContextDisplay();
    }

    /**
     * Clear the context override (use default).
     */
    public void clearContextOverride() {
        this.contextElement = null;
        updateContextDisplay();
    }

    private void updateContextDisplay() {
        if (contextElement != null) {
            contextField.setText(RepresentationTextCreator.getRepresentedText((BaseElement) contextElement));
        } else if (defaultContext != null) {
            contextField.setText(RepresentationTextCreator.getRepresentedText((BaseElement) defaultContext) + " (default)");
        } else {
            contextField.setText("(diagram owner)");
        }
    }

    // Getters for configuration values
    public Namespace getContextElement() {
        return contextElement != null ? contextElement : defaultContext;
    }

    public boolean isContextOverridden() {
        return contextElement != null;
    }

    public boolean isRecursive() {
        return recursiveCheckbox.isSelected();
    }

    public String getElementType() {
        return (String) elementTypeCombo.getSelectedItem();
    }

    public boolean isIncludeSubtypes() {
        return includeSubtypesCheckbox.isSelected();
    }

    public String getRelationCriteria() {
        return (String) relationCriteriaCombo.getSelectedItem();
    }

    public boolean isShowImplied() {
        return showImpliedCheckbox.isSelected();
    }

    public int getDepth() {
        return (Integer) depthSpinner.getValue();
    }

    public boolean isShowOrphans() {
        return showOrphansCheckbox.isSelected();
    }

    public boolean isShowLabels() {
        return showLabelsCheckbox.isSelected();
    }

    public boolean isShowLegend() {
        return showLegendCheckbox.isSelected();
    }

    // Setters for loading saved settings
    public void setRecursive(boolean value) {
        recursiveCheckbox.setSelected(value);
    }

    public void setElementType(String value) {
        elementTypeCombo.setSelectedItem(value);
    }

    public void setIncludeSubtypes(boolean value) {
        includeSubtypesCheckbox.setSelected(value);
    }

    public void setRelationCriteria(String value) {
        relationCriteriaCombo.setSelectedItem(value);
    }

    public void setShowImplied(boolean value) {
        showImpliedCheckbox.setSelected(value);
    }

    public void setDepth(int value) {
        depthSpinner.setValue(value);
    }

    public void setShowOrphans(boolean value) {
        showOrphansCheckbox.setSelected(value);
    }

    public void setShowLabels(boolean value) {
        showLabelsCheckbox.setSelected(value);
    }

    public void setShowLegend(boolean value) {
        showLegendCheckbox.setSelected(value);
    }

    public void addRefreshListener(ActionListener listener) {
        refreshButton.addActionListener(listener);
    }
}
