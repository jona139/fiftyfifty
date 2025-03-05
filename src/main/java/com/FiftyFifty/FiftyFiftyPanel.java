package com.FiftyFifty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.BorderFactory;
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
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setOpaque(false);
        contentPanel.add(progressPanel, BorderLayout.NORTH);
        contentPanel.add(statsPanel, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void update() {
        Map<String, Integer> allKills = killTracker.getAllKills();
        Map<String, Integer> thresholds = NpcKillThreshold.getNpcThresholds();

        if (allKills.isEmpty()) {
            remove(progressPanel);
            remove(statsPanel);
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
        killTracker.resetKills();
        update();
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