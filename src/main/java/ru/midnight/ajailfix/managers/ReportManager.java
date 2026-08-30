package ru.midnight.ajailfix.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReportManager {
    
    private final AJailFixPlugin plugin;
    private final Map<Integer, Report> reports = new ConcurrentHashMap<>();
    private int nextId = 1;
    
    public static class Report {
        public int id;
        public String reporter;
        public UUID reporterUUID;
        public String message;
        public long timestamp;
        public ReportStatus status;
        public UUID claimedBy;
        public String claimedByName;
        public String answer;
        public long answeredAt;
        public ReportPriority priority;
        public String category;
        
        public Report(String reporter, UUID reporterUUID, String message) {
            this.reporter = reporter;
            this.reporterUUID = reporterUUID;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
            this.status = ReportStatus.OPEN;
            this.priority = ReportPriority.NORMAL;
        }
    }
    
    public enum ReportStatus {
        OPEN,
        IN_PROGRESS,
        ANSWERED,
        CLOSED
    }
    
    public enum ReportPriority {
        LOW(1),
        NORMAL(2),
        HIGH(3),
        CRITICAL(4);
        
        private final int level;
        
        ReportPriority(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    public ReportManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
        load();
    }
    
    public int createReport(Player reporter, String message) {
        int id = nextId++;
        Report report = new Report(reporter.getName(), reporter.getUniqueId(), message);
        report.id = id;
        reports.put(id, report);
        
        // Notify staff
        String prefix = plugin.getConfigManager().getPrefix();
        String msg = prefix + "§eНовый репорт #" + id + " от " + reporter.getName() + 
            ": §f" + message;
        
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("ajail.reports")) {
                plugin.sendMessage(staff, msg);
            }
        }
        
        // Log
        plugin.getAuditManager().log("REPORT_CREATE", reporter.getName(), message);
        
        saveReport(report);
        return id;
    }
    
    public void answerReport(int id, Player staff, String answer) {
        Report report = reports.get(id);
        if (report == null) {
            plugin.sendMessage(staff, plugin.getConfigManager().getPrefix() + "§cРепорт #" + id + " не найден!");
            return;
        }
        
        report.answer = answer;
        report.answeredAt = System.currentTimeMillis();
        report.status = ReportStatus.ANSWERED;
        
        // Notify reporter if online
        Player reporter = Bukkit.getPlayer(report.reporterUUID);
        if (reporter != null) {
            plugin.sendMessage(reporter, plugin.getConfigManager().getPrefix() + 
                "§aНа ваш репорт #" + id + " пришёл ответ от администрации:");
            plugin.sendMessage(reporter, "§e" + staff.getName() + ": §f" + answer);
        }
        
        // Notify staff
        plugin.getAuditManager().log("REPORT_ANSWER", "Report #" + id, answer);
        
        saveReport(report);
    }
    
    public void claimReport(int id, Player staff) {
        Report report = reports.get(id);
        if (report == null) {
            plugin.sendMessage(staff, plugin.getConfigManager().getPrefix() + "§cРепорт #" + id + " не найден!");
            return;
        }
        
        if (report.claimedBy != null) {
            plugin.sendMessage(staff, plugin.getConfigManager().getPrefix() + 
                "§cРепорт уже взят в работу: " + report.claimedByName);
            return;
        }
        
        report.claimedBy = staff.getUniqueId();
        report.claimedByName = staff.getName();
        report.status = ReportStatus.IN_PROGRESS;
        
        plugin.sendMessage(staff, plugin.getConfigManager().getPrefix() + 
            "§aВы взяли репорт #" + id + " в работу!");
        
        plugin.getAuditManager().log("REPORT_CLAIM", staff.getName(), "Report #" + id);
        saveReport(report);
    }
    
    public void closeReport(int id, Player staff) {
        Report report = reports.get(id);
        if (report == null) {
            plugin.sendMessage(staff, plugin.getConfigManager().getPrefix() + "§cРепорт #" + id + " не найден!");
            return;
        }
        
        report.status = ReportStatus.CLOSED;
        
        plugin.sendMessage(staff, plugin.getConfigManager().getPrefix() + 
            "§aРепорт #" + id + " закрыт!");
        
        plugin.getAuditManager().log("REPORT_CLOSE", staff.getName(), "Report #" + id);
        saveReport(report);
    }
    
    public Report getReport(int id) {
        return reports.get(id);
    }
    
    public Collection<Report> getOpenReports() {
        return reports.values().stream()
            .filter(r -> r.status == ReportStatus.OPEN || r.status == ReportStatus.IN_PROGRESS)
            .sorted((a, b) -> {
                // Sort by priority first, then by time
                int priorityCompare = Integer.compare(b.priority.getLevel(), a.priority.getLevel());
                if (priorityCompare != 0) return priorityCompare;
                return Long.compare(a.timestamp, b.timestamp);
            })
            .toList();
    }
    
    public int getOpenCount() {
        return (int) reports.values().stream()
            .filter(r -> r.status == ReportStatus.OPEN || r.status == ReportStatus.IN_PROGRESS)
            .count();
    }
    
    private void saveReport(Report report) {
        try {
            File dir = new File(plugin.getDataFolder(), "reports");
            if (!dir.exists()) dir.mkdirs();
            
            File file = new File(dir, report.id + ".yml");
            StringBuilder content = new StringBuilder();
            content.append("id: ").append(report.id).append("\n");
            content.append("reporter: ").append(report.reporter).append("\n");
            content.append("reporterUUID: ").append(report.reporterUUID).append("\n");
            content.append("message: '").append(report.message.replace("'", "''")).append("'\n");
            content.append("timestamp: ").append(report.timestamp).append("\n");
            content.append("status: ").append(report.status.name()).append("\n");
            if (report.claimedBy != null) {
                content.append("claimedBy: ").append(report.claimedByName).append("\n");
                content.append("claimedAt: ").append(report.timestamp).append("\n");
            }
            if (report.answer != null) {
                content.append("answer: '").append(report.answer.replace("'", "''")).append("'\n");
                content.append("answeredAt: ").append(report.answeredAt).append("\n");
            }
            content.append("priority: ").append(report.priority.name()).append("\n");
            
            Files.write(file.toPath(), content.toString().getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save report: " + e.getMessage());
        }
    }
    
    public void load() {
        File dir = new File(plugin.getDataFolder(), "reports");
        if (!dir.exists()) return;
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        for (File file : files) {
            try {
                // Load report from file
                // Implementation would parse YAML file
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load report: " + file.getName());
            }
        }
    }
    
    public void save() {
        for (Report report : reports.values()) {
            saveReport(report);
        }
    }
}
