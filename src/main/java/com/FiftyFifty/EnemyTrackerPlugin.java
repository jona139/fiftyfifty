package com.FiftyFifty;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@PluginDescriptor(
    name = "Enemy Tracker",
    description = "Tracks enemy kills and grays them out after a specified threshold",
    tags = {"combat", "overlay", "pve", "kill", "tracker"}
)
public class EnemyTrackerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private EnemyTrackerConfig config;
    
    @Inject
    private OverlayManager overlayManager;
    
    @Inject
    private ConfigManager configManager;
    
    private EnemyKillTracker killTracker;
    private EnemyHighlighter highlighter;
    private RecentKillOverlay recentKillOverlay;
    private MenuEntrySwapper menuEntrySwapper;
    private ProgressDashboard progressDashboard;
    
    // Keep track of player interactions
    private final Map<NPC, Player> interactingMap = new HashMap<>();
    
    @Override
    protected void startUp() throws Exception
    {
        log.info("Enemy Tracker started!");
        
        killTracker = new EnemyKillTracker(configManager);
        highlighter = new EnemyHighlighter(client, killTracker, config);
        recentKillOverlay = new RecentKillOverlay(config, killTracker);
        menuEntrySwapper = new MenuEntrySwapper(client, killTracker, config);
        progressDashboard = new ProgressDashboard(killTracker, configManager);
        
        overlayManager.add(highlighter);
        overlayManager.add(recentKillOverlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("Enemy Tracker stopped!");
        
        overlayManager.remove(highlighter);
        overlayManager.remove(recentKillOverlay);
        
        if (progressDashboard != null && progressDashboard.isOpen())
        {
            progressDashboard.dispose();
        }
        
        interactingMap.clear();
    }
    
    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
        {
            interactingMap.clear();
        }
        
        if (config.resetKills())
        {
            killTracker.resetKills();
            // Reset the config option
            configManager.setConfiguration(EnemyTrackerConfig.class.getAnnotation(ConfigGroup.class).value(), "resetKills", false);
        }
        
        // Check if dashboard should be opened
        if (config.openDashboard() && !progressDashboard.isOpen())
        {
            progressDashboard.open();
            // Reset the config option
            configManager.setConfiguration(EnemyTrackerConfig.class.getAnnotation(ConfigGroup.class).value(), "openDashboard", false);
        }
    }
    
    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        menuEntrySwapper.onMenuEntryAdded(event);
    }
    
    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        
        if (event.getSource() instanceof Player && event.getTarget() instanceof NPC)
        {
            Player player = (Player) event.getSource();
            NPC npc = (NPC) event.getTarget();
            
            // Only track interactions for the local player
            if (player == client.getLocalPlayer())
            {
                interactingMap.put(npc, player);
            }
        }
    }
    
    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        NPC npc = event.getNpc();
        
        // If the NPC is dead and was being interacted with by the player
        if (npc.isDead() && interactingMap.containsKey(npc))
        {
            Player player = interactingMap.get(npc);
            
            // Check if the player who interacted with the NPC is the local player
            if (player == client.getLocalPlayer() && npc.getName() != null)
            {
                String npcName = npc.getName();
                killTracker.addKill(npcName);
                
                // Update the recent kill overlay
                recentKillOverlay.setRecentKill(npcName);
                
                log.debug("Killed {}, count: {}/{}", 
                    npcName, 
                    killTracker.getKills(npcName),
                    NpcKillThreshold.getThreshold(npcName));
            }
            
            // Remove the NPC from the tracking map
            interactingMap.remove(npc);
        }
    }
    
    @Provides
    EnemyTrackerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(EnemyTrackerConfig.class);
    }
}
