package ru.midnight.ajailfix.managers;

import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.*;

public class CooldownManager {
    
    private final AJailFixPlugin plugin;
    private final Map<String, Map<UUID, Long>> cooldowns = new HashMap<>();
    
    public CooldownManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check if player is on cooldown for a command
     */
    public boolean isOnCooldown(Player player, String command) {
        Map<UUID, Long> commandCooldowns = cooldowns.get(command.toLowerCase());
        if (commandCooldowns == null) {
            return false;
        }
        
        Long lastUse = commandCooldowns.get(player.getUniqueId());
        if (lastUse == null) {
            return false;
        }
        
        int cooldownTime = getCooldownTime(command);
        if (cooldownTime <= 0) {
            return false;
        }
        
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
        return elapsed < cooldownTime;
    }
    
    /**
     * Get remaining cooldown in seconds
     */
    public int getRemainingCooldown(Player player, String command) {
        Map<UUID, Long> commandCooldowns = cooldowns.get(command.toLowerCase());
        if (commandCooldowns == null) {
            return 0;
        }
        
        Long lastUse = commandCooldowns.get(player.getUniqueId());
        if (lastUse == null) {
            return 0;
        }
        
        int cooldownTime = getCooldownTime(command);
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
        return Math.max(0, cooldownTime - (int) elapsed);
    }
    
    /**
     * Set cooldown for a command
     */
    public void setCooldown(Player player, String command) {
        cooldowns.computeIfAbsent(command.toLowerCase(), k -> new HashMap<>())
            .put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Clear cooldown for a command
     */
    public void clearCooldown(Player player, String command) {
        Map<UUID, Long> commandCooldowns = cooldowns.get(command.toLowerCase());
        if (commandCooldowns != null) {
            commandCooldowns.remove(player.getUniqueId());
        }
    }
    
    /**
     * Clear all cooldowns for a player
     */
    public void clearAllCooldowns(Player player) {
        for (Map<UUID, Long> commandCooldowns : cooldowns.values()) {
            commandCooldowns.remove(player.getUniqueId());
        }
    }
    
    /**
     * Get cooldown time for a command in seconds
     */
    private int getCooldownTime(String command) {
        // Default cooldowns
        return switch (command.toLowerCase()) {
            case "back" -> 10;
            case "spawn" -> 30;
            case "rtp", "randomtp" -> 120;
            case "home" -> 5;
            case "warp" -> 5;
            case "heal" -> 60;
            case "feed" -> 60;
            case "fly" -> 10;
            case "gm" -> 10;
            default -> 0;
        };
    }
    
    /**
     * Clean up expired cooldowns
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        
        for (Map<UUID, Long> commandCooldowns : cooldowns.values()) {
            commandCooldowns.entrySet().removeIf(entry -> {
                int cooldownTime = getCooldownTime(""); // Would need command context
                return now - entry.getValue() > cooldownTime * 1000 * 2;
            });
        }
    }
}
