package ru.midnight.ajailfix.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.*;

public class ChatManager {
    
    private final AJailFixPlugin plugin;
    private boolean chatMuted = false;
    private int slowChatSeconds = 0;
    private final Map<UUID, Long> playerLastMessage = new HashMap<>();
    private final Map<UUID, String> lastMessages = new HashMap<>();
    private final Map<UUID, Integer> messageCount = new HashMap<>();
    private final Set<UUID> spamWarnings = new HashSet<>();
    
    public ChatManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
        load();
    }
    
    public boolean canSendMessage(Player player) {
        // Check if chat is muted
        if (chatMuted && !player.hasPermission("ajail.mutechat.bypass")) {
            return false;
        }
        
        // Check slow chat
        if (slowChatSeconds > 0 && !player.hasPermission("ajail.slowchat.bypass")) {
            UUID uuid = player.getUniqueId();
            Long lastTime = playerLastMessage.get(uuid);
            
            if (lastTime != null) {
                long elapsed = (System.currentTimeMillis() - lastTime) / 1000;
                if (elapsed < slowChatSeconds) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public void onPlayerMessage(Player player, String message) {
        UUID uuid = player.getUniqueId();
        
        // Check mute
        if (plugin.getPunishmentManager().isMuted(player.getName())) {
            var mute = plugin.getPunishmentManager().getMute(player.getName());
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cВы замучены!");
            plugin.sendMessage(player, "§eПричина: §f" + mute.reason);
            plugin.sendMessage(player, "§eОсталось: §f" + mute.getTimeLeft());
            return;
        }
        
        // Update last message time
        playerLastMessage.put(uuid, System.currentTimeMillis());
        lastMessages.put(uuid, message);
        
        // Increment message count
        messageCount.merge(uuid, 1, Integer::sum);
        
        // Check for spam
        checkSpam(player, message);
    }
    
    private void checkSpam(Player player, String message) {
        UUID uuid = player.getUniqueId();
        
        // Get recent messages
        String lastMsg = lastMessages.get(uuid);
        if (lastMsg != null && lastMsg.equals(message)) {
            // Same message
            int count = messageCount.getOrDefault(uuid, 1);
            if (count >= 3) {
                // Auto-mute for spam
                plugin.getPunishmentManager().mute(player, "Spam", 
                    Bukkit.getConsoleSender(), 5 * 60 * 1000);
                
                plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                    "§cАвтоматический мут за спам!");
                
                plugin.getAuditManager().log("AUTO_MUTE", player.getName(), "Spam detected");
                
                messageCount.remove(uuid);
                return;
            }
        }
        
        // Reset count after 10 seconds of no messages
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messageCount.remove(uuid);
        }, 200L);
    }
    
    public void muteChat(Player admin) {
        chatMuted = true;
        
        Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() + 
            "§cЧат закрыт администрацией!");
        
        plugin.getAuditManager().log("CHAT_MUTE", admin.getName(), "");
    }
    
    public void unmuteChat(Player admin) {
        chatMuted = false;
        
        Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() + 
            "§aЧат открыт!");
        
        plugin.getAuditManager().log("CHAT_UNMUTE", admin.getName(), "");
    }
    
    public void toggleChat(Player admin) {
        if (chatMuted) {
            unmuteChat(admin);
        } else {
            muteChat(admin);
        }
    }
    
    public void setSlowChat(Player admin, int seconds) {
        slowChatSeconds = seconds;
        
        if (seconds > 0) {
            Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() + 
                "§eЗадержка чата: §f" + seconds + " секунд");
        } else {
            Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() + 
                "§aЗадержка чата снята!");
        }
        
        plugin.getAuditManager().log("SLOW_CHAT", admin.getName(), "Seconds: " + seconds);
    }
    
    public void clearChat(Player admin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) {
                player.sendMessage("");
            }
        }
        
        Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() + 
            "§eЧат очищен администрацией");
        
        plugin.getAuditManager().log("CLEAR_CHAT", admin.getName(), "");
    }
    
    public boolean isChatMuted() {
        return chatMuted;
    }
    
    public int getSlowChatSeconds() {
        return slowChatSeconds;
    }
    
    public void load() {
        var config = plugin.getConfigManager().getDataConfig();
        chatMuted = config.getBoolean("chat.muted", false);
        slowChatSeconds = config.getInt("chat.slow", 0);
    }
    
    public void save() {
        var config = plugin.getConfigManager().getDataConfig();
        config.set("chat.muted", chatMuted);
        config.set("chat.slow", slowChatSeconds);
        plugin.getConfigManager().save();
    }
}
