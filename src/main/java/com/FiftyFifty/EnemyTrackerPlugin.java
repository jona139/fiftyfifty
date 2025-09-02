package com.FiftyFifty;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.ChatMessageType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@PluginDescriptor(
        name = "Fifty-Fifty",
        description = "Limits kills on monsters to make rare drops a 50/50 chance",
        tags = {"combat", "overlay", "pve", "kill", "tracker", "fifty"}
)
public class EnemyTrackerPlugin extends Plugin
{
    // GitHub repository URL
    private static final String GITHUB_URL = "https://github.com/GamecubeJona/fifty-fifty";
    private static final String CONFIG_GROUP = "enemytracker";
    private static final String PENDING_MONSTERS_KEY = "pendingMonsters";

    @Inject
    private Client client;

    @Inject
    private EnemyTrackerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClientThread clientThread;

    private EnemyKillTracker killTracker;
    private EnemyHighlighter highlighter;
    private RecentKillOverlay recentKillOverlay;
    private MenuEntrySwapper menuEntrySwapper;
    private ProgressDashboard progressDashboard;
    private FiftyFiftyPanel pluginPanel;
    private NavigationButton navButton;

    // Keep track of player interactions
    private final Map<NPC, Player> interactingMap = new HashMap<>();

    // Keep track of recently seen new monsters to avoid showing multiple dialogs
    private final Map<String, Long> recentNewMonsters = new ConcurrentHashMap<>();
    private static final long NEW_MONSTER_COOLDOWN = 60000; // 60 seconds in milliseconds

    // Map to store pending new monsters for batch processing
    private final Map<String, Long> pendingNewMonsters = new ConcurrentHashMap<>();

    @Override
    protected void startUp() throws Exception
    {
        log.info("Fifty-Fifty plugin started!");

        // Initialize custom NPC thresholds
        NpcKillThreshold.loadCustomMonsters(configManager);

        // Load pending monsters
        loadPendingMonsters();

        killTracker = new EnemyKillTracker(configManager);
        highlighter = new EnemyHighlighter(client, killTracker, config);
        recentKillOverlay = new RecentKillOverlay(config, killTracker);
        menuEntrySwapper = new MenuEntrySwapper(client, killTracker, config, clientThread);

        // Initialize the plugin panel
        pluginPanel = new FiftyFiftyPanel(this, killTracker, config);

        // Create a simple icon instead of loading one
        final BufferedImage icon = createIcon();

        // Create navigation button
        navButton = NavigationButton.builder()
                .tooltip("Fifty-Fifty")
                .icon(icon)
                .priority(5)
                .panel(pluginPanel)
                .build();

        // Add button to sidebar
        clientToolbar.addNavigation(navButton);

        // Add overlays
        overlayManager.add(highlighter);
        overlayManager.add(recentKillOverlay);

        // Register the MenuEntrySwapper
        eventBus.register(menuEntrySwapper);

        // Update panel content
        pluginPanel.update();
    }

    /**
     * Creates a simple icon for the plugin
     * @return BufferedImage to use as icon
     */
    private BufferedImage createIcon()
    {
        // Create a 24x24 pixel image
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Draw a simple icon - a circle with "50" in it
        g.setColor(new Color(80, 80, 80));
        g.fillOval(0, 0, 24, 24);

        g.setColor(new Color(220, 220, 220));
        g.drawOval(0, 0, 23, 23);

        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(10f));
        g.drawString("50", 5, 15);

        g.dispose();
        return image;
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("Fifty-Fifty plugin stopped!");

        overlayManager.remove(highlighter);
        overlayManager.remove(recentKillOverlay);

        if (progressDashboard != null && progressDashboard.isOpen())
        {
            progressDashboard.dispose();
        }

        // Remove navigation button
        clientToolbar.removeNavigation(navButton);

        // Unregister the MenuEntrySwapper
        eventBus.unregister(menuEntrySwapper);

        interactingMap.clear();
        recentNewMonsters.clear();

