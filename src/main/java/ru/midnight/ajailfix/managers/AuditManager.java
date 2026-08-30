package ru.midnight.ajailfix.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AuditManager {
    
    private final AJailFixPlugin plugin;
    private final Queue<AuditEntry> auditLog = new ConcurrentLinkedQueue<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    // Maximum entries to keep in memory
    private static final int MAX_MEMORY_ENTRIES = 1000;
    
    public static class AuditEntry {
        public String action;
        public String playerName;
        public String details;
        public String performedBy;
        public long timestamp;
        public String ip;
        
        public AuditEntry(String action, String playerName, String details, String performedBy) {
            this.action = action;
            this.playerName = playerName;
            this.details = details;
            this.performedBy = performedBy;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public AuditManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
        load();
    }
    
    /**
     * Log an action
     */
    public void log(String action, String playerName, String details) {
        String performedBy = "Console";
        
        if (Bukkit.getServer().isPrimaryThread()) {
            Collection<Player> players = Bukkit.getOnlinePlayers();
            // Log who performed the action (would need to get from stack trace or pass explicitly)
        }
        
        AuditEntry entry = new AuditEntry(action, playerName, details, performedBy);
        auditLog.offer(entry);
        
        // Trim old entries
        while (auditLog.size() > MAX_MEMORY_ENTRIES) {
            auditLog.poll();
        }
        
        // Write to file
        writeToFile(entry);
        
        // Broadcast to players with log permission
        if (plugin.getConfigManager().shouldBroadcastToOps()) {
            String message = "§7[§cAUDIT§7] §e" + performedBy + " §7" + action + " §e" + 
                playerName + (details != null && !details.isEmpty() ? " §7(" + details + ")" : "");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("ajail.logs") || plugin.getAdminLevelManager().hasAdminLevel(player)) {
                    plugin.sendMessage(player, message);
                }
            }
        }
    }
    
    /**
     * Log an action with performer
     */
    public void log(String action, String playerName, String details, Player performer) {
        String performedBy = performer != null ? performer.getName() : "Unknown";
        
        AuditEntry entry = new AuditEntry(action, playerName, details, performedBy);
        entry.ip = performer != null && performer.getAddress() != null ? 
            performer.getAddress().getAddress().getHostAddress() : null;
        
        auditLog.offer(entry);
        
        while (auditLog.size() > MAX_MEMORY_ENTRIES) {
            auditLog.poll();
        }
        
        writeToFile(entry);
        
        if (plugin.getConfigManager().shouldBroadcastToOps()) {
            String message = "§7[§cAUDIT§7] §e" + performedBy + " §7" + action + " §e" + 
                playerName + (details != null && !details.isEmpty() ? " §7(" + details + ")" : "");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("ajail.logs") || plugin.getAdminLevelManager().hasAdminLevel(player)) {
                    plugin.sendMessage(player, message);
                }
            }
        }
    }
    
    /**
     * Get recent audit entries
     */
    public List<AuditEntry> getRecentEntries(int limit) {
        List<AuditEntry> result = new ArrayList<>();
        int count = 0;
        
        Iterator<AuditEntry> iterator = auditLog.descendingIterator();
        while (iterator.hasNext() && count < limit) {
            result.add(iterator.next());
            count++;
        }
        
        Collections.reverse(result);
        return result;
    }
    
    /**
     * Get audit entries for a specific player
     */
    public List<AuditEntry> getEntriesForPlayer(String playerName) {
        List<AuditEntry> result = new ArrayList<>();
        
        for (AuditEntry entry : auditLog) {
            if (entry.playerName.equalsIgnoreCase(playerName)) {
                result.add(entry);
            }
        }
        
        return result;
    }
    
    /**
     * Get audit entries by action type
     */
    public List<AuditEntry> getEntriesByAction(String action) {
        List<AuditEntry> result = new ArrayList<>();
        
        for (AuditEntry entry : auditLog) {
            if (entry.action.equalsIgnoreCase(action)) {
                result.add(entry);
            }
        }
        
        return result;
    }
    
    /**
     * Search audit log
     */
    public List<AuditEntry> search(String query) {
        List<AuditEntry> result = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (AuditEntry entry : auditLog) {
            if (entry.playerName.toLowerCase().contains(lowerQuery) ||
                entry.action.toLowerCase().contains(lowerQuery) ||
                (entry.details != null && entry.details.toLowerCase().contains(lowerQuery))) {
                result.add(entry);
            }
        }
        
        return result;
    }
    
    private void writeToFile(AuditEntry entry) {
        try {
            File logDir = new File(plugin.getDataFolder(), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            File logFile = new File(logDir, "audit_" + getDateString() + ".log");
            String logLine = "[" + dateFormat.format(new Date(entry.timestamp)) + "] " +
                "[" + entry.action + "] " +
                (entry.performedBy != null ? "By: " + entry.performedBy : "Console") + " | " +
                "Player: " + entry.playerName +
                (entry.details != null && !entry.details.isEmpty() ? " | " + entry.details : "") +
                (entry.ip != null ? " | IP: " + entry.ip : "") + "\n";
            
            Files.writeString(logFile.toPath(), logLine, java.nio.file.StandardOpenOption.CREATE, 
                java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write audit log: " + e.getMessage());
        }
    }
    
    private String getDateString() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
    
    public void load() {
        // Load recent audit entries from files
    }
    
    public void save() {
        // Save audit entries to file
    }
}
