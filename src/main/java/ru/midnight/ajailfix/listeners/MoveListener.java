package ru.midnight.ajailfix.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.midnight.ajailfix.AJailFixPlugin;

public class MoveListener implements Listener {
    
    private final AJailFixPlugin plugin;
    
    public MoveListener(AJailFixPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Frozen player check
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();
            
            if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ())) {
                // Only prevent horizontal movement, allow looking around
                event.setTo(new Location(to.getWorld(), from.getX(), to.getY(), from.getZ(), to.getYaw(), to.getPitch()));
                
                // Notify player
                if (!plugin.getCooldownManager().isOnCooldown(player, "freeze_warn")) {
                    plugin.sendMessage(player, "§cВы заморожены! Не двигайтесь.");
                    plugin.getCooldownManager().setCooldown(player, "freeze_warn");
                }
            }
        }
        
        // Location spy check
        if (plugin.isSpying(player.getUniqueId())) {
            // Log player locations
        }
    }
}
