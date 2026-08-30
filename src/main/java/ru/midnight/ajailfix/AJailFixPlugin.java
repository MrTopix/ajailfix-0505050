package ru.midnight.ajailfix;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.midnight.ajailfix.commands.*;
import ru.midnight.ajailfix.listeners.*;
import ru.midnight.ajailfix.managers.*;
import ru.midnight.ajailfix.utils.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class AJailFixPlugin extends JavaPlugin implements Listener {

    private static AJailFixPlugin instance;
    
    // Managers
    private ConfigManager configManager;
    private AdminLevelManager adminLevelManager;
    private CheckManager checkManager;
    private ReportManager reportManager;
    private PunishmentManager punishmentManager;
    private TeleportManager teleportManager;
    private VanishManager vanishManager;
    private FreezeManager freezeManager;
    private ChatManager chatManager;
    private CooldownManager cooldownManager;
    private AuditManager auditManager;
    private StaffModeManager staffModeManager;
    private AntiCheatManager antiCheatManager;
    
    // Data storage
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Set<UUID> staffModePlayers = new HashSet<>();
    private final Set<UUID> spyingPlayers = new HashSet<>();
    
    // Tab completion cache
    private final Map<String, List<String>> tabCache = new HashMap<>();
    private long lastTabCacheUpdate = 0;
    private static final long TAB_CACHE_TIMEOUT = 5000;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        createDefaultFiles();
        
        configManager = new ConfigManager(this);
        adminLevelManager = new AdminLevelManager(this);
        checkManager = new CheckManager(this);
        reportManager = new ReportManager(this);
        punishmentManager = new PunishmentManager(this);
        teleportManager = new TeleportManager(this);
        vanishManager = new VanishManager(this);
        freezeManager = new FreezeManager(this);
        chatManager = new ChatManager(this);
        cooldownManager = new CooldownManager(this);
        auditManager = new AuditManager(this);
        staffModeManager = new StaffModeManager(this);
        antiCheatManager = new AntiCheatManager(this);
        
        loadAllData();
        
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new MoveListener(this), this);
        
        registerCommands();
        startScheduledTasks();
        
        getLogger().info("AJailFix v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        saveAllData();
        
        for (UUID uuid : vanishedPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) vanishManager.showPlayer(p);
        }
        
        for (UUID uuid : frozenPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) freezeManager.unfreezePlayer(p);
        }
        
        getLogger().info("AJailFix disabled!");
    }
    
    private void createDefaultFiles() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        new File(getDataFolder(), "data").mkdirs();
        new File(getDataFolder(), "logs").mkdirs();
        new File(getDataFolder(), "checks").mkdirs();
        new File(getDataFolder(), "punishments").mkdirs();
    }
    
    private void registerCommands() {
        // Main commands - each registers itself in constructor
        new AJailCommand(this);
        new AdminPanelCommand(this);
        new AntiCheatCommand(this);
        new TeleportCommand(this);
        new InvseeCommand(this);
        new AdminLevelCommand(this);
        new CheckCommand(this);
        new GodModeCommand(this);
        new PunishmentCommands(this);
        new BasicCommands(this);
        
        // Additional commands
        new BackCommand(this);
        new SpecCommand(this);
        new StaffListCommand(this);
        new WarnCommand(this);
        new BroadcastCommand(this);
        new ServerInfoCommand(this);
        new AuditCommand(this);
        new ReloadCommand(this);
        new HelpAdminCommand(this);
    }
    
    private void startScheduledTasks() {
        Bukkit.getScheduler().runTaskTimer(this, this::saveAllData, 6000L, 6000L);
        Bukkit.getScheduler().runTaskTimer(this, this::updateTabCache, 200L, 200L);
        Bukkit.getScheduler().runTaskTimer(this, checkManager::processTimeouts, 1200L, 1200L);
        Bukkit.getScheduler().runTaskTimer(this, freezeManager::checkFrozenPlayers, 40L, 40L);
    }
    
    private void updateTabCache() {
        long now = System.currentTimeMillis();
        if (now - lastTabCacheUpdate < TAB_CACHE_TIMEOUT) return;
        
        List<String> onlinePlayers = Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .collect(Collectors.toList());
        
        tabCache.put("online_players", onlinePlayers);
        
        // Add offline player options
        List<String> allPlayers = new ArrayList<>(onlinePlayers);
        allPlayers.add("offline:");
        
        tabCache.put("all_players", allPlayers);
        lastTabCacheUpdate = now;
    }
    
    private void loadAllData() {
        configManager.load();
        adminLevelManager.load();
        checkManager.load();
        reportManager.load();
        punishmentManager.load();
        auditManager.load();
    }
    
    private void saveAllData() {
        configManager.save();
        adminLevelManager.save();
        checkManager.save();
        reportManager.save();
        punishmentManager.save();
        auditManager.save();
    }
    
    // TAB completion handler
    @EventHandler
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        // Commands are handled by individual tab completers
    }
    
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    public void sendMessage(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    public void broadcast(String message) {
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    // Getters
    public static AJailFixPlugin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public AdminLevelManager getAdminLevelManager() { return adminLevelManager; }
    public CheckManager getCheckManager() { return checkManager; }
    public ReportManager getReportManager() { return reportManager; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public TeleportManager getTeleportManager() { return teleportManager; }
    public VanishManager getVanishManager() { return vanishManager; }
    public FreezeManager getFreezeManager() { return freezeManager; }
    public ChatManager getChatManager() { return chatManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public AuditManager getAuditManager() { return auditManager; }
    public StaffModeManager getStaffModeManager() { return staffModeManager; }
    public AntiCheatManager getAntiCheatManager() { return antiCheatManager; }
    
    // Player data
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, k -> new PlayerData(uuid));
    }
    
    public boolean isVanished(UUID uuid) { return vanishedPlayers.contains(uuid); }
    public void setVanished(UUID uuid, boolean v) {
        if (v) vanishedPlayers.add(uuid); else vanishedPlayers.remove(uuid);
    }
    
    public boolean isFrozen(UUID uuid) { return frozenPlayers.contains(uuid); }
    public void setFrozen(UUID uuid, boolean v) {
        if (v) frozenPlayers.add(uuid); else frozenPlayers.remove(uuid);
    }
    
    public boolean isInStaffMode(UUID uuid) { return staffModePlayers.contains(uuid); }
    public void setStaffMode(UUID uuid, boolean v) {
        if (v) staffModePlayers.add(uuid); else staffModePlayers.remove(uuid);
    }
    
    public boolean isSpying(UUID uuid) { return spyingPlayers.contains(uuid); }
    public void setSpying(UUID uuid, boolean v) {
        if (v) spyingPlayers.add(uuid); else spyingPlayers.remove(uuid);
    }
    
    public List<String> getTabCache(String key) { return tabCache.get(key); }
    
    // Player data class
    public static class PlayerData {
        private final UUID uuid;
        private long playtime;
        private long firstJoin;
        private long lastJoin;
        private String ipAddress;
        private List<String> ips = new ArrayList<>();
        private List<String> alts = new ArrayList<>();
        private Map<String, Object> customData = new HashMap<>();
        
        public PlayerData(UUID uuid) {
            this.uuid = uuid;
            this.firstJoin = System.currentTimeMillis();
            this.lastJoin = System.currentTimeMillis();
        }
        
        public UUID getUuid() { return uuid; }
        public long getPlaytime() { return playtime; }
        public void addPlaytime(long millis) { this.playtime += millis; }
        public long getFirstJoin() { return firstJoin; }
        public void setFirstJoin(long t) { this.firstJoin = t; }
        public long getLastJoin() { return lastJoin; }
        public void setLastJoin(long t) { this.lastJoin = t; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ip) {
            this.ipAddress = ip;
            if (!ips.contains(ip)) ips.add(ip);
        }
        public List<String> getIps() { return ips; }
        public List<String> getAlts() { return alts; }
        public void addAlt(String name) { if (!alts.contains(name)) alts.add(name); }
        public Map<String, Object> getCustomData() { return customData; }
    }
}
