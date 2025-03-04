package com.FiftyFifty;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;

/**
 * Class responsible for hiding attack options for NPCs that have reached max kill count
 */
@Slf4j
public class MenuEntrySwapper
{
    private final Client client;
    private final EnemyKillTracker killTracker;
    private final EnemyTrackerConfig config;
    
    @Inject
    public MenuEntrySwapper(Client client, EnemyKillTracker killTracker, EnemyTrackerConfig config)
    {
        this.client = client;
        this.killTracker = killTracker;
        this.config = config;
    }
    
    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (!config.hideAttackOption())
        {
            return;
        }
        
        // Check the option directly instead of relying on specific action IDs
        String option = event.getOption().toLowerCase();
        
        // Check if this is an attack-related option
        if (!(option.equals("attack") || option.equals("fight") || option.startsWith("cast")))
        {
            return;
        }
        
        NPC npc = null;
        for (NPC n : client.getNpcs())
        {
            if (n != null && n.getIndex() == event.getIdentifier())
            {
                npc = n;
                break;
            }
        }
        
        if (npc != null && npc.getName() != null)
        {
            String npcName = npc.getName();
            int threshold = NpcKillThreshold.getThreshold(npcName);
            
            if (killTracker.hasReachedThreshold(npcName, threshold))
            {
                // Remove this menu entry by moving it off-screen
                MenuEntry[] menuEntries = client.getMenuEntries();
                for (MenuEntry entry : menuEntries)
                {
                    if (entry.getIdentifier() == event.getIdentifier() && 
                        entry.getOption().equals(event.getOption()))
                    {
                        // This effectively hides the option
                        entry.setTarget("");
                        entry.setOption("");
                        break;
                    }
                }
            }
        }
    }
}