        // If there are pending monsters, save them to the config
        if (!pendingNewMonsters.isEmpty()) {
            savePendingMonsters();
        }
    }

    /**
     * Save pending monsters to config
     */
    private void savePendingMonsters() {
        // Convert to a serializable format (just the monster names)
        List<String> monsterNames = new ArrayList<>(pendingNewMonsters.keySet());
        String json = new Gson().toJson(monsterNames);
        configManager.setConfiguration(CONFIG_GROUP, PENDING_MONSTERS_KEY, json);
    }

    /**
     * Load pending monsters from config
     */
    private void loadPendingMonsters() {
        String json = configManager.getConfiguration(CONFIG_GROUP, PENDING_MONSTERS_KEY);
        if (json == null || json.isEmpty()) {
            return;
        }

        try {
            Type type = new TypeToken<ArrayList<String>>(){}.getType();
            List<String> monsterNames = new Gson().fromJson(json, type);

            // Add to the pending monsters map with current timestamp
            long now = System.currentTimeMillis();
            for (String name : monsterNames) {
                pendingNewMonsters.put(name, now);
            }
        } catch (Exception e) {
            log.error("Error loading pending monsters", e);
        }
    }

    /**
     * Handle a new monster that's not in the database or edit an existing one
     */
    public void handleNewMonster(String npcName) {
        // Check if we've recently seen this monster to avoid repeated dialogs
        if (recentNewMonsters.containsKey(npcName)) {
            long lastSeen = recentNewMonsters.get(npcName);
            if (System.currentTimeMillis() - lastSeen < NEW_MONSTER_COOLDOWN) {
                // Skip if we've seen this monster recently
                return;
            }
        }

        // Mark this monster as recently seen
        recentNewMonsters.put(npcName, System.currentTimeMillis());

        // Check if this monster already exists in the database
        final boolean isExistingMonster = NpcKillThreshold.isMonsterDefined(npcName);

        // Declare variables before assigning values to make them effectively final
        final String currentDropName;
        final double currentDropRate;
        final boolean currentExempt;

        // If it's an existing monster, get its current data
        if (isExistingMonster) {
            currentDropName = NpcKillThreshold.getRarestDropName(npcName);
            currentExempt = NpcKillThreshold.isExempt(npcName);

            // Get the drop rate if possible
            NpcKillThreshold.MonsterDrop dropInfo = NpcKillThreshold.getMonsterDropInfo(npcName);
            if (dropInfo != null) {
                // For predefined monsters
                currentDropRate = dropInfo.getDropRate();
            } else {
                // For custom monsters
                currentDropRate = NpcKillThreshold.getCustomDropRate(npcName);
            }
        } else {
            // Default values for new monsters
            currentDropName = "";
            currentDropRate = 0.0;
            currentExempt = false;
        }

        // Create and show the dialog on the EDT
        SwingUtilities.invokeLater(() -> {
            // Use null for parent frame which will center it on screen
            Frame parentFrame = null;

            // If the monster already exists, show a confirmation dialog first
            if (isExistingMonster) {
                int confirm = JOptionPane.showConfirmDialog(
                        parentFrame,
                        "Monster \"" + npcName + "\" already exists in the database.\nDo you want to edit its information?",
                        "Monster Already Exists",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                // If the user doesn't want to edit, just return
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Create the dialog with appropriate mode (edit or add)
            NewMonsterDialog dialog = new NewMonsterDialog(
                    parentFrame,
                    npcName,
                    (monsterName, dropName, dropRate, isExempt) -> {
                        // Add or update the monster in the database
                        NpcKillThreshold.addCustomMonster(configManager, monsterName, dropName, dropRate, isExempt);

                        // Update the panel
                        SwingUtilities.invokeLater(() -> pluginPanel.update());

                        // Inform the player
                        clientThread.invoke(() -> {
                            client.addChatMessage(
                                    net.runelite.api.ChatMessageType.GAMEMESSAGE,
                                    "",
                                    (isExistingMonster ? "Updated " : "Added ") + monsterName + " in the Fifty-Fifty database.",
                                    null
                            );
                        });
                    },
                    isExistingMonster,
                    currentDropName,
                    currentDropRate,
                    currentExempt
            );
            dialog.setVisible(true);
        });
    }

    /**
     * Get the pending new monsters
     */
    public Map<String, Long> getPendingNewMonsters() {
        return pendingNewMonsters;
    }

    /**
     * Remove a monster from the pending list
     */
    public void removePendingMonster(String monsterName) {
        pendingNewMonsters.remove(monsterName);
        // Update the panel
        SwingUtilities.invokeLater(() -> pluginPanel.update());
    }

    /**
     * Clear all pending monsters
     */
    public void clearPendingMonsters() {
        pendingNewMonsters.clear();
        // Update the panel
        SwingUtilities.invokeLater(() -> pluginPanel.update());
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.PUBLICCHAT &&
                event.getType() != ChatMessageType.PRIVATECHAT &&
                event.getType() != ChatMessageType.PRIVATECHATOUT)
        {
            return;
        }

        String message = event.getMessage().toLowerCase();

        // Check for edit kill command: "!ff set <monster> <kills>"
        if (message.startsWith("!ff set "))
        {
            String[] parts = event.getMessage().substring(8).split(" ");
            if (parts.length >= 2)
            {
                try
                {
                    // Get the kill count (last part)
                    int kills = Integer.parseInt(parts[parts.length - 1]);

                    // Get the monster name (everything except the last part)
                    StringBuilder monsterName = new StringBuilder();
                    for (int i = 0; i < parts.length - 1; i++)
                    {
                        if (i > 0) monsterName.append(" ");
                        monsterName.append(parts[i]);
                    }

                    String monster = monsterName.toString();

                    // Set the kills
                    killTracker.setKills(monster, kills);

                    // Update the panel
                    SwingUtilities.invokeLater(() -> pluginPanel.update());

                    // Notify the player
                    clientThread.invoke(() -> {
                        client.addChatMessage(
                                ChatMessageType.GAMEMESSAGE,
                                "",
                                "Set " + monster + " kills to " + kills,
                                null
                        );
                    });
                }
                catch (NumberFormatException e)
                {
                    clientThread.invoke(() -> {
                        client.addChatMessage(
                                ChatMessageType.GAMEMESSAGE,
                                "",
                                "Invalid command. Use: !ff set <monster name> <kill count>",
                                null
                        );
                    });
                }
            }
        }
        // Check for add kill command: "!ff add <monster> <kills>"
        else if (message.startsWith("!ff add "))
        {
            String[] parts = event.getMessage().substring(8).split(" ");
            if (parts.length >= 2)
            {
                try
                {
                    // Get the kill count to add (last part)
                    int killsToAdd = Integer.parseInt(parts[parts.length - 1]);

                    // Get the monster name (everything except the last part)
                    StringBuilder monsterName = new StringBuilder();
                    for (int i = 0; i < parts.length - 1; i++)
                    {
                        if (i > 0) monsterName.append(" ");
                        monsterName.append(parts[i]);
                    }

                    String monster = monsterName.toString();

                    // Add the kills
                    int currentKills = killTracker.getKills(monster);
                    killTracker.setKills(monster, currentKills + killsToAdd);

                    // Update the panel
                    SwingUtilities.invokeLater(() -> pluginPanel.update());

                    // Notify the player
                    clientThread.invoke(() -> {
                        client.addChatMessage(
                                ChatMessageType.GAMEMESSAGE,
                                "",
                                "Added " + killsToAdd + " kills to " + monster + " (total: " +
                                        killTracker.getKills(monster) + ")",
                                null
                        );
                    });
                }
                catch (NumberFormatException e)
                {
                    clientThread.invoke(() -> {
                        client.addChatMessage(
                                ChatMessageType.GAMEMESSAGE,
                                "",
                                "Invalid command. Use: !ff add <monster name> <kills to add>",
                                null
                        );
                    });
                }
            }
        }
        // Help command
        else if (message.equals("!ff help"))
        {
            clientThread.invoke(() -> {
                client.addChatMessage(
                        ChatMessageType.GAMEMESSAGE,
                        "",
                        "Fifty-Fifty commands: !ff set <monster> <kills> | !ff add <monster> <kills>",
                        null
                );
            });
        }
    }

    /**
     * This handles the menu when it's fully opened (right-click).
     * We can use this to completely remove attack options.
     */
    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        if (!config.hideAttackOption())
        {
            return;
        }

        MenuEntry[] entries = event.getMenuEntries();
        boolean modified = false;

        // Rebuild the entries array without attack options for maxed NPCs
        MenuEntry[] newEntries = new MenuEntry[entries.length];
        int index = 0;

        for (MenuEntry entry : entries)
        {
            // Skip null entries
            if (entry == null)
            {
                continue;
            }

            String option = entry.getOption();

            // For non-attack options, just keep them
            if (option == null || (!isAttackOption(option)))
            {
                newEntries[index++] = entry;
                continue;
            }

            // For attack options, check if the NPC is maxed out
            int id = entry.getIdentifier();
            NPC npc = findNpcById(id);

            if (npc == null || npc.getName() == null)
            {
                // Keep the option if we can't identify the NPC
                newEntries[index++] = entry;
                continue;
            }

            String npcName = npc.getName();

            // Check if this monster is exempt from kill limits (like cows)
            if (NpcKillThreshold.isExempt(npcName)) {
                // Exempt monsters can always be attacked
                newEntries[index++] = entry;
                continue;
            }

            int threshold = NpcKillThreshold.getThreshold(npcName);
            int currentKills = killTracker.getKills(npcName);

            // If we've reached the threshold, skip this attack option
            if (currentKills >= threshold)
            {
                log.info("Removing attack option for maxed mob: {}", npcName);
                modified = true;
                // Don't add this entry to the new array
            }
            else
            {
                // Keep the attack option for non-maxed NPCs
                newEntries[index++] = entry;
            }
        }

        // If we modified any entries, update the menu
        if (modified)
        {
            // Trim the array to the actual size
            newEntries = Arrays.copyOf(newEntries, index);
            client.setMenuEntries(newEntries);
        }
    }

    /**
     * When a ClientTick occurs, this is our last chance to modify menu entries
     * before they're displayed or processed.
     */
    @Subscribe
    public void onClientTick(ClientTick tick)
    {
        if (!config.hideAttackOption() || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        // Get the menu entries
        MenuEntry[] menuEntries = client.getMenuEntries();
        if (menuEntries == null || menuEntries.length == 0)
        {
            return;
        }

        // Find menu entries with "Attack" as a left-click option for maxed NPCs
        boolean modified = false;
        MenuEntry[] newEntries = new MenuEntry[menuEntries.length];
        int index = 0;

        for (MenuEntry entry : menuEntries)
        {
            if (entry == null)
            {
                continue;
            }

            String option = entry.getOption();
            if (option == null)
            {
                newEntries[index++] = entry;
                continue;
            }

            // Is it an attack option?
            if (!isAttackOption(option))
            {
                newEntries[index++] = entry;
                continue;
            }

            // Is it on an NPC?
            MenuAction type = entry.getType();
            if (type != MenuAction.NPC_FIRST_OPTION &&
                    type != MenuAction.NPC_SECOND_OPTION &&
                    type != MenuAction.NPC_THIRD_OPTION &&
                    type != MenuAction.NPC_FOURTH_OPTION &&
                    type != MenuAction.NPC_FIFTH_OPTION)
            {
                newEntries[index++] = entry;
                continue;
            }

            // Find the NPC
            int id = entry.getIdentifier();
            NPC npc = findNpcById(id);

            if (npc == null || npc.getName() == null)
            {
                newEntries[index++] = entry;
                continue;
            }

            String npcName = npc.getName();

            // Check if this monster is exempt from kill limits
            if (NpcKillThreshold.isExempt(npcName)) {
                // Allow attacking exempt monsters without restrictions
                newEntries[index++] = entry;
                continue;
            }

            int threshold = NpcKillThreshold.getThreshold(npcName);
            int currentKills = killTracker.getKills(npcName);

            // If the NPC has reached the threshold, replace the attack option with "Walk here" or skip it
            if (currentKills >= threshold)
            {
                log.info("Blocking attack option for maxed mob: {}", npcName);

                // Find a "Talk-to" or "Examine" option for this NPC to use instead
                boolean foundReplacement = false;

                for (MenuEntry altEntry : menuEntries)
                {
                    if (altEntry != null &&
                            altEntry.getIdentifier() == id &&
                            altEntry.getOption() != null &&
                            (altEntry.getOption().equals("Talk-to") ||
                                    altEntry.getOption().equals("Examine") ||
                                    altEntry.getOption().equals("Pickpocket")))
                    {
                        // Replace the attack option with this alternate option
                        entry.setOption(altEntry.getOption());
                        entry.setType(altEntry.getType());
                        newEntries[index++] = entry;
                        foundReplacement = true;
                        modified = true;
                        break;
                    }
                }

                // If we didn't find a replacement, just use "Walk here"
                if (!foundReplacement)
                {
                    // Skip this entry - it will effectively be removed
                    modified = true;
                }
            }
            else
            {
                // Keep normal attack options for non-maxed NPCs
                newEntries[index++] = entry;
            }
        }

        // If we modified any entries, update the menu
        if (modified)
        {
            // Trim the array to the actual size
            newEntries = Arrays.copyOf(newEntries, index);
            client.setMenuEntries(newEntries);
        }
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

                // Check if this NPC has reached its kill threshold
                if (npc.getName() != null)
                {
                    String npcName = npc.getName();

                    // Skip exempt monsters (like cows that always drop the same items)
                    if (NpcKillThreshold.isExempt(npcName)) {
                        log.debug("Interacting with exempt monster: {}", npcName);
                        return;
                    }

                    int threshold = NpcKillThreshold.getThreshold(npcName);
                    int currentKills = killTracker.getKills(npcName);

                    // If the NPC has reached the kill threshold, warn the player
                    if (currentKills >= threshold)
                    {
                        log.info("Starting combat with maxed out NPC: {}", npcName);

                        // Show a message to the player
                        clientThread.invoke(() -> {
                            client.addChatMessage(
                                    net.runelite.api.ChatMessageType.GAMEMESSAGE,
                                    "",
                                    "Warning: You've already reached the kill threshold for " + npcName + ".",
                                    null
                            );
                        });
                    }
                }
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

                // Update the plugin panel
                pluginPanel.update();

                log.debug("Killed {}, count: {}/{}",
                        npcName,
                        killTracker.getKills(npcName),
                        NpcKillThreshold.getThreshold(npcName));

                // Check if this is a new monster not in our database
                if (!NpcKillThreshold.isMonsterDefined(npcName)) {
                    log.info("Detected new monster: {}", npcName);

                    // If batch mode is enabled, add to pending monsters
                    if (config.batchModeEnabled()) {
                        // Only add if not already in the pending list
                        if (!pendingNewMonsters.containsKey(npcName)) {
                            pendingNewMonsters.put(npcName, System.currentTimeMillis());
                            // Notify the player that a new monster was added to the pending list
                            clientThread.invoke(() -> {
                                client.addChatMessage(
                                        net.runelite.api.ChatMessageType.GAMEMESSAGE,
                                        "",
                                        "New monster detected: " + npcName + " (Added to pending list)",
                                        null
                                );
                            });
                            // Update the panel to show the new pending monster
                            SwingUtilities.invokeLater(() -> pluginPanel.update());
                        }
                    } else {
                        // If batch mode is disabled, show dialog immediately
                        handleNewMonster(npcName);
                    }
                }
            }

            // Remove the NPC from the tracking map
            interactingMap.remove(npc);
        }
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
            // Update the panel
            pluginPanel.update();
        }

        if (config.resetCustomMonsters())
        {
            NpcKillThreshold.resetCustomMonsters(configManager);
            // Reset the config option
            configManager.setConfiguration(EnemyTrackerConfig.class.getAnnotation(ConfigGroup.class).value(), "resetCustomMonsters", false);
            // Update the panel
            pluginPanel.update();
            // Inform the user
            clientThread.invoke(() -> {
                client.addChatMessage(
                        net.runelite.api.ChatMessageType.GAMEMESSAGE,
                        "",
                        "Reset all custom monster data.",
                        null
                );
            });
        }

        // Check if dashboard should be opened
        if (config.openDashboard())
        {
            openDashboard();
            // Reset the config option
            configManager.setConfiguration(EnemyTrackerConfig.class.getAnnotation(ConfigGroup.class).value(), "openDashboard", false);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals(EnemyTrackerConfig.class.getAnnotation(ConfigGroup.class).value()))
        {
            // Update the panel when config changes
            pluginPanel.update();
        }
    }

    /**
     * Opens the GitHub page for the plugin
     */
    public void openGitHubPage()
    {
        LinkBrowser.browse(GITHUB_URL);
    }

    /**
     * Opens the detailed progress dashboard
     */
    public void openDashboard()
    {
        // Lazy initialization of the dashboard
        if (progressDashboard == null)
        {
            progressDashboard = new ProgressDashboard(killTracker, configManager);
        }

        if (!progressDashboard.isOpen())
        {
            progressDashboard.open();
        }
        else
        {
            progressDashboard.requestFocus();
        }
    }

    /**
     * Opens a dialog to manually add a new monster or edit an existing one
     */
    public void openAddMonsterDialog() {
        SwingUtilities.invokeLater(() -> {
            // Use null for parent frame which will center it on screen
            Frame parentFrame = null;

            // Show an input dialog to get the monster name
            String monsterName = javax.swing.JOptionPane.showInputDialog(
                    parentFrame,
                    "Enter the monster name:",
                    "Add or Edit Monster",
                    javax.swing.JOptionPane.QUESTION_MESSAGE
            );

            if (monsterName != null && !monsterName.trim().isEmpty()) {
                monsterName = monsterName.trim();

                // Check if the monster already exists
                boolean isExistingMonster = NpcKillThreshold.isMonsterDefined(monsterName);

                if (isExistingMonster) {
                    // Show a confirmation dialog
                    int confirm = javax.swing.JOptionPane.showConfirmDialog(
                            parentFrame,
                            "This monster already exists in the database. Do you want to edit it?",
                            "Monster Already Exists",
                            javax.swing.JOptionPane.YES_NO_OPTION,
                            javax.swing.JOptionPane.QUESTION_MESSAGE
                    );

                    // If they don't want to edit, return
                    if (confirm != javax.swing.JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                // Show the dialog to add/edit the monster
                final String finalName = monsterName;
                handleNewMonster(finalName);
            }
        });
    }

    @Provides
    EnemyTrackerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(EnemyTrackerConfig.class);
    }
}