package com.FiftyFifty;

import lombok.Getter;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to store and manage NPC kill thresholds based on drop rates
 * For each monster, stores the name, rarest drop, drop rate, and calculated kill threshold
 * where the threshold is the number of kills needed for a 50% chance to get the rarest drop
 */
public class NpcKillThreshold {
    
    /**
     * Inner class to store monster drop information
     */
    public static class MonsterDrop {
        @Getter private final String name;
        @Getter private final String rarestDrop;
        @Getter private final double dropRate; // As 1/x (e.g., 1/128)
        @Getter private final int killThreshold;
        
        public MonsterDrop(String name, String rarestDrop, double dropRate) {
            this.name = name;
            this.rarestDrop = rarestDrop;
            this.dropRate = dropRate;
            this.killThreshold = calculateThreshold(dropRate);
        }
        
        /**
         * Calculate the number of kills needed for a 50% chance to get the drop
         * Uses the formula: n = log(0.5) / log(1 - 1/x)
         * 
         * @param dropRate The drop rate as 1/x. Special value -1 indicates exempt monsters.
         * @return The number of kills needed for a 50% chance, or Integer.MAX_VALUE for exempt monsters
         */
        private int calculateThreshold(double dropRate) {
            // Special case: A drop rate of -1 indicates an exempt monster
            if (dropRate == -1) {
                return Integer.MAX_VALUE; // Effectively infinite kills allowed
            }
            
            // For 100% drop rates, also exempt the monster
            if (dropRate >= 1.0) {
                return Integer.MAX_VALUE;
            }
            
            // Ensure the drop rate is valid
            if (dropRate <= 0) {
                return 10; // Default fallback
            }
            
            // Calculate the threshold using the binomial formula
            // n = log(0.5) / log(1 - dropRate)
            double n = Math.log(0.5) / Math.log(1 - dropRate);
            
            // Round to the nearest integer
            return (int) Math.ceil(n);
        }
    }
    
    @Getter
    private static final Map<String, MonsterDrop> monsterDrops = new HashMap<>();
    
    static {
        // Common low-level monsters
        monsterDrops.put("Goblin", new MonsterDrop("Goblin", "Goblin Champion Scroll", 1.0/5000.0));
        monsterDrops.put("Imp", new MonsterDrop("Imp", "Imp Champion Scroll", 1.0/5000.0));
        monsterDrops.put("Rat", new MonsterDrop("Rat", "Giant Rat Tail", 1.0/100.0));
        
        // Exempt monsters - always drop the same items
        // Using special drop rate of -1 to indicate an exempt monster
        monsterDrops.put("Cow", new MonsterDrop("Cow", "Cowhide (Always drops)", -1));
        monsterDrops.put("Chicken", new MonsterDrop("Chicken", "Raw chicken (Always drops)", -1));
        
        // Mid-level monsters
        monsterDrops.put("Hill Giant", new MonsterDrop("Hill Giant", "Giant Key", 1.0/128.0));
        monsterDrops.put("Moss Giant", new MonsterDrop("Moss Giant", "Bryophyta's Essence", 1.0/118.0));
        monsterDrops.put("Skeleton", new MonsterDrop("Skeleton", "Skeleton Champion Scroll", 1.0/5000.0));
        monsterDrops.put("Zombie", new MonsterDrop("Zombie", "Zombie Champion Scroll", 1.0/5000.0));
        monsterDrops.put("Bandit", new MonsterDrop("Bandit", "Rogue's Purse", 1.0/150.0));
        
        // Higher level monsters
        monsterDrops.put("Lesser Demon", new MonsterDrop("Lesser Demon", "Lesser Demon Champion Scroll", 1.0/5000.0));
        monsterDrops.put("Greater Demon", new MonsterDrop("Greater Demon", "Greater Demon Champion Scroll", 1.0/5000.0));
        monsterDrops.put("Black Demon", new MonsterDrop("Black Demon", "Black Demon Head", 1.0/6000.0));
        monsterDrops.put("Hellhound", new MonsterDrop("Hellhound", "Smouldering Stone", 1.0/32768.0));
        monsterDrops.put("Abyssal Demon", new MonsterDrop("Abyssal Demon", "Abyssal Whip", 1.0/512.0));
        
        // Dragons
        monsterDrops.put("Blue Dragon", new MonsterDrop("Blue Dragon", "Draconic Visage", 1.0/10000.0));
        monsterDrops.put("Red Dragon", new MonsterDrop("Red Dragon", "Draconic Visage", 1.0/10000.0));
        monsterDrops.put("Black Dragon", new MonsterDrop("Black Dragon", "Draconic Visage", 1.0/5000.0));
        monsterDrops.put("Green Dragon", new MonsterDrop("Green Dragon", "Draconic Visage", 1.0/10000.0));
        
        // Bosses (higher thresholds)
        monsterDrops.put("King Black Dragon", new MonsterDrop("King Black Dragon", "Dragon Pickaxe", 1.0/1500.0));
        monsterDrops.put("Kalphite Queen", new MonsterDrop("Kalphite Queen", "Dragon Chain Body", 1.0/128.0));
        monsterDrops.put("Giant Mole", new MonsterDrop("Giant Mole", "Baby Mole Pet", 1.0/3000.0));
        monsterDrops.put("Dagannoth Rex", new MonsterDrop("Dagannoth Rex", "Berserker Ring", 1.0/128.0));
        monsterDrops.put("Dagannoth Prime", new MonsterDrop("Dagannoth Prime", "Seers Ring", 1.0/128.0));
        monsterDrops.put("Dagannoth Supreme", new MonsterDrop("Dagannoth Supreme", "Archers Ring", 1.0/128.0));
    }
    
    /**
     * Get the kill threshold for a specific NPC.
     * 
     * @param npcName The name of the NPC
     * @return The threshold, or default value (10) if not specifically defined
     */
    public static int getThreshold(String npcName) {
        return monsterDrops.containsKey(npcName) 
            ? monsterDrops.get(npcName).getKillThreshold() 
            : 10;
    }
    
    /**
     * Check if a monster is exempt from kill limits
     * 
     * @param npcName The name of the NPC
     * @return True if monster is exempt (can be killed infinitely), false otherwise
     */
    public static boolean isExempt(String npcName) {
        if (!monsterDrops.containsKey(npcName)) {
            return false; // Non-tracked monsters follow normal rules
        }
        
        return monsterDrops.get(npcName).getKillThreshold() == Integer.MAX_VALUE;
    }
    
    /**
     * Get the thresholds for all NPCs.
     * 
     * @return A map of NPC names to kill thresholds
     */
    public static Map<String, Integer> getNpcThresholds() {
        Map<String, Integer> thresholds = new HashMap<>();
        
        for (Map.Entry<String, MonsterDrop> entry : monsterDrops.entrySet()) {
            thresholds.put(entry.getKey(), entry.getValue().getKillThreshold());
        }
        
        return thresholds;
    }
    
    /**
     * Get detailed information about an NPC's rarest drop
     * 
     * @param npcName The name of the NPC
     * @return A MonsterDrop object, or null if not found
     */
    public static MonsterDrop getMonsterDropInfo(String npcName) {
        return monsterDrops.get(npcName);
    }
}
