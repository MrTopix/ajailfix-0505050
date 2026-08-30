package ru.midnight.ajailfix.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.*;

public class VanishManager {
    
    private final AJailFixPlugin plugin;
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Map<UUID, Set<UUID>> hiddenFrom = new HashMap<>();
    
    public VanishManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void vanish(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (vanishedPlayers.contains(uuid)) {
            return;
        }
        
        vanishedPlayers.add(uuid);
        plugin.setVanished(uuid, true);
        
        // Hide from all players
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("ajail.vanish") && !plugin.getAdminLevelManager().hasAdminLevel(online)) {
                online.hidePlayer(plugin, player);
                hiddenFrom.computeIfAbsent(uuid, k -> new HashSet<>()).add(online.getUniqueId());
            }
        }
        
        // Add effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "§aВы исчезли из виду!");
        
        plugin.getAuditManager().log("VANISH", player.getName(), "");
    }
    
    public void unvanish(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!vanishedPlayers.contains(uuid)) {
            return;
        }
        
        vanishedPlayers.remove(uuid);
        plugin.setVanished(uuid, false);
        
        // Show to all players
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
        
        hiddenFrom.remove(uuid);
        
        // Remove effect
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "§aВы снова видимы!");
        
        plugin.getAuditManager().log("UNVANISH", player.getName(), "");
    }
    
    public void toggleVanish(Player player) {
        if (vanishedPlayers.contains(player.getUniqueId())) {
            unvanish(player);
        } else {
            vanish(player);
        }
    }
    
    public void showPlayer(Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }
    
    public void hidePlayer(Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("ajail.vanish") && !plugin.getAdminLevelManager().hasAdminLevel(online)) {
                online.hidePlayer(plugin, player);
            }
        }
    }
    
    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.contains(uuid);
    }
    
    public boolean canSee(Player viewer, Player target) {
        UUID targetUUID = target.getUniqueId();
        
        // If viewer is vanished, they can see everyone vanished
        if (vanishedPlayers.contains(viewer.getUniqueId())) {
            return true;
        }
        
        // If target is not vanished, visible to everyone
        if (!vanishedPlayers.contains(targetUUID)) {
            return true;
        }
        
        // Check if viewer has permission to see vanished
        return viewer.hasPermission("ajail.vanish") || plugin.getAdminLevelManager().hasAdminLevel(viewer);
    }
    
    public Set<UUID> getVanishedPlayers() {
        return new HashSet<>(vanishedPlayers);
    }
    
    public int getVanishedCount() {
        return vanishedPlayers.size();
    }
    
    // Update visibility when a player joins
    public void onPlayerJoin(Player player) {
        for (UUID vanishedUUID : vanishedPlayers) {
            if (!player.hasPermission("ajail.vanish") && !plugin.getAdminLevelManager().hasAdminLevel(player)) {
                Player vanished = Bukkit.getPlayer(vanishedUUID);
                if (vanished != null) {
                    player.hidePlayer(plugin, vanished);
                }
            }
        }
    }
    
    // Update visibility when permissions change
    public void onPermissionChange(Player player) {
        if (player.hasPermission("ajail.vanish") || plugin.getAdminLevelManager().hasAdminLevel(player)) {
            // Can see vanished players
            for (UUID vanishedUUID : vanishedPlayers) {
                Player vanished = Bukkit.getPlayer(vanishedUUID);
                if (vanished != null) {
                    player.showPlayer(plugin, vanished);
                }
            }
        } else {
            // Cannot see vanished players
            for (UUID vanishedUUID : vanishedPlayers) {
                Player vanished = Bukkit.getPlayer(vanishedUUID);
                if (vanished != null) {
                    player.hidePlayer(plugin, vanished);
                }
            }
        }
    }
}
