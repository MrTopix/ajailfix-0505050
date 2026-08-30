package ru.midnight.ajailfix.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiCheatManager {
    
    private final AJailFixPlugin plugin;
    
    // Active checks
    private final Map<UUID, CheckData> activeChecks = new ConcurrentHashMap<>();
    
    // Check categories
    public enum CheckType {
        // Movement
        FLY("Fly", "Полёт", CheckCategory.MOVEMENT),
        SPEED("Speed", "Ускорение", CheckCategory.MOVEMENT),
        NOCLIP("NoClip", "Проход сквозь блоки", CheckCategory.MOVEMENT),
        PHASE("Phase", "Фаза", CheckCategory.MOVEMENT),
        BUNNY_HOP("BHop", "Банихоп", CheckCategory.MOVEMENT),
        WATER_WALK("WaterWalk", "Ходьба по воде", CheckCategory.MOVEMENT),
        
        // Combat
        REACH("Reach", "Дальний удар", CheckCategory.COMBAT),
        AIMBOT("Aimbot", "Аимбот", CheckCategory.COMBAT),
        AUTO_CLICK("AutoClick", "Автоклик", CheckCategory.COMBAT),
        KILL_AURA("KillAura", "Килаура", CheckCategory.COMBAT),
        TRIGGERBOT("Triggerbot", "Триггербот", CheckCategory.COMBAT),
        VELOCITY("Velocity", "Велосити", CheckCategory.COMBAT),
        ANTI_KB("AntiKnockback", "Анти-кнакбек", CheckCategory.COMBAT),
        
        // Player
        SCAFFOLD("Scaffold", "Скаффолд", CheckCategory.PLAYER),
        AUTO_ARMOR("AutoArmor", "Авто-броня", CheckCategory.PLAYER),
        AUTO_FOOD("AutoFood", "Авто-еда", CheckCategory.PLAYER),
        INVENTORY("Inventory", "Неверный инвентарь", CheckCategory.PLAYER),
        
        // Other
        SPAM("Spam", "Спам", CheckCategory.OTHER),
        CHAT("Chat", "Чат", CheckCategory.OTHER),
        COMMANDS("Commands", "Команды", CheckCategory.OTHER);
        
        private final String name;
        private final String displayName;
        private final CheckCategory category;
        
        CheckType(String name, String displayName, CheckCategory category) {
            this.name = name;
            this.displayName = displayName;
            this.category = category;
        }
        
        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public CheckCategory getCategory() { return category; }
    }
    
    public enum CheckCategory {
        MOVEMENT("§bДвижение"),
        COMBAT("§cБой"),
        PLAYER("§aИгрок"),
        OTHER("§eДругое");
        
        private final String color;
        
        CheckCategory(String color) {
            this.color = color;
        }
        
        public String getColor() { return color; }
    }
    
    public static class CheckData {
        public UUID uuid;
        public String playerName;
        public CheckType checkType;
        public String reason;
        public UUID initiatedBy;
        public String initiatedByName;
        public long startTime;
        public long lastActivity;
        public int violations;
        public boolean isActive;
        public String notes;
        public Location spawnLocation;
        public List<String> evidence;
        
        public CheckData(UUID uuid, String playerName, CheckType type, String reason, UUID initiatedBy) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.checkType = type;
            this.reason = reason != null ? reason : type.getDisplayName();
            this.initiatedBy = initiatedBy;
            this.startTime = System.currentTimeMillis();
            this.lastActivity = System.currentTimeMillis();
            this.violations = 0;
            this.isActive = true;
            this.evidence = new ArrayList<>();
        }
    }
    
    public AntiCheatManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Start a new check on a player
     */
    public void startCheck(Player target, CheckType type, String reason, Player initiator) {
        UUID targetUUID = target.getUniqueId();
        
        // Check if already being checked
        if (activeChecks.containsKey(targetUUID)) {
            plugin.sendMessage(initiator, plugin.getConfigManager().getPrefix() + 
                "§cИгрок " + target.getName() + " уже проверяется!");
            return;
        }
        
        // Save spawn location
        Location spawnLoc = target.getLocation().clone();
        
        // Create check data
        CheckData data = new CheckData(
            targetUUID,
            target.getName(),
            type,
            reason,
            initiator.getUniqueId()
        );
        data.spawnLocation = spawnLoc;
        data.initiatedByName = initiator.getName();
        
        activeChecks.put(targetUUID, data);
        
        // Notify staff
        String checkMessage = plugin.getConfigManager().getPrefix() + 
            "§e" + initiator.getName() + " §aначал проверку на §c" + type.getDisplayName() + 
            " §aигрока §e" + target.getName();
        
        broadcastToStaff(checkMessage);
        
        // Apply check effects
        applyCheckEffects(target);
        
        // Log
        plugin.getAuditManager().log("CHECK_STARTED", target.getName(), 
            "Type: " + type.getName() + ", Reason: " + reason + ", By: " + initiator.getName());
        
        // Save to file
        saveCheckData(data);
    }
    
    /**
     * Apply visual and gameplay effects for check
     */
    private void applyCheckEffects(Player player) {
        // Give checker a visible indicator
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 0, false, false));
        
        // Send title
        player.sendTitle(
            "§c⚠ ПРОВЕРКА НА ЧИТЫ",
            "§eПросим вас оставаться на месте",
            10, 100, 10
        );
        
        // Clear inventory for clean check
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
    }
    
    /**
     * End a check
     */
    public void endCheck(Player target, String verdict, String notes) {
        UUID targetUUID = target.getUniqueId();
        CheckData data = activeChecks.remove(targetUUID);
        
        if (data == null) {
            return;
        }
        
        data.isActive = false;
        data.notes = notes;
        data.lastActivity = System.currentTimeMillis();
        
        // Remove check effects
        removeCheckEffects(target);
        
        // Teleport back if needed
        if (data.spawnLocation != null && data.spawnLocation.getWorld() != null) {
            target.teleport(data.spawnLocation);
        }
        
        // Notify staff
        String endMessage = plugin.getConfigManager().getPrefix() + 
            "§e" + data.playerName + " §a- проверка завершена! Вердикт: §c" + verdict;
        
        broadcastToStaff(endMessage);
        
        // Log
        plugin.getAuditManager().log("CHECK_ENDED", data.playerName, 
            "Verdict: " + verdict + ", Notes: " + notes);
        
        // Save to history
        saveCheckHistory(data, verdict);
    }
    
    /**
     * Remove check effects from player
     */
    private void removeCheckEffects(Player player) {
        // Remove potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        
        // Clear title
        player.sendTitle("", "", 0, 0, 0);
    }
    
    /**
     * Add violation during check
     */
    public void addViolation(Player checker, Player target, String violation) {
        UUID targetUUID = target.getUniqueId();
        CheckData data = activeChecks.get(targetUUID);
        
        if (data == null) {
            return;
        }
        
        data.violations++;
        data.lastActivity = System.currentTimeMillis();
        
        String violationMsg = "§c[Violation #" + data.violations + "] §e" + target.getName() + 
            " §7- §f" + violation;
        
        // Send to checker
        plugin.sendMessage(checker, violationMsg);
        
        // Also send to all staff
        broadcastToStaff(violationMsg);
        
        // Add evidence
        addEvidence(target, violation);
    }
    
    /**
     * Add evidence for check
     */
    public void addEvidence(Player player, String evidence) {
        UUID uuid = player.getUniqueId();
        CheckData data = activeChecks.get(uuid);
        
        if (data != null) {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            data.evidence.add("[" + timestamp + "] " + evidence);
        }
    }
    
    /**
     * Get current check for player
     */
    public CheckData getCheck(UUID uuid) {
        return activeChecks.get(uuid);
    }
    
    /**
     * Check if player is being checked
     */
    public boolean isBeingChecked(UUID uuid) {
        return activeChecks.containsKey(uuid);
    }
    
    /**
     * Get all active checks
     */
    public Collection<CheckData> getActiveChecks() {
        return activeChecks.values();
    }
    
    /**
     * Get check count by type
     */
    public Map<CheckType, Integer> getCheckStats() {
        Map<CheckType, Integer> stats = new EnumMap<>(CheckType.class);
        for (CheckType type : CheckType.values()) {
            stats.put(type, 0);
        }
        for (CheckData data : activeChecks.values()) {
            stats.merge(data.checkType, 1, Integer::sum);
        }
        return stats;
    }
    
    /**
     * Open check menu for player
     */
    public void openCheckMenu(Player player, Player target) {
        // This would open an inventory GUI for check management
        // For now, just send the check types
        StringBuilder types = new StringBuilder();
        types.append("§e=== Типы проверок ===\n");
        
        for (CheckCategory category : CheckCategory.values()) {
            types.append("\n§6" + category.name() + ":\n");
            for (CheckType type : CheckType.values()) {
                if (type.getCategory() == category) {
                    types.append(" §f- §c").append(type.getDisplayName()).append("§7 (").append(type.getName()).append(")\n");
                }
            }
        }
        
        player.sendMessage(types.toString());
        player.sendMessage("§eИспользуйте: §c/check <player> <type> [reason]");
    }
    
    /**
     * Broadcast message to all staff
     */
    private void broadcastToStaff(String message) {
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("ajail.checks") || plugin.getAdminLevelManager().hasAdminLevel(staff)) {
                plugin.sendMessage(staff, message);
            }
        }
    }
    
    /**
     * Save check data to file
     */
    private void saveCheckData(CheckData data) {
        try {
            File checkDir = new File(plugin.getDataFolder(), "checks");
            if (!checkDir.exists()) {
                checkDir.mkdirs();
            }
            
            File checkFile = new File(checkDir, data.uuid.toString() + ".yml");
            
            StringBuilder content = new StringBuilder();
            content.append("# AJailFix Check Data\n");
            content.append("player: ").append(data.playerName).append("\n");
            content.append("check-type: ").append(data.checkType.getName()).append("\n");
            content.append("reason: ").append(data.reason).append("\n");
            content.append("initiated-by: ").append(data.initiatedByName).append("\n");
            content.append("start-time: ").append(data.startTime).append("\n");
            content.append("active: ").append(data.isActive).append("\n");
            
            Files.write(checkFile.toPath(), content.toString().getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save check data: " + e.getMessage());
        }
    }
    
    /**
     * Save check to history
     */
    private void saveCheckHistory(CheckData data, String verdict) {
        try {
            File historyDir = new File(plugin.getDataFolder(), "checks/history");
            if (!historyDir.exists()) {
                historyDir.mkdirs();
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            File historyFile = new File(historyDir, data.playerName + "_" + sdf.format(new Date()) + ".yml");
            
            StringBuilder content = new StringBuilder();
            content.append("# AJailFix Check History\n");
            content.append("player: ").append(data.playerName).append("\n");
            content.append("check-type: ").append(data.checkType.getName()).append("\n");
            content.append("reason: ").append(data.reason).append("\n");
            content.append("initiated-by: ").append(data.initiatedByName).append("\n");
            content.append("start-time: ").append(new Date(data.startTime)).append("\n");
            content.append("end-time: ").append(new Date()).append("\n");
            content.append("verdict: ").append(verdict).append("\n");
            content.append("notes: ").append(data.notes).append("\n");
            content.append("violations: ").append(data.violations).append("\n");
            
            if (!data.evidence.isEmpty()) {
                content.append("\n# Evidence\n");
                for (String evidence : data.evidence) {
                    content.append("evidence: '").append(evidence).append("'\n");
                }
            }
            
            Files.write(historyFile.toPath(), content.toString().getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save check history: " + e.getMessage());
        }
    }
    
    public void load() {
        // Load check data from files
        File checkDir = new File(plugin.getDataFolder(), "checks");
        if (!checkDir.exists()) {
            return;
        }
        
        // This would load any active checks that were in progress
    }
    
    public void save() {
        // Save all active checks
        for (CheckData data : activeChecks.values()) {
            saveCheckData(data);
        }
    }
}
