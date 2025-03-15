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
        monsterDrops.put("Twisted banshee", new MonsterDrop("Twisted banshee", "Mystic gloves", 1.0/257.0));
        monsterDrops.put("Banshee", new MonsterDrop("Banshee", "Mystic gloves (dark)", 1.0/513.0));
        monsterDrops.put("Lizardman", new MonsterDrop("Lizardman", "Xeric's talisman (inert)", 1.0/250.0));
        monsterDrops.put("Greater demon", new MonsterDrop("Greater demon", "Rune full helm", 1.0/129.0));
        monsterDrops.put("Gargoyle", new MonsterDrop("Gargoyle", "Mystic Robe Top (dark)", 1.0/513.0));
        monsterDrops.put("Cave goblin guard", new MonsterDrop("Cave goblin guard", "Bone club", 1.0/6.0));
        monsterDrops.put("Ancient Zygomite", new MonsterDrop("Ancient Zygomite", "Redwood tree seed", 1.0/7376.0));
        monsterDrops.put("Kalphite soldier", new MonsterDrop("Kalphite soldier", "Grimy dwarf weed", 1.0/5461.0));
        monsterDrops.put("Mountain troll", new MonsterDrop("Mountain troll", "Torstol seed", 1.0/7061.0));
        monsterDrops.put("Wyrm", new MonsterDrop("Wyrm", "Dragon thrownaxe", 1.0/10001.0));
        monsterDrops.put("Nechryael", new MonsterDrop("Nechryael", "Rune boots", 1.0/117.0));
        monsterDrops.put("Zygomite", new MonsterDrop("Zygomite", "Potato seed", 1.0/138.0));
        monsterDrops.put("Greater nechryael", new MonsterDrop("Greater nechryael", "Rune boots", 1.0/129.0));
        monsterDrops.put("Lizardman shaman", new MonsterDrop("Lizardman shaman", "Dragon warhammer", 1.0/3001.0));
        monsterDrops.put("Chilled jelly", new MonsterDrop("Chilled jelly", "Mithril boots", 1.0/65.0));
        monsterDrops.put("Dust devil", new MonsterDrop("Dust devil", "Dragon chainbody", 1.0/32768.0));
        monsterDrops.put("Cave bug", new MonsterDrop("Cave bug", "Grimy dwarf weed", 1.0/288.0));
        monsterDrops.put("Kalphite worker", new MonsterDrop("Kalphite worker", "Grimy dwarf weed", 1.0/781.0));
        monsterDrops.put("Mithril Dragon", new MonsterDrop("Mithril Dragon", "Dragon full helm", 1.0/32768.0));
        monsterDrops.put("Smoke devil", new MonsterDrop("Smoke devil", "Dragon chainbody", 1.0/32768.0));
        monsterDrops.put("Sourhog", new MonsterDrop("Sourhog", "Torstol seed", 1.0/12810.0));
        monsterDrops.put("Hellhound", new MonsterDrop("Hellhound", "Smouldering stone", 1.0/32768.0));
        monsterDrops.put("Waterfiend", new MonsterDrop("Waterfiend", "Mist battlestaff", 1.0/3001.0));
        monsterDrops.put("Skeleton mage", new MonsterDrop("Skeleton mage", "Grimy dwarf weed", 1.0/456.0));
        monsterDrops.put("Steel Dragon", new MonsterDrop("Steel Dragon", "Dragon platelegs", 1.0/513.0));
        monsterDrops.put("Vyrewatch", new MonsterDrop("Vyrewatch", "Torstol seed", 1.0/1050001.0));
        monsterDrops.put("Fire giant", new MonsterDrop("Fire giant", "Grimy dwarf weed", 1.0/288.0));
        monsterDrops.put("Sulphur Nagua", new MonsterDrop("Sulphur Nagua", "Sulphur blades", 1.0/451.0));
        monsterDrops.put("Icefiend", new MonsterDrop("Icefiend", "Air rune", 1.0/129.0));
        monsterDrops.put("Warped Terrorbird", new MonsterDrop("Warped Terrorbird", "Warped Sceptre (uncharged)", 1.0/321.0));
        monsterDrops.put("Cave goblin", new MonsterDrop("Cave goblin", "Brass necklace", 1.0/51.0));
        monsterDrops.put("Small lizard", new MonsterDrop("Small lizard", "Mystic gloves (light)", 1.0/513.0));
        monsterDrops.put("Gang boss", new MonsterDrop("Gang boss", "Rune scimitar", 1.0/81.0));
        monsterDrops.put("Brutal red dragon", new MonsterDrop("Brutal red dragon", "Rune platebody", 1.0/129.0));
        monsterDrops.put("Warped jelly", new MonsterDrop("Warped jelly", "Mithril boots", 1.0/65.0));
        monsterDrops.put("Skeleton", new MonsterDrop("Skeleton", "Grimy dwarf weed", 1.0/273.0));
        monsterDrops.put("Thermonuclear smoke devil", new MonsterDrop("Thermonuclear smoke devil", "Dragon chainbody", 1.0/2000.0));
        monsterDrops.put("Cave crawler", new MonsterDrop("Cave crawler", "Bronze boots", 1.0/129.0));
        monsterDrops.put("Crawling hand", new MonsterDrop("Crawling hand", "Teal gloves", 1.0/65.0));
        monsterDrops.put("Lizardman brute", new MonsterDrop("Lizardman brute", "Xeric's talisman (inert)", 1.0/250.0));
        monsterDrops.put("Rune dragon", new MonsterDrop("Rune dragon", "Wrath talisman", 1.0/127.0));
        monsterDrops.put("Hydra", new MonsterDrop("Hydra", "Dragon knife", 1.0/10001.0));
        monsterDrops.put("Turoth", new MonsterDrop("Turoth", "Mystic robe bottom (light)", 1.0/513.0));
        monsterDrops.put("Kurask", new MonsterDrop("Kurask", "Leaf-bladed battleaxe", 1.0/1026.0));
        monsterDrops.put("Iron dragon", new MonsterDrop("Iron dragon", "Dragon platelegs", 1.0/1025.0));
        monsterDrops.put("Frost Nagua", new MonsterDrop("Frost Nagua", "Glacial temotli", 1.0/501.0));
        monsterDrops.put("Skeletal wyvern", new MonsterDrop("Skeletal wyvern", "Granite legs", 1.0/513.0));
        monsterDrops.put("Spitting wyvern", new MonsterDrop("Spitting wyvern", "Granite boots", 1.0/2561.0));
        monsterDrops.put("Minotaur", new MonsterDrop("Minotaur", "Right skull half", 1.0/35.0));
        monsterDrops.put("Jelly", new MonsterDrop("Jelly", "Mithril boots", 1.0/129.0));
        monsterDrops.put("Cave slime", new MonsterDrop("Cave slime", "Iron boots", 1.0/129.0));
        monsterDrops.put("Drake", new MonsterDrop("Drake", "Dragon knife", 1.0/10001.0));
        monsterDrops.put("Brutal black dragon", new MonsterDrop("Brutal black dragon", "Uncut dragonstone", 1.0/513.0));
        monsterDrops.put("Basilisk knight", new MonsterDrop("Basilisk knight", "Basilisk jaw", 1.0/1000.0));
        monsterDrops.put("Zamorak warrior", new MonsterDrop("Zamorak warrior", "Rune scimitar", 1.0/51.0));
        monsterDrops.put("Gangster", new MonsterDrop("Gangster", "Rune scimitar", 1.0/81.0));
        monsterDrops.put("Adamant dragon", new MonsterDrop("Adamant dragon", "Wrath Talisman", 1.0/110.0));
        monsterDrops.put("Basilisk", new MonsterDrop("Basilisk", "Mystic hat (light)", 1.0/513.0));
        monsterDrops.put("Aviansie", new MonsterDrop("Aviansie", "Grimy dwarf weed", 1.0/364.0));
        monsterDrops.put("Black demon", new MonsterDrop("Black demon", "Grimy Dwarf Weed", 1.0/237.0));
        monsterDrops.put("Mourner", new MonsterDrop("Mourner", "Grimy dwarf weed", 1.0/364.0));
        monsterDrops.put("Blue dragon", new MonsterDrop("Blue dragon", "Grimy dwarf weed", 1.0/364.0));
        monsterDrops.put("Bronze dragon", new MonsterDrop("Bronze dragon", "Dragon plateplegs", 1.0/2049.0));
        monsterDrops.put("Bloodveld", new MonsterDrop("Bloodveld", "Black boots", 1.0/129.0));
        monsterDrops.put("Brine rat", new MonsterDrop("Brine rat", "Brine sabre", 1.0/513.0));
        monsterDrops.put("Ankou", new MonsterDrop("Ankou", "Left skull half", 1.0/34.0));
        monsterDrops.put("Black dragon", new MonsterDrop("Black dragon", "Rune longsword", 1.0/129.0));
        monsterDrops.put("Dagannoth", new MonsterDrop("Dagannoth", "Torstol Seed", 1.0/840.0));
        monsterDrops.put("Mutated bloodveld", new MonsterDrop("Mutated bloodveld", "Black boots", 1.0/129.0));
        monsterDrops.put("Cow calf", new MonsterDrop("Cow calf", "na", -1));
        monsterDrops.put("Aberrant spectre", new MonsterDrop("Aberrant spectre", "Mystic robe bottom (dark)", 1.0/513.0));
        monsterDrops.put("Cave kraken", new MonsterDrop("Cave kraken", "Uncharged trident", 1.0/201.0));
        monsterDrops.put("Kraken", new MonsterDrop("Kraken", "Trident of the Seas", 1.0/513.0));
        monsterDrops.put("Dark beast", new MonsterDrop("Dark beast", "Dark bow", 1.0/513.0));
        monsterDrops.put("Abyssal demon", new MonsterDrop("Abyssal demon", "Abyssal dagger", 1.0/32001.0));
        monsterDrops.put("Imp", new MonsterDrop("Imp", "Potion", 1.0/129.0));
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