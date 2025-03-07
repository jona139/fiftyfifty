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
        // Non-exempt Monsters
        monsterDrops.put("Goblin", new MonsterDrop("Goblin", "Grimy Dwarf Weed", 1.0/2731.0));

        // Exempt monsters - always drop the same items
        // Using special drop rate of -1 to indicate an exempt monster
        monsterDrops.put("Cow", new MonsterDrop("Cow", "Cowhide (Always drops)", -1));

        // Generated custom monster definitions
        monsterDrops.put("Imp", new MonsterDrop("Imp", "Potion", 1.0/128.0));
        monsterDrops.put("Giant rat", new MonsterDrop("Giant rat", "N/A", -1));
        monsterDrops.put("Rat", new MonsterDrop("Rat", "N/A", -1));
        monsterDrops.put("Giant spider", new MonsterDrop("Giant spider", "N/A", -1));
        monsterDrops.put("Chicken", new MonsterDrop("Chicken", "Feather (15)", 1.0/5.0));
        monsterDrops.put("Man", new MonsterDrop("Man", "Grimy Dwarf Weed", 1.0/237.0));
        monsterDrops.put("Woman", new MonsterDrop("Woman", "Grimy Dwarf Weed", 1.0/237.0));
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

    /**
     * Get custom monsters that aren't in the predefined list
     *
     * @return A map of custom monster names to their drop names, excluding ones already in the predefined list
     */
    public static Map<String, String> getUniqueCustomMonsters() {
        Map<String, String> uniqueCustoms = new HashMap<>();

        for (Map.Entry<String, String> entry : customDrops.entrySet()) {
            String monsterName = entry.getKey();

            // Only include monsters that aren't already in the predefined list
            if (!monsterDrops.containsKey(monsterName)) {
                uniqueCustoms.put(monsterName, entry.getValue());
            }
        }

        return uniqueCustoms;
    }

    /**
     * Export custom monsters as code that can be added to the static initializer
     */
    public static String exportCustomMonstersAsCode() {
        StringBuilder codeBuilder = new StringBuilder();
        codeBuilder.append("// Generated custom monster definitions\n");

        // Export the custom monsters in the format used in the static initializer
        for (Map.Entry<String, String> entry : customDrops.entrySet()) {
            String monsterName = entry.getKey();
            String dropName = entry.getValue();

            // Skip monsters that are already in the predefined list
            if (monsterDrops.containsKey(monsterName)) {
                continue;
            }

            // Get the threshold or exempt status
            boolean isExempt = exemptMonsters.getOrDefault(monsterName, false);
            int threshold = customThresholds.getOrDefault(monsterName, 10);

            // Calculate drop rate from threshold or use -1 for exempt monsters
            double dropRate;
            if (isExempt || threshold == Integer.MAX_VALUE) {
                dropRate = -1;
            } else {
                // Reverse-engineer the drop rate from the threshold
                // Using the formula: p = 1 - (0.5)^(1/n)
                // Where n is the threshold and p is the drop rate
                dropRate = 1 - Math.pow(0.5, 1.0 / threshold);
            }

            // Format the line of code
            codeBuilder.append("monsterDrops.put(\"")
                    .append(monsterName)
                    .append("\", new MonsterDrop(\"")
                    .append(monsterName)
                    .append("\", \"")
                    .append(dropName)
                    .append("\", ");

            if (dropRate == -1) {
                codeBuilder.append("-1");
            } else {
                // Format as 1.0/X.0 to match the original code style
                double denominator = 1.0 / dropRate;
                codeBuilder.append("1.0/").append(Math.round(denominator)).append(".0");
            }

            codeBuilder.append("));\n");
        }

        return codeBuilder.toString();
    }
}