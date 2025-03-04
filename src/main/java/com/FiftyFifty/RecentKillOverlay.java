package com.FiftyFifty;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Slf4j
public class RecentKillOverlay extends Overlay {
    
    private final EnemyTrackerConfig config;
    private final EnemyKillTracker killTracker;
    private final PanelComponent panelComponent = new PanelComponent();
    
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
            .build());
        
        // Kill count
        int currentKills = killTracker.getKills(recentNpcName);
        int maxKills = NpcKillThreshold.getThreshold(recentNpcName);
        
        Color countColor = currentKills >= maxKills ? Color.RED : Color.WHITE;
        
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Kills:")
            .right(currentKills + "/" + maxKills)
            .rightColor(countColor)
            .build());
        
        // Progress
        StringBuilder progressBar = new StringBuilder("[");
        int progressChars = 10;
        int filledChars = (int) Math.min(progressChars, Math.round((double) currentKills / maxKills * progressChars));
        
        for (int i = 0; i < progressChars; i++) {
            if (i < filledChars) {
                progressBar.append("=");
            } else {
                progressBar.append("-");
            }
        }
        progressBar.append("]");
        
        panelComponent.getChildren().add(LineComponent.builder()
            .left("Progress:")
            .right(progressBar.toString())
            .build());
        
        // Status message
        if (currentKills >= maxKills) {
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Status:")
                .right("MAXED - Greyed Out")
                .rightColor(Color.RED)
                .build());
        }
        
        panelComponent.setPreferredSize(new Dimension(200, 100));
        
        return panelComponent.render(graphics);
    }
}
