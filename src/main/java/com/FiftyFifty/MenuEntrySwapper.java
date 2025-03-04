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
        log.info("MenuEntrySwapper initialized"); // DEBUG
    }
    
    /**
     * Handles the menu entry added event, used for modifying right-click options
     */
    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        log.info("MenuEntryAdded - Option: {}, Target: {}, ID: {}, Type: {}", 
            event.getOption(), event.getTarget(), event.getIdentifier(), event.getType()); // DEBUG

        if (!config.hideAttackOption())
        {
            log.info("Attack hiding is disabled in config"); // DEBUG
            return;
        }
        
        String option = event.getOption().toLowerCase();
        if (!(option.equals("attack") || option.equals("fight") || option.startsWith("cast")))
        {
            return;
        }

        log.info("Attack option detected: {}", option); // DEBUG
        
        // Try to find the NPC
        NPC npc = findNpcById(event.getIdentifier());
        if (npc == null)
        {
            log.info("Could not find NPC with ID: {}", event.getIdentifier()); // DEBUG
            return;
        }
        
        if (npc.getName() == null)
        {
            log.info("NPC has no name: {}", npc); // DEBUG
            return;
        }
        
        String npcName = npc.getName();
        int threshold = NpcKillThreshold.getThreshold(npcName);
        int currentKills = killTracker.getKills(npcName);
        
        log.info("NPC: {}, Kills: {}, Threshold: {}", npcName, currentKills, threshold); // DEBUG
        
        if (killTracker.hasReachedThreshold(npcName, threshold))
        {
            log.info("NPC has reached threshold, modifying menu entries"); // DEBUG
            
            // Remove all attack options for this NPC
            MenuEntry[] menuEntries = client.getMenuEntries();
            for (MenuEntry entry : menuEntries)
            {
                if (entry.getIdentifier() == event.getIdentifier() && 
                    isAttackOption(entry.getOption()))
                {
                    log.info("Removing menu entry: {}", entry.getOption()); // DEBUG
                    entry.setOption("");
                    entry.setTarget("");
                }
            }
            
            log.info("Removed attack option for maxed mob: {}", npcName); // DEBUG
        }
    }
    
    /**
     * Handles menu option click events, used for preventing left-click attacks
     */
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        log.info("MenuOptionClicked - Option: {}, Target: {}, ID: {}, MenuAction: {}", 
            event.getMenuOption(), event.getMenuTarget(), event.getId(), event.getMenuAction()); // DEBUG

        if (!config.hideAttackOption())
        {
            log.info("Attack hiding is disabled in config"); // DEBUG
            return;
        }

        // Get attack-related info from the event
        String option = event.getMenuOption().toLowerCase();
        MenuAction action = event.getMenuAction();
        int id = event.getId();
        
        log.info("Processing click - Option: {}, Action: {}, ID: {}", option, action, id); // DEBUG
        
        // Check if this is an NPC interaction
        boolean isNpcAction = action == MenuAction.NPC_FIRST_OPTION || 
                             action == MenuAction.NPC_SECOND_OPTION || 
                             action == MenuAction.NPC_THIRD_OPTION || 
                             action == MenuAction.NPC_FOURTH_OPTION || 
                             action == MenuAction.NPC_FIFTH_OPTION;
        
        boolean isAttack = option.equals("attack") || option.equals("fight") || 
                          option.startsWith("cast");
        
        log.info("Is NPC action: {}, Is attack: {}", isNpcAction, isAttack); // DEBUG
        
        if (isNpcAction && isAttack)
        {
            log.info("NPC attack detected: {} on ID {} with action {}", option, id, action); // DEBUG
            
            // We definitely want to get the right NPC here
            NPC npc = findNpcById(id);
            if (npc == null)
            {
                log.info("Could not find NPC with ID: {}", id); // DEBUG
                
                // Fallback: try to find the NPC by target name
                String targetName = event.getMenuTarget();
                log.info("Trying to find NPC by target name: {}", targetName); // DEBUG
                
                for (NPC n : client.getNpcs())
                {
                    if (n != null && n.getName() != null && targetName.contains(n.getName()))
                    {
                        npc = n;
                        log.info("Found NPC by name: {}", n.getName()); // DEBUG
                        break;
                    }
                }
                
                if (npc == null)
                {
                    log.info("Still could not find NPC, returning"); // DEBUG
                    return;
                }
            }
            
            if (npc.getName() == null)
            {
                log.info("NPC has no name: {}", npc); // DEBUG
                return;
            }
            
            String npcName = npc.getName();
            int threshold = NpcKillThreshold.getThreshold(npcName);
            int currentKills = killTracker.getKills(npcName);
            
            log.info("NPC: {}, Kills: {}, Threshold: {}", npcName, currentKills, threshold); // DEBUG
            
            if (killTracker.hasReachedThreshold(npcName, threshold))
            {
                log.info("NPC has reached threshold, consuming event"); // DEBUG
                
                // Cancel the click and show a message
                event.consume();
                
                clientThread.invoke(() -> {
                    client.addChatMessage(
                        net.runelite.api.ChatMessageType.GAMEMESSAGE,
                        "",
                        "You've already reached the kill threshold for " + npcName + ".",
                        null
                    );
                    
                    log.info("Added chat message about maxed threshold"); // DEBUG
                });
                
                log.info("Blocked attack on maxed mob: {}", npcName); // DEBUG
            }
            else 
            {
                log.info("NPC has NOT reached threshold yet"); // DEBUG
            }
        }
        else
        {
            log.info("Not an NPC attack action"); // DEBUG
        }
    }
    
    /**
     * Helper method to find NPC by ID
     */
    private NPC findNpcById(int id)
    {
        log.info("Looking for NPC with ID: {}", id); // DEBUG
        
        for (NPC npc : client.getNpcs())
        {
            if (npc != null && npc.getIndex() == id)
            {
                log.info("Found NPC: {} (Index: {})", npc.getName(), npc.getIndex()); // DEBUG
                return npc;
            }
        }
        
        log.info("No NPC found with ID: {}", id); // DEBUG
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
