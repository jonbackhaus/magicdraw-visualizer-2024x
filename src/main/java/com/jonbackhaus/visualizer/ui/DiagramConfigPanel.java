package com.jonbackhaus.visualizer.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Configuration panel for Chord Diagram, styled after MagicDraw's Relation Map.
 */
public class DiagramConfigPanel extends JPanel {

    private JComboBox<String> elementTypeCombo;
    private JCheckBox includeSubtypesCheckbox;
    private JComboBox<String> relationCriteriaCombo;
    private JCheckBox showImpliedCheckbox;
    private JSpinner depthSpinner;
    private JComboBox<String> layoutCombo;
    private JButton refreshButton;

    public DiagramConfigPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(280, 400));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

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

        // Layout
        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel("Layout:"), gbc);
        gbc.gridx = 1;
        layoutCombo = new JComboBox<>(new String[] { "Chord", "Radial" });
        layoutCombo.setSelectedItem("Chord");
        add(layoutCombo, gbc);
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

    // Getters for configuration values
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

    public String getLayoutStyle() {
        return (String) layoutCombo.getSelectedItem();
    }

    public void addRefreshListener(ActionListener listener) {
        refreshButton.addActionListener(listener);
    }
}
