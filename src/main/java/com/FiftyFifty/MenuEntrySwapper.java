package com.FiftyFifty;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

/**
 * Class responsible for managing NPC attack options
 */
@Slf4j
public class MenuEntrySwapper
{
    private final Client client;
    private final EnemyKillTracker killTracker;
    private final EnemyTrackerConfig config;
    private final ClientThread clientThread;
    
    @Inject
    public MenuEntrySwapper(Client client, EnemyKillTracker killTracker, EnemyTrackerConfig config, ClientThread clientThread)
    {
        this.client = client;
        this.killTracker = killTracker;
        this.config = config;
        this.clientThread = clientThread;
    }
    
    /**
     * Handles the menu entry added event, used for modifying right-click options
     */
    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (!config.hideAttackOption())
        {
            return;
        }
        
        String option = event.getOption().toLowerCase();
        if (!(option.equals("attack") || option.equals("fight") || option.startsWith("cast")))
        {
            return;
        }
        
        // Try to find the NPC
        NPC npc = findNpcById(event.getIdentifier());
        if (npc == null || npc.getName() == null)
        {
            return;
        }
        
        String npcName = npc.getName();
        
        // Check if this monster is exempt from kill limits
        if (NpcKillThreshold.isExempt(npcName)) {
            return; // Allow the attack option
        }
        
        int threshold = NpcKillThreshold.getThreshold(npcName);
        int currentKills = killTracker.getKills(npcName);
        
        if (killTracker.hasReachedThreshold(npcName, threshold))
        {
            // Remove all attack options for this NPC
            MenuEntry[] menuEntries = client.getMenuEntries();
            for (MenuEntry entry : menuEntries)
            {
                if (entry.getIdentifier() == event.getIdentifier() && 
                    isAttackOption(entry.getOption()))
                {
                    entry.setOption("");
                    entry.setTarget("");
                }
            }
        }
    }
    
    /**
     * Handles menu option click events, used for preventing left-click attacks
     */
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!config.hideAttackOption())
        {
            return;
        }

        // Get attack-related info from the event
        String option = event.getMenuOption().toLowerCase();
        MenuAction action = event.getMenuAction();
        int id = event.getId();
        
        // Check if this is an NPC interaction
        boolean isNpcAction = action == MenuAction.NPC_FIRST_OPTION || 
                             action == MenuAction.NPC_SECOND_OPTION || 
                             action == MenuAction.NPC_THIRD_OPTION || 
                             action == MenuAction.NPC_FOURTH_OPTION || 
                             action == MenuAction.NPC_FIFTH_OPTION;
        
        boolean isAttack = option.equals("attack") || option.equals("fight") || 
                          option.startsWith("cast");
        
        if (isNpcAction && isAttack)
        {
            // Try to find the NPC
            NPC npc = findNpcById(id);
            if (npc == null)
            {
                // Fallback: try to find the NPC by target name
                String targetName = event.getMenuTarget();
                
                for (NPC n : client.getNpcs())
                {
                    if (n != null && n.getName() != null && targetName.contains(n.getName()))
                    {
                        npc = n;
                        break;
                    }
                }
                
                if (npc == null)
                {
                    return;
                }
            }
            
            if (npc.getName() == null)
            {
                return;
            }
            
            String npcName = npc.getName();
            
            // Check if this monster is exempt from kill limits
            if (NpcKillThreshold.isExempt(npcName)) {
                return; // Allow the attack to proceed
            }
            
            int threshold = NpcKillThreshold.getThreshold(npcName);
            int currentKills = killTracker.getKills(npcName);
            
            if (killTracker.hasReachedThreshold(npcName, threshold))
            {
                // Cancel the click and show a message
                event.consume();
                
                clientThread.invoke(() -> {
                    client.addChatMessage(
                        net.runelite.api.ChatMessageType.GAMEMESSAGE,
                        "",
                        "You've already reached the kill threshold for " + npcName + ".",
                        null
                    );
                });
            }
        }
    }
    
    /**
     * Helper method to find NPC by ID
     */
    private NPC findNpcById(int id)
    {
        for (NPC npc : client.getNpcs())
        {
            if (npc != null && npc.getIndex() == id)
            {
                return npc;
            }
        }
        
        return null;
    }
    
    /**
     * Helper method to check if an option is attack-related
     */
    private boolean isAttackOption(String option)
    {
        if (option == null)
        {
            return false;
        }
        
        option = option.toLowerCase();
        return option.equals("attack") || 
               option.equals("fight") || 
               option.startsWith("cast");
    }
}