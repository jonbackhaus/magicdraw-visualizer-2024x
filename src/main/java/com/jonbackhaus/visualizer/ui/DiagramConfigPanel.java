package com.jonbackhaus.visualizer.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

/**
 * Custom UI panel for diagram configuration, adapted from Dependency Matrix UI.
 */
public class DiagramConfigPanel extends JPanel {

    private JComboBox<String> elementTypeCombo;
    private JTextField scopeField;
    private JComboBox<String> dependencyCriteriaCombo;
    private JComboBox<String> directionCombo;
    private JCheckBox showElementsCheckbox;
    private JTextField filterField;

    public DiagramConfigPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Element Type
        add(new JLabel("Element Type:"), gbc);
        gbc.gridx = 1;
        elementTypeCombo = new JComboBox<>(new String[] { "Class", "Component", "Requirement", "Any" });
        add(elementTypeCombo, gbc);

        // Scope
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Scope (Optional):"), gbc);
        gbc.gridx = 1;
        scopeField = new JTextField(20);
        add(scopeField, gbc);

        // Dependency Criteria
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Dependency Criteria:"), gbc);
        gbc.gridx = 1;
        dependencyCriteriaCombo = new JComboBox<>(new String[] { "Dependency", "Realization", "Usage", "Any" });
        add(dependencyCriteriaCombo, gbc);

        // Direction
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Direction:"), gbc);
        gbc.gridx = 1;
        directionCombo = new JComboBox<>(new String[] { "Source to Target", "Target to Source", "Both" });
        directionCombo.setSelectedItem("Both");
        add(directionCombo, gbc);

        // Show Elements
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Show Elements:"), gbc);
        gbc.gridx = 1;
        showElementsCheckbox = new JCheckBox();
        showElementsCheckbox.setSelected(true);
        add(showElementsCheckbox, gbc);

        // Filter
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Filter:"), gbc);
        gbc.gridx = 1;
        filterField = new JTextField(20);
        add(filterField, gbc);

        // Push everything up
        gbc.gridy++;
        gbc.weighty = 1.0;
        add(new JPanel(), gbc);
    }

    // Getters for configuration values
    public String getElementType() {
        return (String) elementTypeCombo.getSelectedItem();
    }

    public String getScope() {
        return scopeField.getText();
    }

    public String getDependencyCriteria() {
        return (String) dependencyCriteriaCombo.getSelectedItem();
    }

    public String getDirection() {
        return (String) directionCombo.getSelectedItem();
    }

    public boolean isShowElements() {
        return showElementsCheckbox.isSelected();
    }

    public String getFilter() {
        return filterField.getText();
    }
}
