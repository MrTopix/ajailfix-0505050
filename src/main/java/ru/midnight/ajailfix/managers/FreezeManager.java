package ru.midnight.ajailfix.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.*;

public class FreezeManager {
    
    private final AJailFixPlugin plugin;
    private final Map<UUID, FreezeData> frozenPlayers = new HashMap<>();
    
    public static class FreezeData {
        public UUID uuid;
        public String playerName;
        public Location freezeLocation;
        public String frozenBy;
        public long freezeTime;
        
        public FreezeData(UUID uuid, String playerName, Location location, String frozenBy) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.freezeLocation = location.clone();
            this.frozenBy = frozenBy;
            this.freezeTime = System.currentTimeMillis();
        }
    }
    
    public FreezeManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void freezePlayer(Player target, Player admin) {
        UUID uuid = target.getUniqueId();
        
        if (frozenPlayers.containsKey(uuid)) {
            plugin.sendMessage(admin, plugin.getConfigManager().getPrefix() + 
                "§cИгрок уже заморожен!");
            return;
        }
        
        FreezeData data = new FreezeData(uuid, target.getName(), target.getLocation(), admin.getName());
        frozenPlayers.put(uuid, data);
        
        plugin.setFrozen(uuid, true);
        
        // Apply effects
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 2, false, false));
        
        // Notify player
        plugin.sendMessage(target, plugin.getConfigManager().getPrefix() + 
            "§c⚠ Вы были заморожены администратором!");
        plugin.sendMessage(target, "§eНе двигайтесь до разморозки.");
        
        // Notify admin
        plugin.sendMessage(admin, plugin.getConfigManager().getPrefix() + 
            "§aИгрок " + target.getName() + " заморожен!");
        
        plugin.getAuditManager().log("FREEZE", target.getName(), "By: " + admin.getName());
    }
    
    public void unfreezePlayer(Player target) {
        UUID uuid = target.getUniqueId();
        
        FreezeData data = frozenPlayers.remove(uuid);
        if (data == null) {
            return;
        }
        
        plugin.setFrozen(uuid, false);
        
        // Remove effects
        target.removePotionEffect(PotionEffectType.BLINDNESS);
        target.removePotionEffect(PotionEffectType.SLOW);
        
        // Notify player
        plugin.sendMessage(target, plugin.getConfigManager().getPrefix() + 
            "§a✓ Вы разморожены!");
        
        plugin.getAuditManager().log("UNFREEZE", target.getName(), "By: " + data.frozenBy);
    }
    
    public void unfreezePlayerByName(String playerName, Player admin) {
        UUID uuid = null;
        for (Map.Entry<UUID, FreezeData> entry : frozenPlayers.entrySet()) {
            if (entry.getValue().playerName.equalsIgnoreCase(playerName)) {
                uuid = entry.getKey();
                break;
            }
        }
        
        if (uuid != null) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
                unfreezePlayer(target);
            } else {
                // Offline player
                frozenPlayers.remove(uuid);
                plugin.sendMessage(admin, plugin.getConfigManager().getPrefix() + 
                    "§aИгрок " + playerName + " разморожен (был оффлайн)!");
            }
        } else {
            plugin.sendMessage(admin, plugin.getConfigManager().getPrefix() + 
                "§cИгрок " + playerName + " не заморожен!");
        }
    }
    
    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.containsKey(uuid);
    }
    
    public FreezeData getFreezeData(UUID uuid) {
        return frozenPlayers.get(uuid);
    }
    
    public Set<UUID> getFrozenPlayers() {
        return new HashSet<>(frozenPlayers.keySet());
    }
    
    public void checkFrozenPlayers() {
        for (Map.Entry<UUID, FreezeData> entry : frozenPlayers.entrySet()) {
            UUID uuid = entry.getKey();
            FreezeData data = entry.getValue();
            
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            
            // Check if player moved significantly
            Location currentLoc = player.getLocation();
            Location freezeLoc = data.freezeLocation;
            
            if (currentLoc.getWorld() != freezeLoc.getWorld() ||
                currentLoc.distance(freezeLoc) > 0.5) {
                
                // Teleport back
                player.teleport(freezeLoc);
                
                // Notify
                plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                    "§c⚠ Вернитесь на место заморозки!");
            }
        }
    }
    
    public void checkMovement(Player player, Location from, Location to) {
        UUID uuid = player.getUniqueId();
        if (!frozenPlayers.containsKey(uuid)) {
            return;
        }
        
        FreezeData data = frozenPlayers.get(uuid);
        
        // Check if moved from freeze location
        if (from.distance(data.freezeLocation) < 0.1 && to.distance(data.freezeLocation) > 0.5) {
            // Teleport back
            player.teleport(data.freezeLocation);
            
            // Send warning
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§c⚠ Вернитесь на место заморозки!");
        }
    }
    
    public int getFrozenCount() {
        return frozenPlayers.size();
    }
}
