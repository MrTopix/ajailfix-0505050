package ru.midnight.ajailfix.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AdminLevelManager {
    
    private final AJailFixPlugin plugin;
    private final Map<UUID, Integer> adminLevels = new HashMap<>();
    private final Map<UUID, Set<String>> playerPermissions = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private Properties levelsConfig;
    private File levelsFile;
    
    // Level definitions
    private final Map<Integer, Set<String>> levelPermissions = new HashMap<>();
    
    public AdminLevelManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
        initLevelPermissions();
        load();
    }
    
    private void initLevelPermissions() {
        // Level 1 - Basic functions
        levelPermissions.put(1, new HashSet<>(Arrays.asList(
            "ajail.suite.access",
            "ajail.reports",
            "ajail.adminchat",
            "ajail.history",
            "ajail.asms"
        )));
        
        // Level 2 - Teleport and inventory
        levelPermissions.put(2, new HashSet<>(Arrays.asList(
            "ajail.level.1",
            "ajail.teleport",
            "ajail.invsee",
            "ajail.spec",
            "ajail.godmode",
            "ajail.fly"
        )));
        
        // Level 3 - Checks and mute
        levelPermissions.put(3, new HashSet<>(Arrays.asList(
            "ajail.level.2",
            "ajail.use",
            "ajail.unjail",
            "ajail.checks",
            "ajail.mute",
            "ajail.unmute",
            "ajail.global"
        )));
        
        // Level 4 - Ban and events
        levelPermissions.put(4, new HashSet<>(Arrays.asList(
            "ajail.level.3",
            "ajail.ban",
            "ajail.unban",
            "ajail.logs"
        )));
        
        // Level 5 - Senior admin
        levelPermissions.put(5, new HashSet<>(Arrays.asList(
            "ajail.level.4",
            "ajail.set",
            "ajail.reload",
            "ajail.adminlevels.manage"
        )));
        
        // Level 6 - Full access
        levelPermissions.put(6, new HashSet<>(Arrays.asList(
            "ajail.level.5",
            "ajail.admin"
        )));
    }
    
    public void load() {
        levelsFile = new File(plugin.getDataFolder(), "admin-levels.properties");
        
        // Load admin levels from config
        if (plugin.getConfigManager().getDataConfig().contains("admin-levels")) {
            var levelsNode = plugin.getConfigManager().getDataConfig().getConfigurationSection("admin-levels");
            if (levelsNode != null) {
                for (String uuidStr : levelsNode.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        int level = levelsNode.getInt(uuidStr);
                        adminLevels.put(uuid, level);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid admin level entry: " + uuidStr);
                    }
                }
            }
        }
        
        // Try to load additional permissions from properties file
        levelsConfig = new Properties();
        if (levelsFile.exists()) {
            try (var reader = new java.io.FileReader(levelsFile)) {
                levelsConfig.load(reader);
                loadCustomPermissions();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load admin-levels.properties: " + e.getMessage());
            }
        }
    }
    
    private void loadCustomPermissions() {
        for (int i = 1; i <= 6; i++) {
            String key = "level." + i + ".permissions";
            String perms = levelsConfig.getProperty(key);
            if (perms != null && !perms.isEmpty()) {
                Set<String> customPerms = new HashSet<>(Arrays.asList(perms.split(",")));
                levelPermissions.put(i, customPerms);
            }
        }
    }
    
    public void save() {
        // Save to config
        var dataConfig = plugin.getConfigManager().getDataConfig();
        for (Map.Entry<UUID, Integer> entry : adminLevels.entrySet()) {
            dataConfig.set("admin-levels." + entry.getKey().toString(), entry.getValue());
        }
        
        // Save to properties file
        try (var writer = new java.io.FileWriter(levelsFile)) {
            for (int i = 1; i <= 6; i++) {
                Set<String> perms = levelPermissions.get(i);
                if (perms != null && !perms.isEmpty()) {
                    writer.write("level." + i + ".permissions=" + String.join(",", perms) + "\n");
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save admin-levels.properties: " + e.getMessage());
        }
        
        plugin.getConfigManager().save();
    }
    
    /**
     * Set admin level for a player
     */
    public void setAdminLevel(Player player, int level) {
        UUID uuid = player.getUniqueId();
        adminLevels.put(uuid, level);
        
        // Add all permissions for this level
        Set<String> permissions = new HashSet<>();
        for (int i = 1; i <= level; i++) {
            Set<String> levelPerms = levelPermissions.get(i);
            if (levelPerms != null) {
                permissions.addAll(levelPerms);
            }
        }
        
        playerPermissions.put(uuid, permissions);
        applyPermissions(player);
        save();
        
        // Notify
        String levelName = getLevelName(level);
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "&aВам выдан уровень администратора: &e" + levelName);
        
        // Log
        plugin.getAuditManager().log("ADMIN_LEVEL_SET", player.getName(), 
            "Level: " + level + " (" + levelName + ")");
    }
    
    /**
     * Remove admin level and clean up all admin permissions
     */
    public void removeAdminLevel(Player player) {
        UUID uuid = player.getUniqueId();
        int previousLevel = adminLevels.getOrDefault(uuid, 0);
        String playerName = player.getName();
        
        // Remove from admin levels
        adminLevels.remove(uuid);
        playerPermissions.remove(uuid);
        
        // CRITICAL: Remove all admin-specific permissions and effects
        removeAdminEffects(player);
        removePermissions(player);
        
        save();
        
        // Notify
        String previousLevelName = getLevelName(previousLevel);
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "&cС вас снят уровень администратора: &e" + previousLevelName);
        
        // Log
        plugin.getAuditManager().log("ADMIN_LEVEL_REMOVED", playerName, 
            "Previous level: " + previousLevel + " (" + previousLevelName + ")");
        
        // Broadcast to other admins
        String message = plugin.getConfigManager().getPrefix() + 
            "&e" + playerName + " &cбыл снят с должности администратора";
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("ajail.admin") || hasAdminLevel(admin)) {
                plugin.sendMessage(admin, message);
            }
        }
    }
    
    /**
     * Remove all admin-specific effects when admin level is removed
     * This includes: GM, Fly, Vanish, StaffMode, etc.
     */
    private void removeAdminEffects(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Remove God Mode
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        
        // Disable flight if enabled
        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
            if (player.isFlying()) {
                player.setFlying(false);
            }
        }
        
        // Remove from vanish
        if (plugin.isVanished(uuid)) {
            plugin.getVanishManager().showPlayer(player);
            plugin.setVanished(uuid, false);
        }
        
        // Remove from staff mode
        if (plugin.isInStaffMode(uuid)) {
            plugin.getStaffModeManager().disableStaffMode(player);
            plugin.setStaffMode(uuid, false);
        }
        
        // Remove from freeze
        if (plugin.isFrozen(uuid)) {
            plugin.getFreezeManager().unfreezePlayer(player);
            plugin.setFrozen(uuid, false);
        }
        
        // Notify player about removed effects
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "&eВсе администраторские эффекты были сняты (GM, Fly, Vanish, StaffMode)");
    }
    
    private void applyPermissions(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Remove old attachment
        PermissionAttachment oldAttachment = attachments.get(uuid);
        if (oldAttachment != null) {
            player.removeAttachment(oldAttachment);
        }
        
        // Create new attachment
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachments.put(uuid, attachment);
        
        // Add permissions
        Set<String> permissions = playerPermissions.get(uuid);
        if (permissions != null) {
            for (String perm : permissions) {
                attachment.setPermission(perm, true);
            }
        }
    }
    
    private void removePermissions(Player player) {
        UUID uuid = player.getUniqueId();
        
        PermissionAttachment attachment = attachments.get(uuid);
        if (attachment != null) {
            // Remove all permissions
            for (String perm : attachment.getPermissions().keySet()) {
                attachment.unsetPermission(perm);
            }
            player.removeAttachment(attachment);
            attachments.remove(uuid);
        }
        
        playerPermissions.remove(uuid);
    }
    
    public int getAdminLevel(UUID uuid) {
        return adminLevels.getOrDefault(uuid, 0);
    }
    
    public int getAdminLevel(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return getAdminLevel(player.getUniqueId());
        }
        
        // Try to get from storage
        UUID uuid = plugin.getPunishmentManager().getPlayerUUID(playerName);
        if (uuid != null) {
            return getAdminLevel(uuid);
        }
        
        return 0;
    }
    
    public boolean hasAdminLevel(Player player) {
        return getAdminLevel(player.getUniqueId()) > 0;
    }
    
    public boolean hasAdminLevel(UUID uuid) {
        return adminLevels.containsKey(uuid);
    }
    
    public boolean hasPermission(Player player, String permission) {
        // Check if admin level grants permission
        int level = getAdminLevel(player.getUniqueId());
        if (level <= 0) {
            return player.hasPermission(permission);
        }
        
        // Check if any level up to their level grants this permission
        for (int i = 1; i <= level; i++) {
            Set<String> perms = levelPermissions.get(i);
            if (perms != null && (perms.contains(permission) || perms.contains("ajail.*"))) {
                return true;
            }
        }
        
        // Check standard permission
        return player.hasPermission(permission);
    }
    
    public boolean canAffect(Player admin, Player target) {
        int adminLevel = getAdminLevel(admin.getUniqueId());
        int targetLevel = getAdminLevel(target.getUniqueId());
        
        // Higher level admins can affect lower level admins
        return adminLevel > targetLevel;
    }
    
    public String getLevelName(int level) {
        switch (level) {
            case 1: return "§aModerator I";
            case 2: return "§bModerator II";
            case 3: return "§dModerator III";
            case 4: return "§6Administrator";
            case 5: return "§cSenior Administrator";
            case 6: return "§4§lChief Administrator";
            default: return "§7None";
        }
    }
    
    public Set<String> getPermissionsForLevel(int level) {
        Set<String> allPerms = new HashSet<>();
        for (int i = 1; i <= level; i++) {
            Set<String> levelPerms = levelPermissions.get(i);
            if (levelPerms != null) {
                allPerms.addAll(levelPerms);
            }
        }
        return allPerms;
    }
    
    public Map<UUID, Integer> getAllAdminLevels() {
        return new HashMap<>(adminLevels);
    }
    
    public List<Player> getOnlineAdmins() {
        return adminLevels.keySet().stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    public int getOnlineAdminCount() {
        return (int) adminLevels.keySet().stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .count();
    }
}
