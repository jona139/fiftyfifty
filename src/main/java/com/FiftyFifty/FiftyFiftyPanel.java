package com.FiftyFifty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ColorJButton;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;

@Slf4j
public class FiftyFiftyPanel extends PluginPanel {

    private final EnemyTrackerPlugin plugin;
    private final EnemyKillTracker killTracker;
    private final EnemyTrackerConfig config;

    private final JPanel progressPanel;
    private final JPanel statsPanel;
    private final PluginErrorPanel errorPanel;
    private final JPanel progressBarsPanel;
    private final JPanel statRowsPanel;

    private final JShadowedLabel progressTitleLabel = new JShadowedLabel("In Progress Monsters:");
    private final JShadowedLabel statsTitleLabel = new JShadowedLabel("Overall Progress:");

    private final ColorJButton resetButton =
            new ColorJButton("Reset All Kills", ColorScheme.DARK_GRAY_COLOR);
    private final ColorJButton viewDashboardButton =
            new ColorJButton("View Detailed Dashboard", ColorScheme.DARK_GRAY_COLOR);
    private final ColorJButton helpButton =
            new ColorJButton("Help", ColorScheme.DARKER_GRAY_COLOR);
            
    // Custom monsters panel components
    private final JPanel customMonstersPanel;
    private final JShadowedLabel customMonstersTitleLabel = new JShadowedLabel("Custom Monsters:");
    private final JPanel customMonstersListPanel;
    private final ColorJButton addMonsterButton =
            new ColorJButton("Add New Monster", ColorScheme.DARKER_GRAY_COLOR);
            
    // Panel for pending new monsters
    private final JPanel pendingMonstersPanel;
    private final JShadowedLabel pendingMonstersTitleLabel = new JShadowedLabel("Pending New Monsters:");
    private final JPanel pendingMonstersListPanel;
    private final ColorJButton reviewMonstersButton =
            new ColorJButton("Review Pending Monsters", ColorScheme.DARKER_GRAY_COLOR);
    private final ColorJButton clearPendingButton =
            new ColorJButton("Clear All Pending", ColorScheme.DARK_GRAY_COLOR);

    @Inject
    public FiftyFiftyPanel(final EnemyTrackerPlugin plugin, final EnemyKillTracker killTracker,
                           final EnemyTrackerConfig config) {
        super();
        this.plugin = plugin;
        this.killTracker = killTracker;
        this.config = config;

        setOpaque(false);
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout(0, 10));

        // Title panel
        final JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        titlePanel.setOpaque(false);

        final JShadowedLabel title = new JShadowedLabel("Fifty-Fifty Tracker");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        titlePanel.add(title, BorderLayout.CENTER);

        // Progress panel for in-progress monsters
        progressPanel = new JPanel(new BorderLayout(0, 5));
        progressPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        progressPanel.setOpaque(false);

        progressTitleLabel.setFont(FontManager.getRunescapeFont());
        progressTitleLabel.setForeground(Color.WHITE);
        progressPanel.add(progressTitleLabel, BorderLayout.NORTH);

        progressBarsPanel = new JPanel(new GridLayout(0, 1, 0, 5));
        progressBarsPanel.setOpaque(false);
        progressPanel.add(progressBarsPanel, BorderLayout.CENTER);

        // Stats panel
        statsPanel = new JPanel(new BorderLayout(0, 5));
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        statsPanel.setOpaque(false);

        statsTitleLabel.setFont(FontManager.getRunescapeFont());
        statsTitleLabel.setForeground(Color.WHITE);
        statsPanel.add(statsTitleLabel, BorderLayout.NORTH);

        statRowsPanel = new JPanel(new GridLayout(0, 1, 0, 5));
        statRowsPanel.setOpaque(false);
        statsPanel.add(statRowsPanel, BorderLayout.CENTER);

        // Custom monsters panel
        customMonstersPanel = new JPanel(new BorderLayout(0, 5));
        customMonstersPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        customMonstersPanel.setOpaque(false);

        customMonstersTitleLabel.setFont(FontManager.getRunescapeFont());
        customMonstersTitleLabel.setForeground(Color.WHITE);
        customMonstersPanel.add(customMonstersTitleLabel, BorderLayout.NORTH);

