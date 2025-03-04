package com.FiftyFifty;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
public class EnemyKillTracker
{
    private static final String CONFIG_GROUP = "enemytracker";
    private static final String KILLS_KEY = "enemyKills";
    
    private final ConfigManager configManager;
    private final Gson gson;
    private Map<String, Integer> enemyKills;
    
    public EnemyKillTracker(ConfigManager configManager)
    {
        this.configManager = configManager;
        this.gson = new Gson();
        this.enemyKills = loadKills();
    }
    
    private Map<String, Integer> loadKills()
    {
        String json = configManager.getConfiguration(CONFIG_GROUP, KILLS_KEY);
        if (json == null || json.isEmpty())
        {
            return new HashMap<>();
        }
        
        try
        {
            Type type = new TypeToken<HashMap<String, Integer>>(){}.getType();
            return gson.fromJson(json, type);
        }
        catch (Exception e)
        {
            log.error("Error loading enemy kills", e);
            return new HashMap<>();
        }
    }
    
    private void saveKills()
    {
        String json = gson.toJson(enemyKills);
        configManager.setConfiguration(CONFIG_GROUP, KILLS_KEY, json);
    }
    
    public int getKills(String enemyName)
    {
        return enemyKills.getOrDefault(enemyName, 0);
    }
    
    public void addKill(String enemyName)
    {
        int currentKills = getKills(enemyName);
        enemyKills.put(enemyName, currentKills + 1);
        saveKills();
    }
    
    public boolean hasReachedThreshold(String enemyName, int threshold)
    {
        return getKills(enemyName) >= threshold;
    }
    
    public float getProgressPercentage(String enemyName)
    {
        int threshold = NpcKillThreshold.getThreshold(enemyName);
        int kills = getKills(enemyName);
        
        if (threshold == 0) return 0; // Avoid division by zero
        
        float percentage = (float) kills / threshold;
        return Math.min(1.0f, percentage); // Cap at 100%
    }
    
    public void resetKills()
    {
        enemyKills.clear();
        saveKills();
    }
    
    public Map<String, Integer> getAllKills()
    {
        return new HashMap<>(enemyKills); // Return a copy to avoid external modification
    }
}
