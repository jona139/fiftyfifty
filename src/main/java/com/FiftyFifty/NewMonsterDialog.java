package com.FiftyFifty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.FlatTextField;

/**
 * Dialog for adding a new monster to the database or editing an existing one
 */
@Slf4j
public class NewMonsterDialog extends JDialog {
    
    private final String monsterName;
    private final NewMonsterCallback callback;
    private final boolean isExistingMonster;
    private final String currentDropName;
    private final double currentDropRate;
    private final boolean currentExempt;
    
    // UI components
    private JTextField dropNameField;
    private FlatTextField dropRateField;
    private JCheckBox exemptCheckbox;
    
    /**
     * Constructor for adding a new monster
     */
    public NewMonsterDialog(Frame parent, String monsterName, NewMonsterCallback callback) {
        this(parent, monsterName, callback, false, "", 0.0, false);
    }
    
    /**
     * Constructor for editing an existing monster
     */
    public NewMonsterDialog(Frame parent, String monsterName, NewMonsterCallback callback, 
                           boolean isExistingMonster, String currentDropName, 
                           double currentDropRate, boolean currentExempt) {
        super(parent, isExistingMonster ? "Edit Monster" : "Add New Monster", true);
        this.monsterName = monsterName;
        this.callback = callback;
        this.isExistingMonster = isExistingMonster;
        this.currentDropName = currentDropName;
        this.currentDropRate = currentDropRate;
        this.currentExempt = currentExempt;
        
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        
        createUI();
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void createUI() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Title
        String titleText = isExistingMonster 
            ? "Edit Monster: " + monsterName
            : "New Monster Detected: " + monsterName;
        
        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Description
        String descText = isExistingMonster
            ? "<html>Update this monster in the Fifty-Fifty database.<br>You can modify its rarest drop information.</html>"
            : "<html>Add this monster to the Fifty-Fifty database.<br>Please provide information about its rarest drop.</html>";
        
        JLabel descLabel = new JLabel(descText);
        descLabel.setFont(FontManager.getRunescapeSmallFont());
        descLabel.setForeground(Color.LIGHT_GRAY);
        descLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        descLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        // Form Panel
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 10));
        formPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Rarest Drop Name
        JLabel dropNameLabel = new JLabel("Rarest Drop Name:");
        dropNameLabel.setFont(FontManager.getRunescapeSmallFont());
        dropNameLabel.setForeground(Color.WHITE);
        
        dropNameField = new JTextField(20);
        dropNameField.setFont(FontManager.getRunescapeSmallFont());
        
        // If editing, set the current drop name
        if (isExistingMonster) {
            dropNameField.setText(currentDropName);
        }
        
        // Drop Rate
        JLabel dropRateLabel = new JLabel("Drop Rate (1/X):");
        dropRateLabel.setFont(FontManager.getRunescapeSmallFont());
        dropRateLabel.setForeground(Color.WHITE);
        
        dropRateField = new FlatTextField();
        
        // If editing and not exempt, calculate the denominator from the drop rate
        if (isExistingMonster && !currentExempt && currentDropRate > 0) {
            int denominator = (int) Math.round(1.0 / currentDropRate);
            dropRateField.setText(String.valueOf(denominator));
        } else {
            dropRateField.setText("128");
        }
        
        // Exempt checkbox
        JLabel exemptLabel = new JLabel("Exempt from kill limit:");
        exemptLabel.setFont(FontManager.getRunescapeSmallFont());
        exemptLabel.setForeground(Color.WHITE);
        
        exemptCheckbox = new JCheckBox();
        exemptCheckbox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        exemptCheckbox.setToolTipText("Check if this monster should be exempt from kill limits (like cows)");
        
        // If editing, set the current exempt status
        if (isExistingMonster) {
            exemptCheckbox.setSelected(currentExempt);
        }
        
        // Add components to form
        formPanel.add(dropNameLabel);
        formPanel.add(dropNameField);
        formPanel.add(dropRateLabel);
        formPanel.add(dropRateField);
        formPanel.add(exemptLabel);
        formPanel.add(exemptCheckbox);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dispose());
        
        String buttonText = isExistingMonster ? "Update Monster" : "Add Monster";
        JButton addButton = new JButton(buttonText);
        addButton.setFocusPainted(false);
        addButton.addActionListener(e -> submitForm());
        
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(addButton);
        
        // Add all panels to main content
        contentPanel.add(titleLabel);
        contentPanel.add(descLabel);
        contentPanel.add(formPanel);
        contentPanel.add(buttonPanel);
        
        setContentPane(contentPanel);
    }
    
    private void submitForm() {
        // Validate drop name
        String dropName = dropNameField.getText().trim();
        if (dropName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a drop name.",
                    "Missing Information",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validate drop rate
        String dropRateStr = dropRateField.getText().trim();
        double dropRate;
        try {
            int denominator = Integer.parseInt(dropRateStr);
            if (denominator <= 0) {
                throw new NumberFormatException("Drop rate must be positive");
            }
            dropRate = 1.0 / denominator;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid drop rate denominator (e.g., 128 for 1/128).",
                    "Invalid Drop Rate",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        boolean isExempt = exemptCheckbox.isSelected();
        
        // All validation passed, call the callback
        callback.onNewMonsterAdded(monsterName, dropName, dropRate, isExempt);
        dispose();
    }
    
    /**
     * Callback interface for handling new monster addition
     */
    public interface NewMonsterCallback {
        void onNewMonsterAdded(String monsterName, String dropName, double dropRate, boolean isExempt);
    }
}