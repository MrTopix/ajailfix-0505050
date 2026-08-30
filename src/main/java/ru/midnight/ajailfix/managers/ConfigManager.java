package ru.midnight.ajailfix.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    
    private final AJailFixPlugin plugin;
    private FileConfiguration messagesConfig;
    private FileConfiguration dataConfig;
    
    // Config values
    private boolean adminLevelsEnabled;
    private String prefix;
    private String adminPrefix;
    private String logFormat;
    private boolean broadcastToOps;
    private boolean useSounds;
    private String checkSound;
    private boolean autoCompleteOffline;
    
    public ConfigManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
        load();
    }
    
    public void load() {
        // Load main config
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        // General settings
        prefix = config.getString("messages.prefix", "&8[&cAJail&8]&r ");
        adminPrefix = config.getString("messages.admin-prefix", "&8[&6Admin&8]&r ");
        logFormat = config.getString("messages.log-format", "&7[%time%] %player%: %message%");
        broadcastToOps = config.getBoolean("settings.broadcast-to-ops", true);
        useSounds = config.getBoolean("settings.use-sounds", true);
        checkSound = config.getString("settings.check-sound", "ENTITY_ENDER_DRAGON_GROWL");
        autoCompleteOffline = config.getBoolean("settings.auto-complete-offline", true);
        adminLevelsEnabled = config.getBoolean("admin-levels.enabled", true);
        
        // Load messages config
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load data config
        File dataFile = new File(plugin.getDataFolder(), "data.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public void save() {
        FileConfiguration config = plugin.getConfig();
        
        // Save main config
        config.set("admin-levels.enabled", adminLevelsEnabled);
        config.set("settings.auto-complete-offline", autoCompleteOffline);
        plugin.saveConfig();
        
        // Save data config
        try {
            dataConfig.save(new File(plugin.getDataFolder(), "data.yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save data.yml: " + e.getMessage());
        }
    }
    
    public String getMessage(String key) {
        String message = messagesConfig.getString(key);
        if (message == null) {
            message = "&cMessage not found: " + key;
        }
        return message.replace("&", "§").replace("{prefix}", prefix);
    }
    
    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }
    
    // Getters and setters
    public boolean isAdminLevelsEnabled() {
        return adminLevelsEnabled;
    }
    
    public void setAdminLevelsEnabled(boolean enabled) {
        this.adminLevelsEnabled = enabled;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public String getAdminPrefix() {
        return adminPrefix;
    }
    
    public String getLogFormat() {
        return logFormat;
    }
    
    public boolean shouldBroadcastToOps() {
        return broadcastToOps;
    }
    
    public boolean shouldUseSounds() {
        return useSounds;
    }
    
    public String getCheckSound() {
        return checkSound;
    }
    
    public boolean shouldAutoCompleteOffline() {
        return autoCompleteOffline;
    }
    
    public FileConfiguration getDataConfig() {
        return dataConfig;
    }
    
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}
