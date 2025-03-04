package com.FiftyFifty;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Slf4j
public class EnemyHighlighter extends Overlay
{
    private final Client client;
    private final EnemyKillTracker killTracker;
    private final EnemyTrackerConfig config;
    
    private static final Color GRAY_OUT_COLOR = new Color(60, 60, 60, 180);
    private static final Color PROGRESS_COLOR = new Color(255, 140, 0, 120);
    
    @Inject
    public EnemyHighlighter(Client client, EnemyKillTracker killTracker, EnemyTrackerConfig config)
    {
        this.client = client;
        this.killTracker = killTracker;
        this.config = config;
        
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }
    
    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != net.runelite.api.GameState.LOGGED_IN)
        {
            return null;
        }
        
        for (NPC npc : client.getNpcs())
        {
            if (npc == null || npc.getName() == null)
            {
                continue;
            }
            
            String npcName = npc.getName();
            int currentKills = killTracker.getKills(npcName);
            int killThreshold = NpcKillThreshold.getThreshold(npcName);
            
            // Add progress visualization for NPCs being worked on
            if (currentKills > 0) {
                // Get hull shape for more precise highlighting
                Shape hull = npc.getConvexHull();
                if (hull != null) {
                    if (currentKills >= killThreshold) {
                        // Reached threshold - fill with dark gray
                        Composite originalComposite = graphics.getComposite();
                        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
                        graphics.setColor(GRAY_OUT_COLOR);
                        graphics.fill(hull);
                        graphics.setComposite(originalComposite);
                    } else if (config.showProgressIndicator()) {
                        // In progress - just outline with color
                        graphics.setColor(PROGRESS_COLOR);
                        graphics.draw(hull);
                    }
                }
            }
        }
        
        return null;
    }
}
