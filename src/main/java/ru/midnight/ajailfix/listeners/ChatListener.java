package ru.midnight.ajailfix.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.midnight.ajailfix.AJailFixPlugin;

public class ChatListener implements Listener {
    
    private final AJailFixPlugin plugin;
    
    public ChatListener(AJailFixPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Admin chat
        if (message.startsWith("@a ") || message.startsWith("!a ") || 
            message.startsWith("/a ") && !event.isCancelled()) {
            
            if (player.hasPermission("ajail.adminchat")) {
                event.setCancelled(true);
                String adminMsg = message.substring(message.indexOf(" ") + 1);
                
                String formatted = plugin.getConfigManager().getAdminPrefix() + 
                    "§e" + player.getName() + " §7» §f" + adminMsg;
                
                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission("ajail.adminchat") || 
                        plugin.getAdminLevelManager().hasAdminLevel(staff)) {
                        staff.sendMessage(formatted);
                    }
                }
                
                plugin.getAuditManager().log("ADMIN_CHAT", player.getName(), adminMsg, player);
            }
            return;
        }
        
        // Spy messages
        for (Player spy : Bukkit.getOnlinePlayers()) {
            if (plugin.isSpying(spy.getUniqueId())) {
                // Log private messages
            }
        }
    }
}