        customMonstersListPanel = new JPanel(new GridLayout(0, 1, 0, 5));
        customMonstersListPanel.setOpaque(false);
        customMonstersPanel.add(customMonstersListPanel, BorderLayout.CENTER);

        addMonsterButton.setFont(FontManager.getRunescapeSmallFont());
        addMonsterButton.setFocusPainted(false);
        addMonsterButton.addActionListener(e -> plugin.openAddMonsterDialog());
        customMonstersPanel.add(addMonsterButton, BorderLayout.SOUTH);
        
        // Pending monsters panel
        pendingMonstersPanel = new JPanel(new BorderLayout(0, 5));
        pendingMonstersPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        pendingMonstersPanel.setOpaque(false);

        pendingMonstersTitleLabel.setFont(FontManager.getRunescapeFont());
        pendingMonstersTitleLabel.setForeground(Color.WHITE);
        pendingMonstersPanel.add(pendingMonstersTitleLabel, BorderLayout.NORTH);

        pendingMonstersListPanel = new JPanel(new GridLayout(0, 1, 0, 5));
        pendingMonstersListPanel.setOpaque(false);
        pendingMonstersPanel.add(pendingMonstersListPanel, BorderLayout.CENTER);

        // Panel for buttons
        JPanel pendingButtonsPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        pendingButtonsPanel.setOpaque(false);

        reviewMonstersButton.setFont(FontManager.getRunescapeSmallFont());
        reviewMonstersButton.setFocusPainted(false);
        reviewMonstersButton.addActionListener(e -> reviewPendingMonsters());

        clearPendingButton.setFont(FontManager.getRunescapeSmallFont());
        clearPendingButton.setFocusPainted(false);
        clearPendingButton.addActionListener(e -> clearPendingMonsters());

        pendingButtonsPanel.add(reviewMonstersButton);
        pendingButtonsPanel.add(clearPendingButton);
        pendingMonstersPanel.add(pendingButtonsPanel, BorderLayout.SOUTH);

        // Button panel
        final JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        buttonPanel.setOpaque(false);

        resetButton.setFont(FontManager.getRunescapeSmallFont());
        resetButton.setFocusPainted(false);
        resetButton.addActionListener(e -> resetAllKills());

        viewDashboardButton.setFont(FontManager.getRunescapeSmallFont());
        viewDashboardButton.setFocusPainted(false);
        viewDashboardButton.addActionListener(e -> openDashboard());

        helpButton.setFont(FontManager.getRunescapeSmallFont());
        helpButton.setFocusPainted(false);
        helpButton.addActionListener(e -> plugin.openGitHubPage());

        buttonPanel.add(resetButton);
        buttonPanel.add(viewDashboardButton);
        buttonPanel.add(helpButton);

        // Error panel
        errorPanel = new PluginErrorPanel();
        errorPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        errorPanel.setContent("No data available", "Start killing monsters to track progress!");

        // Add everything to the main panel
        add(titlePanel, BorderLayout.NORTH);

