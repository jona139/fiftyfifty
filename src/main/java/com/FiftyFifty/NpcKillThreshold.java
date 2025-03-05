package com.FiftyFifty;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import net.runelite.client.config.ConfigManager;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to store and manage NPC kill thresholds based on drop rates
 * For each monster, stores the name, rarest drop, drop rate, and calculated kill threshold
 * where the threshold is the number of kills needed for a 50% chance to get the rarest drop
 */
public class NpcKillThreshold {
    
    // Config keys for storing custom thresholds
    private static final String CONFIG_GROUP = "enemytracker";
    private static final String CUSTOM_THRESHOLDS_KEY = "customThresholds";
    private static final String CUSTOM_DROPS_KEY = "customDrops";
    private static final String EXEMPT_MONSTERS_KEY = "exemptMonsters";
    
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
    
    // Maps to store custom monster data
    private static Map<String, Integer> customThresholds = new HashMap<>();
    private static Map<String, String> customDrops = new HashMap<>();
    private static Map<String, Boolean> exemptMonsters = new HashMap<>();
    
    // Gson instance for serialization
    private static final Gson gson = new Gson();
    
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
     * Initialize the custom monster data from configuration
     */
    public static void loadCustomMonsters(ConfigManager configManager) {
        // Load custom thresholds
        String thresholdsJson = configManager.getConfiguration(CONFIG_GROUP, CUSTOM_THRESHOLDS_KEY);
        if (thresholdsJson != null && !thresholdsJson.isEmpty()) {
            Type type = new TypeToken<HashMap<String, Integer>>(){}.getType();
            customThresholds = gson.fromJson(thresholdsJson, type);
        }
        
        // Load custom drops
        String dropsJson = configManager.getConfiguration(CONFIG_GROUP, CUSTOM_DROPS_KEY);
        if (dropsJson != null && !dropsJson.isEmpty()) {
            Type type = new TypeToken<HashMap<String, String>>(){}.getType();
            customDrops = gson.fromJson(dropsJson, type);
        }
        
        // Load exempt monsters
        String exemptJson = configManager.getConfiguration(CONFIG_GROUP, EXEMPT_MONSTERS_KEY);
        if (exemptJson != null && !exemptJson.isEmpty()) {
            Type type = new TypeToken<HashMap<String, Boolean>>(){}.getType();
            exemptMonsters = gson.fromJson(exemptJson, type);
        }
    }
    
    /**
     * Add a new monster to the custom thresholds
     */
    public static void addCustomMonster(ConfigManager configManager, String npcName, String dropName, 
                                        double dropRate, boolean isExempt) {
        // Add the monster to the appropriate maps
        if (isExempt) {
            customThresholds.put(npcName, Integer.MAX_VALUE);
            exemptMonsters.put(npcName, true);
        } else {
            // Calculate threshold
            MonsterDrop drop = new MonsterDrop(npcName, dropName, dropRate);
            customThresholds.put(npcName, drop.getKillThreshold());
            exemptMonsters.put(npcName, false);
        }
        
        // Store the drop name
        customDrops.put(npcName, dropName);
        
        // Save to configuration
        saveCustomMonsters(configManager);
    }
    
    /**
     * Save custom monster data to configuration
     */
    private static void saveCustomMonsters(ConfigManager configManager) {
        // Save custom thresholds
        String thresholdsJson = gson.toJson(customThresholds);
        configManager.setConfiguration(CONFIG_GROUP, CUSTOM_THRESHOLDS_KEY, thresholdsJson);
        
        // Save custom drops
        String dropsJson = gson.toJson(customDrops);
        configManager.setConfiguration(CONFIG_GROUP, CUSTOM_DROPS_KEY, dropsJson);
        
        // Save exempt monsters
        String exemptJson = gson.toJson(exemptMonsters);
        configManager.setConfiguration(CONFIG_GROUP, EXEMPT_MONSTERS_KEY, exemptJson);
    }
    
    /**
     * Check if a monster is defined (either predefined or custom)
     */
    public static boolean isMonsterDefined(String npcName) {
        return monsterDrops.containsKey(npcName) || customThresholds.containsKey(npcName);
    }
    
    /**
     * Get the kill threshold for a specific NPC.
     * 
     * @param npcName The name of the NPC
     * @return The threshold, or default value (10) if not specifically defined
     */
    public static int getThreshold(String npcName) {
        // Check custom thresholds first
        if (customThresholds.containsKey(npcName)) {
            return customThresholds.get(npcName);
        }
        
        // Then check predefined thresholds
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
        // Check custom exempt monsters first
        if (exemptMonsters.containsKey(npcName)) {
            return exemptMonsters.get(npcName);
        }
        
        // Then check predefined exemptions
        if (!monsterDrops.containsKey(npcName)) {
            return false; // Non-tracked monsters follow normal rules
        }
        
        return monsterDrops.get(npcName).getKillThreshold() == Integer.MAX_VALUE;
    }
    
    /**
     * Get the thresholds for all NPCs (predefined and custom).
     * 
     * @return A map of NPC names to kill thresholds
     */
    public static Map<String, Integer> getNpcThresholds() {
        Map<String, Integer> thresholds = new HashMap<>();
        
        // Add predefined thresholds
        for (Map.Entry<String, MonsterDrop> entry : monsterDrops.entrySet()) {
            thresholds.put(entry.getKey(), entry.getValue().getKillThreshold());
        }
        
        // Add custom thresholds (will override predefined if there are duplicates)
        thresholds.putAll(customThresholds);
        
        return thresholds;
    }
    
    /**
     * Get detailed information about an NPC's rarest drop
     * 
     * @param npcName The name of the NPC
     * @return A MonsterDrop object, or null if not found
     */
    public static MonsterDrop getMonsterDropInfo(String npcName) {
        // Only handle predefined monsters with this method
        return monsterDrops.get(npcName);
    }
    
    /**
     * Get the name of the rarest drop for an NPC
     * 
     * @param npcName The name of the NPC
     * @return The name of the rarest drop, or "Unknown" if not found
     */
    public static String getRarestDropName(String npcName) {
        // Check custom drops first
        if (customDrops.containsKey(npcName)) {
            return customDrops.get(npcName);
        }
        
        // Then check predefined drops
        if (monsterDrops.containsKey(npcName)) {
            return monsterDrops.get(npcName).getRarestDrop();
        }
        
        return "Unknown";
    }
    
    /**
     * Reset all custom monster data
     */
    public static void resetCustomMonsters(ConfigManager configManager) {
        customThresholds.clear();
        customDrops.clear();
        exemptMonsters.clear();
        saveCustomMonsters(configManager);
    }
    
    /**
     * Get all custom monsters
     * 
     * @return A map of custom monster names to their drop names
     */
    public static Map<String, String> getCustomMonsters() {
        return new HashMap<>(customDrops);
    }
}
