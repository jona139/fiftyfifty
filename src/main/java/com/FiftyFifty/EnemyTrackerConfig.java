package com.FiftyFifty;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("enemytracker")
public interface EnemyTrackerConfig extends Config
{
    @ConfigSection(
        name = "Display Options",
        description = "Configure how the plugin displays information",
        position = 0
    )
    String displayOptions = "displayOptions";
    
    @ConfigSection(
        name = "NPC Options",
        description = "Configure specific NPC behavior",
        position = 1
    )
    String npcOptions = "npcOptions";
    
    @ConfigSection(
        name = "Dashboard",
        description = "View your overall progress",
        position = 2
    )
    String dashboardOptions = "dashboardOptions";
    
    // Progress indicator removed as it's no longer needed
    
    @ConfigItem(
        keyName = "showRecentKillOverlay",
        name = "Show Recent Kill Overlay",
        description = "Display information about your most recent kill",
        section = displayOptions
    )
    default boolean showRecentKillOverlay()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "hideAttackOption",
        name = "Hide Attack Option on Maxed NPCs",
        description = "Remove the attack option for NPCs that have reached max kill count",
        section = npcOptions
    )
    default boolean hideAttackOption()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "useGlobalThresholds",
        name = "Use Preset NPC Thresholds",
        description = "Use the preset kill thresholds for specific NPCs",
        section = npcOptions
    )
    default boolean useGlobalThresholds()
    {
        return true;
    }
    
    @ConfigItem(
        keyName = "defaultKillThreshold",
        name = "Default Kill Threshold",
        description = "The default number of kills required to gray out an NPC (if not specifically defined)",
        section = npcOptions
    )
    @Range(min = 1, max = 1000)
    default int defaultKillThreshold()
    {
        return 10;
    }
    
    @ConfigItem(
        keyName = "resetKills",
        name = "Reset All Kills",
        description = "Reset the kill count for all NPCs",
        section = npcOptions
    )
    default boolean resetKills()
    {
        return false;
    }
    
    @ConfigItem(
        keyName = "openDashboard",
        name = "Open Progress Dashboard",
        description = "Show the progress dashboard with all monsters and your current kill counts",
        section = dashboardOptions
    )
    default boolean openDashboard()
    {
        return false;
    }
}