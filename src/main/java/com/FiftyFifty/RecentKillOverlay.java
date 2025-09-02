package com.FiftyFifty;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.ImageUtil;

@Slf4j
public class RecentKillOverlay extends Overlay {

    private final EnemyTrackerConfig config;
    private final EnemyKillTracker killTracker;

    private String recentNpcName = null;
    private long lastKillTime = 0;
    private static final long DISPLAY_TIME = 8000; // 8 seconds

    private static final int OVERLAY_WIDTH = 150;
    // Adjusted height for the taller progress bar
    private static final int OVERLAY_HEIGHT_REGULAR = 74;
    private static final int OVERLAY_HEIGHT_EXEMPT = 35;
    private static final int PADDING = 8;
    private static final int CORNER_RADIUS = 8;

    private final BufferedImage backgroundImage;

    // --- Colors ---
    private static final Color BACKGROUND_COLOR = new Color(20, 20, 20, 230);
    private static final Color TITLE_COLOR = new Color(255, 215, 0); // Gold color for the NPC Name
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color OUTLINE_COLOR = new Color(0, 0, 0, 220);

    @Inject
    public RecentKillOverlay(EnemyTrackerConfig config, EnemyKillTracker killTracker) {
        this.config = config;
        this.killTracker = killTracker;
        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ABOVE_SCENE);

        this.backgroundImage = ImageUtil.loadImageResource(RecentKillOverlay.class, "/background.png");
        if (this.backgroundImage == null) {
            log.warn("Could not load background.png from resources. Check file location and path.");
        }
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

        if (System.currentTimeMillis() - lastKillTime > DISPLAY_TIME) {
            recentNpcName = null;
            return null;
        }

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        boolean isExempt = NpcKillThreshold.isExempt(recentNpcName);
        int currentOverlayHeight = isExempt ? OVERLAY_HEIGHT_EXEMPT : OVERLAY_HEIGHT_REGULAR;

        if (backgroundImage != null) {
            // Draw background image without the darkening layer
            graphics.drawImage(backgroundImage, 0, 0, OVERLAY_WIDTH, currentOverlayHeight, null);
        } else {
            graphics.setColor(BACKGROUND_COLOR);
            graphics.fillRoundRect(0, 0, OVERLAY_WIDTH, currentOverlayHeight, CORNER_RADIUS, CORNER_RADIUS);
        }

        int y = PADDING;

        if (isExempt) {
            y += 19;
            String exemptText = recentNpcName + " (Exempt)";
            Font exemptFont = FontManager.getRunescapeFont().deriveFont(Font.BOLD, 14f);
            drawBoldOutlinedString(graphics, exemptText, y, exemptFont, Color.CYAN);
        } else {
            Font primaryFont = FontManager.getRunescapeFont().deriveFont(Font.BOLD, 16f);

            // NPC Name
            y += 16;
            drawBoldOutlinedString(graphics, recentNpcName, y, primaryFont, TITLE_COLOR);

            int currentKills = killTracker.getKills(recentNpcName);
            int maxKills = NpcKillThreshold.getThreshold(recentNpcName);
            float progress = Math.min(1.0f, (float) currentKills / maxKills);

            // Kill count text
            y += 18;
            String killText = currentKills + " / " + maxKills;
            Color killTextColor = currentKills >= maxKills ? new Color(255, 80, 80) : TEXT_COLOR;
            drawBoldOutlinedString(graphics, killText, y, primaryFont, killTextColor);

            // Progress Bar
            y += 6;
            // Taller bar height to fit the font
            int barHeight = 16;
            int barWidth = OVERLAY_WIDTH - 2 * PADDING;
            int barX = PADDING;
            int barY = y;

            graphics.setColor(new Color(30, 30, 30, 180)); // Added some transparency
            graphics.fillRoundRect(barX, barY, barWidth, barHeight, 5, 5);

            int fillWidth = (int) (barWidth * progress);
            graphics.setColor(getProgressColor(progress));
            if (fillWidth > 0) {
                graphics.fillRoundRect(barX, barY, fillWidth, barHeight, 5, 5);
            }

            graphics.setColor(OUTLINE_COLOR);
            graphics.drawRoundRect(barX, barY, barWidth, barHeight, 5, 5);

            String percentText = (int) (progress * 100) + "%";
            // Adjusted y-position to better center the font in the taller bar
            drawBoldOutlinedString(graphics, percentText, barY + barHeight - 1, primaryFont, Color.WHITE);
        }

        return new Dimension(OVERLAY_WIDTH, currentOverlayHeight);
    }

    private void drawBoldOutlinedString(Graphics2D graphics, String text, int y, Font font, Color color) {
        graphics.setFont(font);
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        int x = (OVERLAY_WIDTH - textWidth) / 2;

        graphics.setColor(OUTLINE_COLOR);
        graphics.drawString(text, x + 1, y + 1);
        graphics.drawString(text, x - 1, y - 1);
        graphics.drawString(text, x + 1, y - 1);
        graphics.drawString(text, x - 1, y + 1);

        graphics.setColor(color);
        graphics.drawString(text, x, y);
    }

    private Color getProgressColor(float progress) {
        if (progress >= 1.0f) {
            return new Color(220, 40, 40); // Bright Red
        } else if (progress >= 0.75f) {
            return new Color(240, 150, 0); // Orange
        } else if (progress >= 0.5f) {
            return new Color(255, 215, 0); // Gold/Yellow
        } else {
            return new Color(60, 180, 60); // Green
        }
    }
}