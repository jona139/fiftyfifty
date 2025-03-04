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
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

@Slf4j
public class ProgressDashboard extends JFrame
{
    private final EnemyKillTracker killTracker;
    private final ConfigManager configManager;
    
    private boolean isOpen = false;
    
    private static final int DASHBOARD_WIDTH = 400;
    private static final int DASHBOARD_HEIGHT = 600;
    
    @Inject
    public ProgressDashboard(EnemyKillTracker killTracker, ConfigManager configManager)
    {
        this.killTracker = killTracker;
        this.configManager = configManager;
        
        setTitle("Kill Progress Dashboard");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(DASHBOARD_WIDTH, DASHBOARD_HEIGHT);
        setLocationRelativeTo(null);
        
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
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("In Progress", createProgressPanel());
        tabbedPane.add("All Monsters", createAllMonstersPanel());
        tabbedPane.add("Statistics", createStatsPanel());
        
        getContentPane().add(tabbedPane);
        
        pack();
        setVisible(true);
    }
    
    private JPanel createProgressPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(DASHBOARD_WIDTH - 20, DASHBOARD_HEIGHT - 80));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Get all kills
        Map<String, Integer> allKills = killTracker.getAllKills();
        
        // Track monsters in progress
        List<MobProgress> inProgress = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : allKills.entrySet())
        {
            String mobName = entry.getKey();
            int kills = entry.getValue();
            int threshold = NpcKillThreshold.getThreshold(mobName);
            
            if (kills > 0 && kills < threshold)
            {
                inProgress.add(new MobProgress(mobName, kills, threshold));
            }
        }
        
        // Sort by progress percentage (descending)
        Collections.sort(inProgress);
        
        // Create in-progress panel
        JPanel progressCardsPanel = new JPanel();
        progressCardsPanel.setLayout(new GridLayout(0, 1, 0, 10));
        
        if (inProgress.isEmpty())
        {
            JLabel emptyLabel = new JLabel("No monsters in progress", SwingConstants.CENTER);
            emptyLabel.setFont(FontManager.getRunescapeFont());
            progressCardsPanel.add(emptyLabel);
        }
        else
        {
            for (MobProgress mob : inProgress)
            {
                progressCardsPanel.add(createProgressCard(mob));
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(progressCardsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createProgressCard(MobProgress mob)
    {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        card.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
        
        // Title
        JLabel nameLabel = new JLabel(mob.getName());
        nameLabel.setFont(FontManager.getRunescapeBoldFont());
        nameLabel.setForeground(Color.WHITE);
        card.add(nameLabel, BorderLayout.NORTH);
        
        // Progress bar
        JProgressBar progressBar = new JProgressBar(0, mob.getThreshold());
        progressBar.setValue(mob.getKills());
        float percentage = (float) mob.getKills() / mob.getThreshold();
        
        // Color based on progress
        if (percentage < 0.3)
            progressBar.setForeground(new Color(255, 60, 60)); // Red
        else if (percentage < 0.7)
            progressBar.setForeground(new Color(255, 140, 0)); // Orange
        else
            progressBar.setForeground(new Color(0, 180, 0));   // Green
            
        progressBar.setStringPainted(true);
        progressBar.setString(mob.getKills() + " / " + mob.getThreshold());
        card.add(progressBar, BorderLayout.CENTER);
        
        // Percentage
        DecimalFormat df = new DecimalFormat("#.##%");
        JLabel percentLabel = new JLabel(df.format(percentage), SwingConstants.RIGHT);
        percentLabel.setFont(FontManager.getRunescapeSmallFont());
        card.add(percentLabel, BorderLayout.EAST);
        
        return card;
    }
    
    private JPanel createAllMonstersPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(DASHBOARD_WIDTH - 20, DASHBOARD_HEIGHT - 80));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create column names
        String[] columnNames = {"Monster", "Kills", "Required", "Progress"};
        
        // Get all predefined monsters
        Map<String, Integer> thresholds = NpcKillThreshold.getNpcThresholds();
        Map<String, Integer> allKills = killTracker.getAllKills();
        
        // Create data
        Object[][] data = new Object[thresholds.size()][4];
        int i = 0;
        
        for (Map.Entry<String, Integer> entry : thresholds.entrySet())
        {
            String mobName = entry.getKey();
            int threshold = entry.getValue();
            int kills = allKills.getOrDefault(mobName, 0);
            
            data[i][0] = mobName;
            data[i][1] = kills;
            data[i][2] = threshold;
            
            // Calculate percentage
            float percentage = (float) kills / threshold;
            DecimalFormat df = new DecimalFormat("#.##%");
            data[i][3] = df.format(percentage);
            
            i++;
        }
        
        // Create table model
        DefaultTableModel model = new DefaultTableModel(data, columnNames)
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
        JTable table = new JTable(model);
        table.setRowHeight(25);
        table.setFont(FontManager.getRunescapeSmallFont());
        table.getTableHeader().setReorderingAllowed(false);
        
        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        
        // Sortable (click on column header)
        table.setAutoCreateRowSorter(true);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Filter controls
        JPanel filterPanel = new JPanel(new BorderLayout(5, 0));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JLabel filterLabel = new JLabel("Filter: ");
        filterLabel.setFont(FontManager.getRunescapeSmallFont());
        
        String[] filterOptions = {"All", "In Progress", "Completed", "Not Started"};
        JComboBox<String> filterBox = new JComboBox<>(filterOptions);
        filterBox.addActionListener((ActionEvent e) -> {
            String selection = (String) filterBox.getSelectedItem();
            DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
            tableModel.setRowCount(0);
            
            for (Map.Entry<String, Integer> entry : thresholds.entrySet())
            {
                String mobName = entry.getKey();
                int threshold = entry.getValue();
                int kills = allKills.getOrDefault(mobName, 0);
                
                boolean include = false;
                
                switch (selection)
                {
                    case "All":
                        include = true;
                        break;
                    case "In Progress":
                        include = kills > 0 && kills < threshold;
                        break;
                    case "Completed":
                        include = kills >= threshold;
                        break;
                    case "Not Started":
                        include = kills == 0;
                        break;
                }
                
                if (include)
                {
                    float percentage = (float) kills / threshold;
                    DecimalFormat df = new DecimalFormat("#.##%");
                    tableModel.addRow(new Object[]{
                        mobName, kills, threshold, df.format(percentage)
                    });
                }
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(DASHBOARD_WIDTH - 20, DASHBOARD_HEIGHT - 80));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel statsPanel = new JPanel(new GridLayout(0, 1, 0, 10));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Gather stats
        Map<String, Integer> allKills = killTracker.getAllKills();
        Map<String, Integer> thresholds = NpcKillThreshold.getNpcThresholds();
        
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
            
            totalKills += kills;
            
            if (kills > mostKillCount)
            {
                mostKilled = mobName;
                mostKillCount = kills;
            }
        }
        
        for (Map.Entry<String, Integer> entry : thresholds.entrySet())
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
        float overallProgress = (float) totalCompleted / thresholds.size();
        
        // Add stats
        addStatRow(statsPanel, "Total Monsters Tracked", String.valueOf(thresholds.size()));
        addStatRow(statsPanel, "Total Kill Count", String.valueOf(totalKills));
        addStatRow(statsPanel, "Completed Monsters", totalCompleted + " (" + 
            new DecimalFormat("#.##%").format(overallProgress) + ")");
        addStatRow(statsPanel, "In Progress", String.valueOf(totalInProgress));
        addStatRow(statsPanel, "Not Started", String.valueOf(totalNotStarted));
        addStatRow(statsPanel, "Most Killed Enemy", mostKilled + " (" + mostKillCount + " kills)");
        
        // Add reset button
        JButton resetButton = new JButton("Reset All Kill Counts");
        resetButton.addActionListener((ActionEvent e) -> {
            killTracker.resetKills();
            JLabel confirmLabel = new JLabel("All kill counts have been reset!", SwingConstants.CENTER);
            confirmLabel.setForeground(Color.RED);
            panel.add(confirmLabel, BorderLayout.SOUTH);
            panel.revalidate();
            panel.repaint();
            
            // Refresh the UI to show the reset
            SwingUtilities.invokeLater(() -> {
                dispose();
                open();
            });
        });
        
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        buttonPanel.add(resetButton, BorderLayout.CENTER);
        
        JScrollPane scrollPane = new JScrollPane(statsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void addStatRow(JPanel panel, String label, String value)
    {
        JPanel rowPanel = new JPanel(new BorderLayout(5, 0));
        rowPanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
        rowPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.LIGHT_GRAY_COLOR),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(FontManager.getRunescapeBoldFont());
        
        JLabel valueLabel = new JLabel(value, SwingConstants.RIGHT);
        valueLabel.setFont(FontManager.getRunescapeFont());
        
        rowPanel.add(nameLabel, BorderLayout.WEST);
        rowPanel.add(valueLabel, BorderLayout.EAST);
        
        panel.add(rowPanel);
    }
    
    // Helper class for sorting monsters by progress
    private static class MobProgress implements Comparable<MobProgress>
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
            return (float) kills / threshold;
        }
        
        @Override
        public int compareTo(MobProgress other)
        {
            // Sort by progress (descending)
            return Float.compare(other.getProgress(), this.getProgress());
        }
    }
    
    public boolean isOpen()
    {
        return isOpen;
    }
}