        // Container for content panels
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        contentPanel.add(progressPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(statsPanel);

        // Only add pending monsters panel if there are any pending monsters
        if (!plugin.getPendingNewMonsters().isEmpty()) {
            contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            contentPanel.add(pendingMonstersPanel);
        }

        // Only add custom monsters panel if configured
        if (config.showCustomMonstersSection()) {
            contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            contentPanel.add(customMonstersPanel);
        }

        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void update() {
        Map<String, Integer> allKills = killTracker.getAllKills();
        Map<String, Integer> thresholds = NpcKillThreshold.getNpcThresholds();

        if (allKills.isEmpty()) {
            remove(progressPanel);
            remove(statsPanel);
            remove(pendingMonstersPanel);
            remove(customMonstersPanel);
            add(errorPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        // Remove error panel if there's data
        remove(errorPanel);

        // Update progress panel with in-progress monsters
        updateProgressPanel(allKills);

        // Update stats panel
        updateStatsPanel(allKills, thresholds);
        
        // Update pending monsters panel
        updatePendingMonstersPanel();
        
        // Update custom monsters panel
        updateCustomMonstersPanel();
        
        // Get the content panel
        JPanel contentPanel = (JPanel)getComponent(1);
        
        // Clear the content panel
        contentPanel.removeAll();
        
        // Add panels to content panel
        contentPanel.add(progressPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(statsPanel);
        
        // Only add pending monsters panel if there are any pending monsters
        if (!plugin.getPendingNewMonsters().isEmpty()) {
            contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            contentPanel.add(pendingMonstersPanel);
        }
        
        // Only add custom monsters panel if configured
        if (config.showCustomMonstersSection()) {
            contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            contentPanel.add(customMonstersPanel);
        }

        revalidate();
        repaint();
    }

    private void updateProgressPanel(Map<String, Integer> allKills) {
        // Get in-progress monsters (kills > 0 but < threshold), excluding exempt monsters
        List<MobProgress> inProgressMobs = allKills.entrySet().stream()
                .filter(entry -> {
                    String mobName = entry.getKey();

                    // Skip exempt monsters like cows
                    if (NpcKillThreshold.isExempt(mobName)) {
                        return false;
                    }

                    int threshold = NpcKillThreshold.getThreshold(mobName);
                    return entry.getValue() > 0 && entry.getValue() < threshold;
                })
                .map(entry -> new MobProgress(
                        entry.getKey(),
                        entry.getValue(),
                        NpcKillThreshold.getThreshold(entry.getKey())
                ))
                .sorted(Comparator.comparing(MobProgress::getProgress).reversed())
                .limit(5) // Show at most 5 to avoid panel getting too long
                .collect(Collectors.toList());

        // Clear existing progress bars
        progressBarsPanel.removeAll();

        // If no monsters in progress
        if (inProgressMobs.isEmpty()) {
            JShadowedLabel noProgressLabel = new JShadowedLabel("No monsters in progress");
            noProgressLabel.setFont(FontManager.getRunescapeSmallFont());
            noProgressLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            noProgressLabel.setHorizontalAlignment(SwingConstants.CENTER);
            progressBarsPanel.add(noProgressLabel);
            return;
        }

        // Create progress bars
        for (MobProgress mob : inProgressMobs) {
            ProgressBar progressBar = new ProgressBar();
            progressBar.setMaximumValue(mob.getThreshold());
            progressBar.setValue(mob.getKills());
            progressBar.setLeftLabel(mob.getName());
            progressBar.setRightLabel(mob.getKills() + "/" + mob.getThreshold());

            // Calculate percentage (0-100)
            int percentage = (int)Math.round((double)mob.getKills() / mob.getThreshold() * 100);
            progressBar.setCenterLabel(percentage + "%");
            progressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);

            // Color based on progress
            if (percentage < 30) {
                progressBar.setForeground(Color.RED);
            } else if (percentage < 60) {
                progressBar.setForeground(Color.decode("#ea6600")); // Orange
            } else if (percentage < 90) {
                progressBar.setForeground(Color.decode("#ffb600")); // Yellow
            } else {
                progressBar.setForeground(Color.GREEN);
            }

            progressBarsPanel.add(progressBar);
        }
    }

    private void updateStatsPanel(Map<String, Integer> allKills, Map<String, Integer> thresholds) {
        // Clear existing stats
        statRowsPanel.removeAll();

        // Filter out exempt monsters
        Map<String, Integer> filteredThresholds = new HashMap<>();
        for (Map.Entry<String, Integer> entry : thresholds.entrySet()) {
            if (!NpcKillThreshold.isExempt(entry.getKey())) {
                filteredThresholds.put(entry.getKey(), entry.getValue());
            }
        }

        // Calculate stats for non-exempt monsters only
        int totalKills = 0;
        int completed = 0;
        int inProgress = 0;
        int notStarted = 0;

        String mostKilled = "None";
        int mostKillCount = 0;

        for (Map.Entry<String, Integer> entry : allKills.entrySet()) {
            String mobName = entry.getKey();
            int kills = entry.getValue();

            // Only count kills for non-exempt monsters
            if (!NpcKillThreshold.isExempt(mobName)) {
                totalKills += kills;

                if (kills > mostKillCount) {
                    mostKilled = mobName;
                    mostKillCount = kills;
                }
            }
        }

        for (Map.Entry<String, Integer> entry : filteredThresholds.entrySet()) {
            String mobName = entry.getKey();
            int threshold = entry.getValue();
            int kills = allKills.getOrDefault(mobName, 0);

            if (kills >= threshold) {
                completed++;
            } else if (kills > 0) {
                inProgress++;
            } else {
                notStarted++;
            }
        }

        // Create stats labels
        addStatRow(statRowsPanel, "Total Kills", String.valueOf(totalKills));
        addStatRow(statRowsPanel, "Completed", String.valueOf(completed));
        addStatRow(statRowsPanel, "In Progress", String.valueOf(inProgress));
        addStatRow(statRowsPanel, "Not Started", String.valueOf(notStarted));

        if (!mostKilled.equals("None")) {
            addStatRow(statRowsPanel, "Most Killed", mostKilled + " (" + mostKillCount + ")");
        }

        // Overall progress
        float overallProgress = filteredThresholds.size() > 0
                ? (float) completed / filteredThresholds.size()
                : 0;

        ProgressBar totalProgressBar = new ProgressBar();
        totalProgressBar.setMaximumValue(filteredThresholds.size());
        totalProgressBar.setValue(completed);
        totalProgressBar.setLeftLabel("Overall");

        DecimalFormat df = new DecimalFormat("#.#%");
        totalProgressBar.setRightLabel(df.format(overallProgress));

        int percentage = (int)Math.round(overallProgress * 100);
        totalProgressBar.setCenterLabel("Progress");
        totalProgressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Color based on progress
        if (percentage < 30) {
            totalProgressBar.setForeground(Color.RED);
        } else if (percentage < 60) {
            totalProgressBar.setForeground(Color.decode("#ea6600")); // Orange
        } else if (percentage < 90) {
            totalProgressBar.setForeground(Color.decode("#ffb600")); // Yellow
        } else {
            totalProgressBar.setForeground(Color.GREEN);
        }

        statRowsPanel.add(totalProgressBar);
    }
    
    /**
     * Updates the pending monsters panel with current pending monsters
     */
    private void updatePendingMonstersPanel() {
        // Clear existing list
        pendingMonstersListPanel.removeAll();
        
        // Get pending monsters
        Map<String, Long> pendingMonsters = plugin.getPendingNewMonsters();
        
        // If no pending monsters
        if (pendingMonsters.isEmpty()) {
            // Return if there are no pending monsters - the panel won't be shown
            return;
        }
        
        // Sort monsters alphabetically
        List<String> sortedMonsters = new ArrayList<>(pendingMonsters.keySet());
        Collections.sort(sortedMonsters);
        
        // Add each pending monster to the panel
        for (String monsterName : sortedMonsters) {
            JPanel monsterPanel = new JPanel(new BorderLayout());
            monsterPanel.setOpaque(false);
            monsterPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR));
            
            JShadowedLabel nameLabel = new JShadowedLabel(monsterName);
            nameLabel.setFont(FontManager.getRunescapeSmallFont());
            nameLabel.setForeground(Color.WHITE);
            
            JButton addButton = new JButton("Add");
            addButton.setFont(FontManager.getRunescapeSmallFont());
            addButton.setFocusPainted(false);
            addButton.addActionListener(e -> addPendingMonster(monsterName));
            
            JButton skipButton = new JButton("Skip");
            skipButton.setFont(FontManager.getRunescapeSmallFont());
            skipButton.setFocusPainted(false);
            skipButton.addActionListener(e -> skipPendingMonster(monsterName));
            
            JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
            buttonPanel.setOpaque(false);
            buttonPanel.add(addButton);
            buttonPanel.add(skipButton);
            
            monsterPanel.add(nameLabel, BorderLayout.CENTER);
            monsterPanel.add(buttonPanel, BorderLayout.EAST);
            
            pendingMonstersListPanel.add(monsterPanel);
        }
    }
    
    /**
     * Updates the custom monsters panel
     */
    private void updateCustomMonstersPanel() {
        // Clear existing list
        customMonstersListPanel.removeAll();
        
        // Get custom monsters
        Map<String, String> customMonsters = NpcKillThreshold.getCustomMonsters();
        
        // If no custom monsters
        if (customMonsters.isEmpty()) {
            JShadowedLabel noCustomLabel = new JShadowedLabel("No custom monsters defined");
            noCustomLabel.setFont(FontManager.getRunescapeSmallFont());
            noCustomLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            noCustomLabel.setHorizontalAlignment(SwingConstants.CENTER);
            customMonstersListPanel.add(noCustomLabel);
            return;
        }
        
        // Sort monsters alphabetically
        List<Map.Entry<String, String>> sortedMonsters = new ArrayList<>(customMonsters.entrySet());
        sortedMonsters.sort(Map.Entry.comparingByKey());
        
        // Add each custom monster to the panel
        for (Map.Entry<String, String> entry : sortedMonsters) {
            String monsterName = entry.getKey();
            String dropName = entry.getValue();
            int kills = killTracker.getKills(monsterName);
            int threshold = NpcKillThreshold.getThreshold(monsterName);
            boolean isExempt = NpcKillThreshold.isExempt(monsterName);
            
            JPanel monsterPanel = new JPanel(new BorderLayout());
            monsterPanel.setOpaque(false);
            monsterPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR));
            
            JPanel infoPanel = new JPanel(new GridLayout(0, 1));
            infoPanel.setOpaque(false);
            
            // Monster name
            JShadowedLabel nameLabel = new JShadowedLabel(monsterName);
            nameLabel.setFont(FontManager.getRunescapeSmallFont());
            nameLabel.setForeground(Color.WHITE);
            infoPanel.add(nameLabel);
            
            // Drop info
            String dropInfo = isExempt ? "Exempt" : "Drop: " + dropName;
            JShadowedLabel dropLabel = new JShadowedLabel(dropInfo);
            dropLabel.setFont(FontManager.getRunescapeSmallFont());
            dropLabel.setForeground(isExempt ? Color.CYAN : ColorScheme.LIGHT_GRAY_COLOR);
            infoPanel.add(dropLabel);
            
            // Kill info if not exempt
            if (!isExempt) {
                String killInfo = kills + "/" + threshold + " kills";
                JShadowedLabel killsLabel = new JShadowedLabel(killInfo);
                killsLabel.setFont(FontManager.getRunescapeSmallFont());
                killsLabel.setForeground(kills >= threshold ? Color.RED : ColorScheme.LIGHT_GRAY_COLOR);
                infoPanel.add(killsLabel);
            }
            
            monsterPanel.add(infoPanel, BorderLayout.CENTER);
            customMonstersListPanel.add(monsterPanel);
        }
    }
    
    /**
     * Add a pending monster to the database
     */
    private void addPendingMonster(String monsterName) {
        plugin.handleNewMonster(monsterName);
        plugin.removePendingMonster(monsterName);
    }

    /**
     * Skip a pending monster (remove it from the list without adding)
     */
    private void skipPendingMonster(String monsterName) {
        plugin.removePendingMonster(monsterName);
    }

    /**
     * Review all pending monsters in sequence
     */
    private void reviewPendingMonsters() {
        // Get a copy of the monster names to avoid concurrent modification
        List<String> monsters = new ArrayList<>(plugin.getPendingNewMonsters().keySet());
        if (monsters.isEmpty()) {
            return;
        }
        
        // Process the first monster
        addPendingMonster(monsters.get(0));
    }

    /**
     * Clear all pending monsters
     */
    private void clearPendingMonsters() {
        // Confirm with the user first
        int confirm = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to clear all pending monsters?",
            "Confirm Clear",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            plugin.clearPendingMonsters();
        }
    }

    private void addStatRow(JPanel panel, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setOpaque(false);

        JShadowedLabel nameLabel = new JShadowedLabel(label);
        nameLabel.setFont(FontManager.getRunescapeSmallFont());
        nameLabel.setForeground(Color.WHITE);

        JShadowedLabel valueLabel = new JShadowedLabel(value);
        valueLabel.setFont(FontManager.getRunescapeSmallFont());
        valueLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(nameLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);

        panel.add(row);
    }

    private void resetAllKills() {
        // Create a confirmation dialog
        int confirm = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to reset all kill counts?",
            "Confirm Reset",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE
        );
        
        // Only reset if user confirmed
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            killTracker.resetKills();
            update();
        }
    }

    private void openDashboard() {
        plugin.openDashboard();
    }

    // Helper class for sorting monsters by progress
    private static class MobProgress {
        private final String name;
        private final int kills;
        private final int threshold;

        public MobProgress(String name, int kills, int threshold) {
            this.name = name;
            this.kills = kills;
            this.threshold = threshold;
        }

        public String getName() {
            return name;
        }

        public int getKills() {
            return kills;
        }

        public int getThreshold() {
            return threshold;
        }

        public float getProgress() {
            return (float) kills / threshold;
        }
    }
}