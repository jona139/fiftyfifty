package com.FiftyFifty;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.util.ImageUtil;

/**
 * Component for displaying enemy kill progress inline with game UI
 */
public class KillProgressComponent implements LayoutableRenderableEntity {

    private static final int BORDER_SIZE = 5;
    private static final int HEIGHT = 30;
    private static final int HORIZONTAL_PADDING = 8;
    
    private String enemyName;
    private int currentKills;
    private int killThreshold;
    
    private final BufferedImage background =
        ImageUtil.loadImageResource(FiftyFiftyPanel.class, "background.png");
    
    @Getter private final Rectangle bounds = new Rectangle();
    @Setter private Point preferredLocation = new Point();
    
    public KillProgressComponent() {
        // Default constructor
    }
    
    public void setProgress(String enemyName, int currentKills, int killThreshold) {
        this.enemyName = enemyName;
        this.currentKills = currentKills;
        this.killThreshold = killThreshold;
    }
    
    @Override
    public void setPreferredSize(final Dimension dimension) {
        // Not needed - size is determined by content
    }
    
    @Override
    public Dimension render(final Graphics2D graphics) {
        if (enemyName == null) {
            return null;
        }
        
        // Calculate dimensions based on text length
        final FontMetrics metrics = graphics.getFontMetrics(FontManager.getRunescapeSmallFont());
        final int textWidth = metrics.stringWidth(enemyName + ": " + currentKills + "/" + killThreshold);
        final int totalWidth = textWidth + HORIZONTAL_PADDING * 2;
        
        // Draw background
        if (background != null) {
            graphics.drawImage(background, preferredLocation.x, preferredLocation.y, 
                totalWidth, HEIGHT, null);
        } else {
            // Fallback if image loading fails
            graphics.setColor(new Color(70, 61, 50, 225));
            graphics.fillRect(preferredLocation.x, preferredLocation.y, totalWidth, HEIGHT);
            graphics.setColor(Color.BLACK);
            graphics.drawRect(preferredLocation.x, preferredLocation.y, totalWidth, HEIGHT);
        }
        
        // Draw text
        graphics.setFont(FontManager.getRunescapeSmallFont());
        final int textY = preferredLocation.y + BORDER_SIZE + metrics.getHeight();
        
        // Shadow
        graphics.setColor(Color.BLACK);
        graphics.drawString(enemyName, preferredLocation.x + HORIZONTAL_PADDING + 1, textY + 1);
        
        // Actual text
        graphics.setColor(Color.WHITE);
        graphics.drawString(enemyName, preferredLocation.x + HORIZONTAL_PADDING, textY);
        
        // Draw progress bar
        final ProgressBarComponent progressBar = new ProgressBarComponent();
        progressBar.setBackgroundColor(new Color(61, 56, 49));
        progressBar.setMinimum(0);
        progressBar.setMaximum(killThreshold);
        progressBar.setValue(currentKills);
        
        // Calculate progress percentage
        float progressPercent = (float) currentKills / killThreshold;
        
        // Set color based on progress
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
        
        // Draw kill count text
        final String killText = currentKills + "/" + killThreshold;
        final int killTextWidth = metrics.stringWidth(killText);
        
        // Shadow
        graphics.setColor(Color.BLACK);
        graphics.drawString(killText, 
            preferredLocation.x + totalWidth - HORIZONTAL_PADDING - killTextWidth + 1, 
            textY + 1);
        
        // Actual text
        graphics.setColor(progressPercent >= 1.0f ? Color.RED : Color.WHITE);
        graphics.drawString(killText, 
            preferredLocation.x + totalWidth - HORIZONTAL_PADDING - killTextWidth, 
            textY);
        
        // Update bounds
        final Dimension dimension = new Dimension(totalWidth, HEIGHT);
        bounds.setLocation(preferredLocation);
        bounds.setSize(dimension);
        
        return dimension;
    }
}
