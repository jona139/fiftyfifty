package com.FiftyFifty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.ColorJButton;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;

@Slf4j
public class ProgressDashboard extends JFrame
{
    private final EnemyKillTracker killTracker;
    private final ConfigManager configManager;

    private boolean isOpen = false;

    private static final int DASHBOARD_WIDTH = 500;
    private static final int DASHBOARD_HEIGHT = 650;

    // Table model for all monsters
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> tableSorter;

    public ProgressDashboard(EnemyKillTracker killTracker, ConfigManager configManager)
    {
        this.killTracker = killTracker;
        this.configManager = configManager;

        setTitle("Fifty-Fifty Progress Dashboard");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(DASHBOARD_WIDTH, DASHBOARD_HEIGHT);
        setLocationRelativeTo(null);

        setBackground(ColorScheme.DARK_GRAY_COLOR);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                isOpen = false;
                configManager.setConfiguration("enemytracker", "openDashboard", false);
            }
        });
    }

    public void open()
    {
        if (isOpen)
        {
            requestFocus();
            return;
        }

        isOpen = true;

        // Create tabbed pane with RuneLite styling
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.setBorder(null);

        // Add tabs
        tabbedPane.add("In Progress", createProgressPanel());
        tabbedPane.add("All Monsters", createAllMonstersPanel());
        tabbedPane.add("Statistics", createStatsPanel());

        getContentPane().setBackground(ColorScheme.DARK_GRAY_COLOR);
        getContentPane().add(tabbedPane);

        pack();
        setVisible(true);
    }

    private JPanel createProgressPanel()
    {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setPreferredSize(new Dimension(DASHBOARD_WIDTH - 20, DASHBOARD_HEIGHT - 80));

        // Add header
        JShadowedLabel headerLabel = new JShadowedLabel("Monsters In Progress");
        headerLabel.setFont(FontManager.getRunescapeBoldFont());
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(headerLabel, BorderLayout.NORTH);

        // Get all kills
        Map<String, Integer> allKills = killTracker.getAllKills();

        // Track monsters in progress, excluding exempt monsters
        List<MobProgress> inProgress = allKills.entrySet().stream()
                .filter(entry -> {
                    String mobName = entry.getKey();

                    // Skip exempt monsters
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
                .collect(Collectors.toList());

        // Create in-progress panel
        JPanel progressCardsPanel = new JPanel();
        progressCardsPanel.setLayout(new BoxLayout(progressCardsPanel, BoxLayout.Y_AXIS));
        progressCardsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        progressCardsPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        if (inProgress.isEmpty())
        {
            JShadowedLabel emptyLabel = new JShadowedLabel("No monsters in progress");
            emptyLabel.setFont(FontManager.getRunescapeFont());
            emptyLabel.setForeground(Color.LIGHT_GRAY);
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            progressCardsPanel.add(emptyLabel);
        }
        else
        {
            for (MobProgress mob : inProgress)
            {
                progressCardsPanel.add(createProgressCard(mob));
                progressCardsPanel.add(createSpacerPanel(5));
            }
        }

        JScrollPane scrollPane = new JScrollPane(progressCardsPanel);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(16, 0));
        scrollPane.getVerticalScrollBar().setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollPane.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARKER_GRAY_COLOR),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                )
        );

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSpacerPanel(int height)
    {
        JPanel spacer = new JPanel();
        spacer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        spacer.setPreferredSize(new Dimension(0, height));
        spacer.setMinimumSize(new Dimension(0, height));
        spacer.setMaximumSize(new Dimension(Short.MAX_VALUE, height));
        spacer.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        return spacer;
    }

    private JPanel createProgressCard(MobProgress mob)
    {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARKER_GRAY_COLOR),
                BorderFactory.createEmptyBorder(7, 7, 7, 7)
        ));
        card.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
        card.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 85));

        // Header panel with name and percentage
        JPanel headerPanel = new JPanel(new BorderLayout(5, 0));
        headerPanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);

        // Monster name
        JShadowedLabel nameLabel = new JShadowedLabel(mob.getName());
        nameLabel.setFont(FontManager.getRunescapeBoldFont());
        nameLabel.setForeground(Color.WHITE);
        headerPanel.add(nameLabel, BorderLayout.WEST);

        // Percentage
        float percentage = mob.getProgress();
        DecimalFormat df = new DecimalFormat("#.#%");
        JShadowedLabel percentLabel = new JShadowedLabel(df.format(percentage));
        percentLabel.setFont(FontManager.getRunescapeSmallFont());
        percentLabel.setForeground(getColorForPercentage(percentage));
        headerPanel.add(percentLabel, BorderLayout.EAST);

        card.add(headerPanel, BorderLayout.NORTH);

        // Progress info
        JPanel infoPanel = new JPanel(new BorderLayout(5, 0));
        infoPanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);

        // Kill count info
        JShadowedLabel killsLabel = new JShadowedLabel(mob.getKills() + " / " + mob.getThreshold() + " kills");
        killsLabel.setFont(FontManager.getRunescapeSmallFont());
        killsLabel.setForeground(Color.LIGHT_GRAY);
        infoPanel.add(killsLabel, BorderLayout.WEST);

        // Remaining kills
        int remaining = mob.getThreshold() - mob.getKills();
        JShadowedLabel remainingLabel = new JShadowedLabel(remaining + " remaining");
        remainingLabel.setFont(FontManager.getRunescapeSmallFont());
        remainingLabel.setForeground(Color.LIGHT_GRAY);
        infoPanel.add(remainingLabel, BorderLayout.EAST);

        card.add(infoPanel, BorderLayout.CENTER);

        // Progress bar
        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaximumValue(mob.getThreshold());
        progressBar.setValue(mob.getKills());
        progressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        progressBar.setForeground(getColorForPercentage(percentage));

        card.add(progressBar, BorderLayout.SOUTH);

        return card;
    }

    private Color getColorForPercentage(float percentage)
    {
        if (percentage >= 1.0f) {
            return Color.RED;  // Maxed out - red
        } else if (percentage >= 0.75f) {
            return Color.decode("#aeff00");  // Light green
        } else if (percentage >= 0.5f) {
            return Color.decode("#ffe500");  // Yellow
        } else if (percentage >= 0.25f) {
            return Color.decode("#ffb600");  // Orange
        } else {
            return Color.decode("#ea6600");  // Dark orange
        }
    }

    private JPanel createAllMonstersPanel()
    {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setPreferredSize(new Dimension(DASHBOARD_WIDTH - 20, DASHBOARD_HEIGHT - 80));

        // Create column names
        String[] columnNames = {"Monster", "Kills", "Required", "Progress"};

        // Get all predefined monsters
        Map<String, Integer> thresholds = NpcKillThreshold.getNpcThresholds();
        Map<String, Integer> allKills = killTracker.getAllKills();

        // Filter out exempt monsters
        Map<String, Integer> filteredThresholds = new HashMap<>();
        for (Map.Entry<String, Integer> entry : thresholds.entrySet()) {
            String mobName = entry.getKey();
            // Only include non-exempt monsters
            if (!NpcKillThreshold.isExempt(mobName)) {
                filteredThresholds.put(mobName, entry.getValue());
            }
        }

        // Create data
        Object[][] data = new Object[filteredThresholds.size()][4];
        int i = 0;

        for (Map.Entry<String, Integer> entry : filteredThresholds.entrySet())
        {
            String mobName = entry.getKey();
            int threshold = entry.getValue();
            int kills = allKills.getOrDefault(mobName, 0);

            data[i][0] = mobName;
            data[i][1] = kills;
            data[i][2] = threshold;

            // Calculate percentage
            float percentage = (float) kills / threshold;
            DecimalFormat df = new DecimalFormat("#.#%");
            data[i][3] = df.format(percentage);

            i++;
        }

        // Create table model
        tableModel = new DefaultTableModel(data, columnNames)
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column)
            {
                if (column == 1 || column == 2)
                {
                    return Integer.class;
                }
                return String.class;
            }
        };

        // Create table
        JTable table = new JTable(tableModel);
        table.setRowHeight(25);
        table.setFont(FontManager.getRunescapeSmallFont());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(ColorScheme.DARKER_GRAY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(FontManager.getRunescapeBoldFont());
        table.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        table.setForeground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(ColorScheme.DARK_GRAY_COLOR);

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        centerRenderer.setForeground(Color.WHITE);

        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // Setup sorter
        tableSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(tableSorter);

        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(16, 0));
        scrollPane.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARKER_GRAY_COLOR),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                )
        );

        // Filter controls
        JPanel filterPanel = new JPanel(new BorderLayout(5, 0));
        filterPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        filterPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JShadowedLabel filterLabel = new JShadowedLabel("Filter: ");
        filterLabel.setFont(FontManager.getRunescapeSmallFont());
        filterLabel.setForeground(Color.WHITE);

        String[] filterOptions = {"All", "In Progress", "Completed", "Not Started"};
        JComboBox<String> filterBox = new JComboBox<>(new DefaultComboBoxModel<>(filterOptions));
        filterBox.setFocusable(false);
        filterBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        filterBox.setForeground(Color.WHITE);
        filterBox.setFont(FontManager.getRunescapeSmallFont());

        filterBox.addActionListener((ActionEvent e) -> {
            String selection = (String) filterBox.getSelectedItem();

            if (selection.equals("All")) {
                tableSorter.setRowFilter(null);
            } else {
                tableSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        int kills = (Integer) entry.getModel().getValueAt(entry.getIdentifier(), 1);
                        int threshold = (Integer) entry.getModel().getValueAt(entry.getIdentifier(), 2);

                        switch (selection) {
                            case "In Progress":
                                return kills > 0 && kills < threshold;
                            case "Completed":
                                return kills >= threshold;
                            case "Not Started":
                                return kills == 0;
                            default:
                                return true;
                        }
                    }
                });
            }
        });

        filterPanel.add(filterLabel, BorderLayout.WEST);
        filterPanel.add(filterBox, BorderLayout.CENTER);

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatsPanel()
    {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setPreferredSize(new Dimension(DASHBOARD_WIDTH - 20, DASHBOARD_HEIGHT - 80));

        // Header
        JShadowedLabel headerLabel = new JShadowedLabel("Statistics");
        headerLabel.setFont(FontManager.getRunescapeBoldFont());
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(headerLabel, BorderLayout.NORTH);

        // Stats content panel
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new DynamicGridLayout(0, 1, 0, 8));
        statsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        statsPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARKER_GRAY_COLOR),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                )
        );

        // Gather stats
        Map<String, Integer> allKills = killTracker.getAllKills();
        Map<String, Integer> thresholds = NpcKillThreshold.getNpcThresholds();

        // Filter out exempt monsters for statistics
        Map<String, Integer> filteredThresholds = new HashMap<>();
        for (Map.Entry<String, Integer> entry : thresholds.entrySet()) {
            String mobName = entry.getKey();
            // Only include non-exempt monsters
            if (!NpcKillThreshold.isExempt(mobName)) {
                filteredThresholds.put(mobName, entry.getValue());
            }
        }

        int totalKills = 0;
        int totalCompleted = 0;
        int totalInProgress = 0;
        int totalNotStarted = 0;
        String mostKilled = "None";
        int mostKillCount = 0;

        for (Map.Entry<String, Integer> entry : allKills.entrySet())
        {
            String mobName = entry.getKey();
            int kills = entry.getValue();

            // Only count kills for non-exempt monsters
            if (!NpcKillThreshold.isExempt(mobName)) {
                totalKills += kills;

                if (kills > mostKillCount)
                {
                    mostKilled = mobName;
                    mostKillCount = kills;
                }
            }
        }

        for (Map.Entry<String, Integer> entry : filteredThresholds.entrySet())
        {
            String mobName = entry.getKey();
            int threshold = entry.getValue();
            int kills = allKills.getOrDefault(mobName, 0);

            if (kills >= threshold)
            {
                totalCompleted++;
            }
            else if (kills > 0)
            {
                totalInProgress++;
            }
            else
            {
                totalNotStarted++;
            }
        }

        // Calculate overall progress
        float overallProgress = filteredThresholds.size() > 0 ? (float) totalCompleted / filteredThresholds.size() : 0;

        // Add stats
        addStatRow(statsPanel, "Total Monsters Tracked", String.valueOf(filteredThresholds.size()));
        addStatRow(statsPanel, "Total Kill Count", String.valueOf(totalKills));
        addStatRow(statsPanel, "Completed Monsters", totalCompleted + " (" +
                new DecimalFormat("#.#%").format(overallProgress) + ")");
        addStatRow(statsPanel, "In Progress", String.valueOf(totalInProgress));
        addStatRow(statsPanel, "Not Started", String.valueOf(totalNotStarted));

        if (!mostKilled.equals("None")) {
            addStatRow(statsPanel, "Most Killed Enemy", mostKilled + " (" + mostKillCount + " kills)");
        }

        // Overall progress bar
        statsPanel.add(createSpacerPanel(10));

        JShadowedLabel progressLabel = new JShadowedLabel("Overall Completion");
        progressLabel.setFont(FontManager.getRunescapeBoldFont());
        progressLabel.setForeground(Color.WHITE);
        statsPanel.add(progressLabel);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaximumValue(filteredThresholds.size());
        progressBar.setValue(totalCompleted);
        progressBar.setForeground(getColorForPercentage(overallProgress));
        progressBar.setLeftLabel(String.valueOf(totalCompleted));
        progressBar.setRightLabel(String.valueOf(filteredThresholds.size()));
        progressBar.setCenterLabel(new DecimalFormat("#.#%").format(overallProgress));
        statsPanel.add(progressBar);

        // Add reset button
        JPanel buttonPanel = new JPanel(new BorderLayout(0, 5));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        ColorJButton resetButton = new ColorJButton("Reset All Kill Counts", ColorScheme.DARK_GRAY_COLOR);
        resetButton.setFocusPainted(false);
        resetButton.setFont(FontManager.getRunescapeSmallFont());
        resetButton.addActionListener((ActionEvent e) -> {
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
                dispose();
                open(); // Reopen with fresh data
            }
        });

        buttonPanel.add(resetButton, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(statsPanel);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(16, 0));

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addStatRow(JPanel panel, String label, String value)
    {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JShadowedLabel nameLabel = new JShadowedLabel(label);
        nameLabel.setFont(FontManager.getRunescapeSmallFont());
        nameLabel.setForeground(Color.WHITE);

        JShadowedLabel valueLabel = new JShadowedLabel(value);
        valueLabel.setFont(FontManager.getRunescapeSmallFont());
        valueLabel.setForeground(Color.LIGHT_GRAY);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(nameLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);

        panel.add(row);
    }

    public boolean isOpen()
    {
        return isOpen;
    }

    // Helper class for sorting monsters by progress
    private static class MobProgress
    {
        private final String name;
        private final int kills;
        private final int threshold;

        public MobProgress(String name, int kills, int threshold)
        {
            this.name = name;
            this.kills = kills;
            this.threshold = threshold;
        }

        public String getName()
        {
            return name;
        }

        public int getKills()
        {
            return kills;
        }

        public int getThreshold()
        {
            return threshold;
        }

        public float getProgress()
        {
            return threshold > 0 ? (float) kills / threshold : 0;
        }
    }
}