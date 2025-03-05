package com.FiftyFifty;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;

@Slf4j
public class RecentKillOverlay extends OverlayPanel {
    
    private final EnemyTrackerConfig config;
    private final EnemyKillTracker killTracker;
    
    private String recentNpcName = null;
    private long lastKillTime = 0;
    private static final long DISPLAY_TIME = 10000; // 10 seconds in milliseconds
    
    @Inject
    public RecentKillOverlay(EnemyTrackerConfig config, EnemyKillTracker killTracker) {
        this.config = config;
        this.killTracker = killTracker;
        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.TOP_LEFT);
    }
    
    public void setRecentKill(String npcName) {
        this.recentNpcName = npcName;
        this.lastKillTime = System.currentTimeMillis();
    }
    
    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showRecentKillOverlay() || recentNpcName == null) {
            return null;
        }
        
        // Hide after display time expires
        if (System.currentTimeMillis() - lastKillTime > DISPLAY_TIME) {
            recentNpcName = null;
            return null;
        }
        
        panelComponent.getChildren().clear();
        
        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
            .text("Recent Kill")
            .color(Color.GREEN)
            .build());
        
        // NPC name
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Monster:")
            .right(recentNpcName)
            .leftColor(Color.WHITE)
            .rightColor(Color.WHITE)
            .build());
        
        // Check if this is an exempt monster
        boolean isExempt = NpcKillThreshold.isExempt(recentNpcName);
        
        // For exempt monsters, show a different message
        if (isExempt) {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Status:")
                .right("EXEMPT")
                .leftColor(Color.WHITE)
                .rightColor(Color.CYAN)
                .build());
                
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Note:")
                .right("Common drops only")
                .leftColor(Color.WHITE)
                .rightColor(Color.LIGHT_GRAY)
                .build());
        } else {
            // Kill count
            int currentKills = killTracker.getKills(recentNpcName);
            int maxKills = NpcKillThreshold.getThreshold(recentNpcName);
            
            Color countColor = currentKills >= maxKills ? Color.RED : Color.WHITE;
            
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Kills:")
                .right(currentKills + "/" + maxKills)
                .leftColor(Color.WHITE)
                .rightColor(countColor)
                .build());
            
            // Progress bar instead of text progress
            final ProgressBarComponent progressBar = new ProgressBarComponent();
            progressBar.setBackgroundColor(new Color(61, 56, 49));
            progressBar.setMinimum(0);
            progressBar.setMaximum(maxKills);
            progressBar.setValue(currentKills);
            
            // Set color based on progress
            float progressPercent = (float) currentKills / maxKills;
            
            if (progressPercent >= 1.0f) {
                progressBar.setForegroundColor(Color.RED);  // Maxed out - red
            } else if (progressPercent >= 0.75f) {
                progressBar.setForegroundColor(Color.decode("#aeff00"));  // Light green
            } else if (progressPercent >= 0.5f) {
                progressBar.setForegroundColor(Color.decode("#ffe500"));  // Yellow
            } else if (progressPercent >= 0.25f) {
                progressBar.setForegroundColor(Color.decode("#ffb600"));  // Orange
            } else {
                progressBar.setForegroundColor(Color.decode("#ea6600"));  // Dark orange
            }
            
            panelComponent.getChildren().add(progressBar);
            
            // Status message
            if (currentKills >= maxKills) {
                panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right("MAXED OUT")
                    .leftColor(Color.WHITE)
                    .rightColor(Color.RED)
                    .build());
            }
        }
        
        panelComponent.setPreferredSize(new Dimension(200, 100));
        return super.render(graphics);
    }
}
