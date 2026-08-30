package ru.midnight.ajailfix.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {
    
    private final AJailFixPlugin plugin;
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Location> savedLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Location> spawnLocations = new HashMap<>();
    
    public TeleportManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
        load();
    }
    
    public void teleport(Player player, Location location) {
        // Save current location for /back
        lastLocations.put(player.getUniqueId(), player.getLocation());
        
        player.teleport(location);
    }
    
    public void teleportToPlayer(Player player, Player target) {
        teleport(player, target.getLocation());
        
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "§aТелепортация к игроку: §e" + target.getName());
    }
    
    public void teleportToOfflinePlayer(Player player, String targetName) {
        // Try to find offline player data
        var info = plugin.getPunishmentManager().getPlayerInfo(targetName);
        
        if (info == null || info.firstJoin == 0) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cИгрок " + targetName + " не найден в базе данных!");
            return;
        }
        
        // Get last known location from data
        Location lastLoc = getLastKnownLocation(targetName);
        if (lastLoc != null) {
            teleport(player, lastLoc);
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§aТелепортация к последней известной локации игрока: §e" + targetName);
        } else {
            // Teleport to spawn if no location found
            Location spawn = plugin.getServer().getWorlds().get(0).getSpawnLocation();
            teleport(player, spawn);
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§eИгрок " + targetName + " оффлайн. Телепортация на спавн.");
        }
    }
    
    public void bringPlayer(Player target, Player admin) {
        teleport(target, admin.getLocation());
        
        plugin.sendMessage(admin, plugin.getConfigManager().getPrefix() + 
            "§aИгрок " + target.getName() + " телепортирован к вам!");
        
        if (plugin.getConfigManager().shouldUseSounds()) {
            target.playSound(target.getLocation(), "ENTITY_ENDERMAN_TELEPORT", 1.0f, 1.0f);
        }
    }
    
    public void bringOfflinePlayer(String targetName, Player admin) {
        // Cannot directly teleport offline player, but we can teleport to them
        teleportToOfflinePlayer(admin, targetName);
    }
    
    public void goBack(Player player) {
        Location lastLoc = lastLocations.remove(player.getUniqueId());
        
        if (lastLoc == null) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cНет сохранённой локации!");
            return;
        }
        
        player.teleport(lastLoc);
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "§aВозврат на предыдущую локацию!");
    }
    
    public void saveLocation(Player player, String name) {
        savedLocations.put(player.getUniqueId(), player.getLocation());
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "§aЛокация сохранена: §e" + name);
    }
    
    public void loadLocation(Player player, String name) {
        Location loc = savedLocations.get(player.getUniqueId());
        if (loc == null) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cСохранённая локация не найдена!");
            return;
        }
        
        teleport(player, loc);
    }
    
    public Location getLastKnownLocation(String playerName) {
        var info = plugin.getPunishmentManager().getPlayerInfo(playerName);
        if (info == null || info.lastJoin == 0) {
            return null;
        }
        
        // Return spawn location as fallback
        return plugin.getServer().getWorlds().get(0).getSpawnLocation();
    }
    
    public void spectatorMode(Player player, Player target) {
        // Save player state
        lastLocations.put(player.getUniqueId(), player.getLocation());
        
        // Switch to spectator
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.setSpectatorTarget(target);
        
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "§aРежим наблюдения: §e" + target.getName());
    }
    
    public void stopSpectating(Player player) {
        Location lastLoc = lastLocations.remove(player.getUniqueId());
        
        if (lastLoc != null) {
            player.teleport(lastLoc);
        } else {
            player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
        }
        
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "§aВы вышли из режима наблюдения!");
    }
    
    public void randomTeleport(Player player) {
        if (player.getWorld() == null) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cОшибка: мир не найден!");
            return;
        }
        
        int x = (int) (Math.random() * 10000) - 5000;
        int z = (int) (Math.random() * 10000) - 5000;
        
        Location loc = new Location(player.getWorld(), x, player.getWorld().getHighestBlockYAt(x, z) + 1, z);
        
        teleport(player, loc);
        
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "§aСлучайная телепортация: §eX=" + x + " Z=" + z);
    }
    
    public void randomTeleportInWorld(Player player, String worldName) {
        var world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cМир не найден: " + worldName);
            return;
        }
        
        int x = (int) (Math.random() * 10000) - 5000;
        int z = (int) (Math.random() * 10000) - 5000;
        
        Location loc = new Location(world, x, world.getHighestBlockYAt(x, z) + 1, z);
        
        teleport(player, loc);
        
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "§aСлучайная телепортация в мире " + worldName + ": §eX=" + x + " Z=" + z);
    }
    
    public void setSpawn(Location location) {
        spawnLocations.put(null, location);
        plugin.getConfigManager().getDataConfig().set("spawn.world", location.getWorld().getName());
        plugin.getConfigManager().getDataConfig().set("spawn.x", location.getX());
        plugin.getConfigManager().getDataConfig().set("spawn.y", location.getY());
        plugin.getConfigManager().getDataConfig().set("spawn.z", location.getZ());
        plugin.getConfigManager().getDataConfig().set("spawn.yaw", location.getYaw());
        plugin.getConfigManager().getDataConfig().set("spawn.pitch", location.getPitch());
        plugin.getConfigManager().save();
    }
    
    public Location getSpawn() {
        if (spawnLocations.containsKey(null)) {
            return spawnLocations.get(null);
        }
        
        var data = plugin.getConfigManager().getDataConfig();
        if (data.contains("spawn.world")) {
            String worldName = data.getString("spawn.world");
            var world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                double x = data.getDouble("spawn.x");
                double y = data.getDouble("spawn.y");
                double z = data.getDouble("spawn.z");
                float yaw = (float) data.getDouble("spawn.yaw");
                float pitch = (float) data.getDouble("spawn.pitch");
                
                Location loc = new Location(world, x, y, z, yaw, pitch);
                spawnLocations.put(null, loc);
                return loc;
            }
        }
        
        return plugin.getServer().getWorlds().get(0).getSpawnLocation();
    }
    
    public void teleportToSpawn(Player player) {
        teleport(player, getSpawn());
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "§aТелепортация на спавн!");
    }
    
    public void load() {
        // Load saved locations from config
    }
    
    public void save() {
        // Save locations to config
    }
}
