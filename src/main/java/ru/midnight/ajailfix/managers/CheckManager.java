package ru.midnight.ajailfix.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class CheckManager {
    
    private final AJailFixPlugin plugin;
    private final Map<UUID, CheckInfo> checks = new HashMap<>();
    
    public static class CheckInfo {
        public UUID playerUUID;
        public String playerName;
        public String reason;
        public long startTime;
        public UUID initiatedBy;
        public String initiatedByName;
        public int violations;
        public boolean active;
        public List<String> logs;
        
        public CheckInfo(UUID playerUUID, String playerName, String reason, UUID initiatedBy, String initiatedByName) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.reason = reason;
            this.startTime = System.currentTimeMillis();
            this.initiatedBy = initiatedBy;
            this.initiatedByName = initiatedByName;
            this.violations = 0;
            this.active = true;
            this.logs = new ArrayList<>();
        }
    }
    
    public CheckManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void startCheck(Player target, String reason, Player initiator) {
        UUID uuid = target.getUniqueId();
        
        if (checks.containsKey(uuid)) {
            plugin.sendMessage(initiator, plugin.getConfigManager().getPrefix() + 
                "§cИгрок уже проверяется!");
            return;
        }
        
        CheckInfo check = new CheckInfo(uuid, target.getName(), reason, 
            initiator.getUniqueId(), initiator.getName());
        checks.put(uuid, check);
        
        // Notify staff
        String msg = plugin.getConfigManager().getPrefix() + "§e" + initiator.getName() + 
            " §aначал проверку игрока §e" + target.getName() + 
            (reason != null ? " §7(§f" + reason + "§7)" : "");
        
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("ajail.checks")) {
                plugin.sendMessage(staff, msg);
            }
        }
        
        // Log
        plugin.getAuditManager().log("CHECK_START", target.getName(), 
            "Reason: " + reason + ", By: " + initiator.getName());
        
        // Save
        saveCheck(check);
    }
    
    public void endCheck(Player target, String verdict, String notes) {
        UUID uuid = target.getUniqueId();
        CheckInfo check = checks.remove(uuid);
        
        if (check == null) {
            return;
        }
        
        check.active = false;
        
        // Notify staff
        String msg = plugin.getConfigManager().getPrefix() + "§e" + check.playerName + 
            " §a- проверка завершена! Вердикт: §c" + verdict;
        
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("ajail.checks")) {
                plugin.sendMessage(staff, msg);
            }
        }
        
        // Log
        plugin.getAuditManager().log("CHECK_END", check.playerName, 
            "Verdict: " + verdict + ", Notes: " + notes);
        
        // Save to history
        saveToHistory(check, verdict, notes);
    }
    
    public void addViolation(Player checker, Player target, String violation) {
        UUID uuid = target.getUniqueId();
        CheckInfo check = checks.get(uuid);
        
        if (check == null) {
            return;
        }
        
        check.violations++;
        check.logs.add("[" + check.violations + "] " + violation);
        
        String msg = "§c[Violation #" + check.violations + "] §e" + target.getName() + 
            " §7- §f" + violation;
        
        plugin.sendMessage(checker, msg);
        
        // Broadcast to staff
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("ajail.checks")) {
                plugin.sendMessage(staff, msg);
            }
        }
    }
    
    public CheckInfo getCheck(UUID uuid) {
        return checks.get(uuid);
    }
    
    public boolean isBeingChecked(UUID uuid) {
        return checks.containsKey(uuid);
    }
    
    public Collection<CheckInfo> getActiveChecks() {
        return checks.values();
    }
    
    public void processTimeouts() {
        long now = System.currentTimeMillis();
        long timeout = 30 * 60 * 1000; // 30 minutes
        
        List<UUID> toRemove = new ArrayList<>();
        
        for (Map.Entry<UUID, CheckInfo> entry : checks.entrySet()) {
            CheckInfo check = entry.getValue();
            if (check.active && (now - check.startTime) > timeout) {
                toRemove.add(entry.getKey());
                
                Player target = Bukkit.getPlayer(check.playerUUID);
                if (target != null) {
                    endCheck(target, "§eTimeout", "Автоматическое завершение по таймауту");
                }
            }
        }
    }
    
    private void saveCheck(CheckInfo check) {
        try {
            File dir = new File(plugin.getDataFolder(), "checks");
            if (!dir.exists()) dir.mkdirs();
            
            File file = new File(dir, check.playerUUID + ".yml");
            StringBuilder content = new StringBuilder();
            content.append("player: ").append(check.playerName).append("\n");
            content.append("reason: ").append(check.reason).append("\n");
            content.append("initiatedBy: ").append(check.initiatedByName).append("\n");
            content.append("startTime: ").append(check.startTime).append("\n");
            content.append("active: ").append(check.active).append("\n");
            
            Files.write(file.toPath(), content.toString().getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save check: " + e.getMessage());
        }
    }
    
    private void saveToHistory(CheckInfo check, String verdict, String notes) {
        try {
            File dir = new File(plugin.getDataFolder(), "checks/history");
            if (!dir.exists()) dir.mkdirs();
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            File file = new File(dir, check.playerName + "_" + sdf.format(new Date()) + ".yml");
            
            StringBuilder content = new StringBuilder();
            content.append("player: ").append(check.playerName).append("\n");
            content.append("reason: ").append(check.reason).append("\n");
            content.append("initiatedBy: ").append(check.initiatedByName).append("\n");
            content.append("startTime: ").append(new Date(check.startTime)).append("\n");
            content.append("endTime: ").append(new Date()).append("\n");
            content.append("verdict: ").append(verdict).append("\n");
            content.append("notes: ").append(notes).append("\n");
            content.append("violations: ").append(check.violations).append("\n");
            
            for (String log : check.logs) {
                content.append("log: '").append(log).append("'\n");
            }
            
            Files.write(file.toPath(), content.toString().getBytes());
            
            // Delete active check file
            File activeFile = new File(dir.getParent(), check.playerUUID + ".yml");
            if (activeFile.exists()) {
                activeFile.delete();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save check history: " + e.getMessage());
        }
    }
    
    public void load() {
        File dir = new File(plugin.getDataFolder(), "checks");
        if (!dir.exists()) return;
        
        // Load active checks
    }
    
    public void save() {
        for (CheckInfo check : checks.values()) {
            saveCheck(check);
        }
    }
}
