package com.FiftyFifty;

import lombok.Getter;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to store and manage NPC kill thresholds
 */
public class NpcKillThreshold {
    
    @Getter
    private static final Map<String, Integer> npcThresholds = new HashMap<>();
    
    static {
        // Common low-level monsters
        npcThresholds.put("Goblin", 15);
        npcThresholds.put("Imp", 20);
        npcThresholds.put("Rat", 10);
        npcThresholds.put("Cow", 15);
        npcThresholds.put("Chicken", 10);
        
        // Mid-level monsters
        npcThresholds.put("Hill Giant", 25);
        npcThresholds.put("Moss Giant", 30);
        npcThresholds.put("Skeleton", 25);
        npcThresholds.put("Zombie", 30);
        npcThresholds.put("Bandit", 35);
        
        // Higher level monsters
        npcThresholds.put("Lesser Demon", 40);
        npcThresholds.put("Greater Demon", 50);
        npcThresholds.put("Black Demon", 60);
        npcThresholds.put("Hellhound", 45);
        npcThresholds.put("Abyssal Demon", 75);
        
        // Dragons
        npcThresholds.put("Blue Dragon", 50);
        npcThresholds.put("Red Dragon", 60);
        npcThresholds.put("Black Dragon", 70);
        npcThresholds.put("Green Dragon", 55);
        
        // Bosses (higher thresholds)
        npcThresholds.put("King Black Dragon", 100);
        npcThresholds.put("Kalphite Queen", 150);
        npcThresholds.put("Giant Mole", 80);
        npcThresholds.put("Dagannoth Rex", 90);
        npcThresholds.put("Dagannoth Prime", 90);
        npcThresholds.put("Dagannoth Supreme", 90);
    }
    
    /**
     * Get the kill threshold for a specific NPC.
     * 
     * @param npcName The name of the NPC
     * @return The threshold, or default value (10) if not specifically defined
     */
    public static int getThreshold(String npcName) {
        return npcThresholds.getOrDefault(npcName, 10);
    }
}
