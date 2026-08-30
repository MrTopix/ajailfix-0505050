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

public class PunishmentManager {
    
    private final AJailFixPlugin plugin;
    
    // Active bans: playerName -> BanData
    private final Map<String, BanData> bans = new ConcurrentHashMap<>();
    
    // Active mutes: playerName -> MuteData
    private final Map<String, MuteData> mutes = new ConcurrentHashMap<>();
    
    // Player UUID cache
    private final Map<String, UUID> nameToUUID = new ConcurrentHashMap<>();
    private final Map<UUID, String> uuidToName = new ConcurrentHashMap<>();
    
    // Player data
    private final Map<UUID, PlayerInfo> playerInfos = new ConcurrentHashMap<>();
    
    public static class BanData {
        public String playerName;
        public UUID playerUUID;
        public String reason;
        public String bannedBy;
        public long startTime;
        public long duration; // -1 for permanent
        public long endTime;
        public String ip;
        
        public BanData(String playerName, UUID uuid, String reason, String bannedBy, long duration, String ip) {
            this.playerName = playerName;
            this.playerUUID = uuid;
            this.reason = reason;
            this.bannedBy = bannedBy;
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
            this.endTime = duration > 0 ? startTime + duration : -1;
            this.ip = ip;
        }
        
        public boolean isPermanent() {
            return duration <= 0;
        }
        
        public boolean isExpired() {
            return !isPermanent() && System.currentTimeMillis() > endTime;
        }
        
        public String getTimeLeft() {
            if (isPermanent()) return "Permanent";
            long left = endTime - System.currentTimeMillis();
            return formatDuration(left);
        }
    }
    
    public static class MuteData {
        public String playerName;
        public UUID playerUUID;
        public String reason;
        public String mutedBy;
        public long startTime;
        public long duration;
        public long endTime;
        
        public MuteData(String playerName, UUID uuid, String reason, String mutedBy, long duration) {
            this.playerName = playerName;
            this.playerUUID = uuid;
            this.reason = reason;
            this.mutedBy = mutedBy;
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
            this.endTime = duration > 0 ? startTime + duration : -1;
        }
        
        public boolean isPermanent() {
            return duration <= 0;
        }
        
        public boolean isExpired() {
            return !isPermanent() && System.currentTimeMillis() > endTime;
        }
        
        public String getTimeLeft() {
            if (isPermanent()) return "Permanent";
            long left = endTime - System.currentTimeMillis();
            return formatDuration(left);
        }
    }
    
    public static class PlayerInfo {
        public UUID uuid;
        public String name;
        public String ip;
        public long firstJoin;
        public long lastJoin;
        public long playtime;
        public List<String> ips;
        public List<String> alts;
        public List<PunishmentRecord> punishments;
        public List<WarningRecord> warnings;
        public List<String> notes;
        
        public PlayerInfo(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
            this.firstJoin = System.currentTimeMillis();
            this.lastJoin = System.currentTimeMillis();
            this.playtime = 0;
            this.ips = new ArrayList<>();
            this.alts = new ArrayList<>();
            this.punishments = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.notes = new ArrayList<>();
        }
    }
    
    public static class PunishmentRecord {
        public String type;
        public String reason;
        public String by;
        public long time;
        public long duration;
        
        public PunishmentRecord(String type, String reason, String by, long duration) {
            this.type = type;
            this.reason = reason;
            this.by = by;
            this.time = System.currentTimeMillis();
            this.duration = duration;
        }
    }
    
    public static class WarningRecord {
        public int id;
        public String reason;
        public String by;
        public long time;
        public boolean active;
        
        public WarningRecord(int id, String reason, String by) {
            this.id = id;
            this.reason = reason;
            this.by = by;
            this.time = System.currentTimeMillis();
            this.active = true;
        }
    }
    
    public PunishmentManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
        load();
    }
    
    // Ban methods
    public void ban(Player target, String reason, Player admin, long duration) {
        ban(target.getName(), target.getUniqueId(), reason, admin.getName(), duration, getPlayerIP(target));
    }
    
    public void ban(String playerName, UUID uuid, String reason, String bannedBy, long duration, String ip) {
        BanData ban = new BanData(playerName, uuid, reason, bannedBy, duration, ip);
        bans.put(playerName.toLowerCase(), ban);
        
        // Kick player if online
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            String kickMsg = plugin.getConfigManager().getPrefix() + "§cВы заблокированы!\n" +
                "§eПричина: §f" + reason + "\n" +
                "§eАдминистратор: §f" + bannedBy + "\n" +
                "§eСрок: §f" + ban.getTimeLeft();
            player.kickPlayer(kickMsg.replace("§", ""));
        }
        
        // Add to ban list
        addBanToList(playerName, reason, bannedBy, duration);
        
        // Log
        plugin.getAuditManager().log("BAN", playerName, "By: " + bannedBy + ", Reason: " + reason + ", Duration: " + duration);
        
        saveBan(ban);
    }
    
    public void unban(String playerName, Player admin) {
        BanData ban = bans.remove(playerName.toLowerCase());
        if (ban != null) {
            plugin.sendMessage(admin, plugin.getConfigManager().getPrefix() + 
                "§aИгрок " + playerName + " разбанен!");
            
            plugin.getAuditManager().log("UNBAN", playerName, "By: " + admin.getName());
            
            removeBanFromList(playerName);
        } else {
            plugin.sendMessage(admin, plugin.getConfigManager().getPrefix() + 
                "§cИгрок " + playerName + " не найден в бане!");
        }
    }
    
    public BanData getBan(String playerName) {
        return bans.get(playerName.toLowerCase());
    }
    
    public boolean isBanned(String playerName) {
        BanData ban = bans.get(playerName.toLowerCase());
        return ban != null && !ban.isExpired();
    }
    
    public Collection<BanData> getBans() {
        return bans.values();
    }
    
    // Mute methods
    public void mute(Player target, String reason, Player admin, long duration) {
        mute(target.getName(), target.getUniqueId(), reason, admin.getName(), duration);
    }
    
    public void mute(String playerName, UUID uuid, String reason, String mutedBy, long duration) {
        MuteData mute = new MuteData(playerName, uuid, reason, mutedBy, duration);
        mutes.put(playerName.toLowerCase(), mute);
        
        // Log
        plugin.getAuditManager().log("MUTE", playerName, "By: " + mutedBy + ", Reason: " + reason + ", Duration: " + duration);
        
        saveMute(mute);
    }
    
    public void unmute(String playerName, Player admin) {
        MuteData mute = mutes.remove(playerName.toLowerCase());
        if (mute != null) {
            plugin.sendMessage(admin, plugin.getConfigManager().getPrefix() + 
                "§aС игрока " + playerName + " снят мут!");
            
            // Notify player
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                    "§aВам снят мут!");
            }
            
            plugin.getAuditManager().log("UNMUTE", playerName, "By: " + admin.getName());
        } else {
            plugin.sendMessage(admin, plugin.getConfigManager().getPrefix() + 
                "§cИгрок " + playerName + " не найден в муте!");
        }
    }
    
    public MuteData getMute(String playerName) {
        return mutes.get(playerName.toLowerCase());
    }
    
    public boolean isMuted(String playerName) {
        MuteData mute = mutes.get(playerName.toLowerCase());
        return mute != null && !mute.isExpired();
    }
    
    public Collection<MuteData> getMutes() {
        return mutes.values();
    }
    
    // Warning methods
    public int addWarning(String playerName, String reason, String by) {
        PlayerInfo info = getPlayerInfo(playerName);
        int id = info.warnings.size() + 1;
        info.warnings.add(new WarningRecord(id, reason, by));
        
        plugin.getAuditManager().log("WARN", playerName, "By: " + by + ", Reason: " + reason);
        
        // Notify player
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cВам выдано предупреждение!");
            plugin.sendMessage(player, "§eПричина: §f" + reason);
        }
        
        savePlayerInfo(info);
        return id;
    }
    
    public void removeWarning(String playerName, int warningId, Player admin) {
        PlayerInfo info = getPlayerInfo(playerName);
        
        for (int i = 0; i < info.warnings.size(); i++) {
            if (info.warnings.get(i).id == warningId) {
                info.warnings.get(i).active = false;
                plugin.sendMessage(admin, plugin.getConfigManager().getPrefix() + 
                    "§aПредупреждение #" + warningId + " снято!");
                plugin.getAuditManager().log("UNWARN", playerName, "Warning #" + warningId + " by " + admin.getName());
                savePlayerInfo(info);
                return;
            }
        }
        
        plugin.sendMessage(admin, plugin.getConfigManager().getPrefix() + 
            "§cПредупреждение #" + warningId + " не найдено!");
    }
    
    public int getActiveWarnings(String playerName) {
        PlayerInfo info = getPlayerInfo(playerName);
        return (int) info.warnings.stream().filter(w -> w.active).count();
    }
    
    // Player info methods
    public PlayerInfo getPlayerInfo(String playerName) {
        UUID uuid = getPlayerUUID(playerName);
        if (uuid != null) {
            return getPlayerInfo(uuid);
        }
        
        // Create new info
        PlayerInfo info = new PlayerInfo(null, playerName);
        playerInfos.put(uuid, info);
        return info;
    }
    
    public PlayerInfo getPlayerInfo(UUID uuid) {
        return playerInfos.computeIfAbsent(uuid, k -> {
            PlayerInfo info = loadPlayerInfo(uuid);
            if (info != null) return info;
            
            Player player = Bukkit.getPlayer(uuid);
            String name = player != null ? player.getName() : "Unknown";
            return new PlayerInfo(uuid, name);
        });
    }
    
    public UUID getPlayerUUID(String name) {
        // Check cache first
        UUID cached = nameToUUID.get(name.toLowerCase());
        if (cached != null) return cached;
        
        // Check online players
        Player online = Bukkit.getPlayer(name);
        if (online != null) {
            UUID uuid = online.getUniqueId();
            nameToUUID.put(name.toLowerCase(), uuid);
            uuidToName.put(uuid, name);
            return uuid;
        }
        
        // Load from storage
        PlayerInfo info = loadPlayerInfoFromFile(name);
        if (info != null) {
            nameToUUID.put(name.toLowerCase(), info.uuid);
            uuidToName.put(info.uuid, info.name);
            return info.uuid;
        }
        
        return null;
    }
    
    public String getPlayerName(UUID uuid) {
        String cached = uuidToName.get(uuid);
        if (cached != null) return cached;
        
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            String name = online.getName();
            uuidToName.put(uuid, name);
            nameToUUID.put(name.toLowerCase(), uuid);
            return name;
        }
        
        return null;
    }
    
    public String getPlayerIP(Player player) {
        return player.getAddress().getAddress().getHostAddress();
    }
    
    // Utility methods
    public static String formatDuration(long millis) {
        if (millis <= 0) return "Permanent";
        
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours % 24 > 0) {
            sb.append(hours % 24).append("h ");
        }
        if (minutes % 60 > 0) {
            sb.append(minutes % 60).append("m ");
        }
        if (seconds % 60 > 0 && days == 0) {
            sb.append(seconds % 60).append("s");
        }
        
        return sb.toString().trim();
    }
    
    public static long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) return -1;
        
        duration = duration.toLowerCase().trim();
        
        if (duration.equals("perm") || duration.equals("permanent") || duration.equals("-1")) {
            return -1;
        }
        
        long total = 0;
        StringBuilder num = new StringBuilder();
        
        for (char c : duration.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                long value = num.length() > 0 ? Long.parseLong(num.toString()) : 1;
                
                switch (c) {
                    case 's' -> total += value * 1000;
                    case 'm' -> total += value * 60 * 1000;
                    case 'h' -> total += value * 60 * 60 * 1000;
                    case 'd' -> total += value * 24 * 60 * 60 * 1000;
                    case 'w' -> total += value * 7 * 24 * 60 * 60 * 1000;
                }
                
                num = new StringBuilder();
            }
        }
        
        return total;
    }
    
    // Private save/load methods
    private void addBanToList(String playerName, String reason, String bannedBy, long duration) {
        // Implementation for adding to ban list (for kick message)
    }
    
    private void removeBanFromList(String playerName) {
        // Implementation for removing from ban list
    }
    
    private void saveBan(BanData ban) {
        try {
            File dir = new File(plugin.getDataFolder(), "punishments/bans");
            if (!dir.exists()) dir.mkdirs();
            
            File file = new File(dir, ban.playerName.toLowerCase() + ".yml");
            StringBuilder content = new StringBuilder();
            content.append("player: ").append(ban.playerName).append("\n");
            content.append("uuid: ").append(ban.playerUUID).append("\n");
            content.append("reason: '").append(ban.reason.replace("'", "''")).append("'\n");
            content.append("bannedBy: ").append(ban.bannedBy).append("\n");
            content.append("startTime: ").append(ban.startTime).append("\n");
            content.append("duration: ").append(ban.duration).append("\n");
            content.append("ip: ").append(ban.ip != null ? ban.ip : "").append("\n");
            
            Files.write(file.toPath(), content.toString().getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save ban: " + e.getMessage());
        }
    }
    
    private void saveMute(MuteData mute) {
        try {
            File dir = new File(plugin.getDataFolder(), "punishments/mutes");
            if (!dir.exists()) dir.mkdirs();
            
            File file = new File(dir, mute.playerName.toLowerCase() + ".yml");
            StringBuilder content = new StringBuilder();
            content.append("player: ").append(mute.playerName).append("\n");
            content.append("uuid: ").append(mute.playerUUID).append("\n");
            content.append("reason: '").append(mute.reason.replace("'", "''")).append("'\n");
            content.append("mutedBy: ").append(mute.mutedBy).append("\n");
            content.append("startTime: ").append(mute.startTime).append("\n");
            content.append("duration: ").append(mute.duration).append("\n");
            
            Files.write(file.toPath(), content.toString().getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save mute: " + e.getMessage());
        }
    }
    
    private void savePlayerInfo(PlayerInfo info) {
        try {
            File dir = new File(plugin.getDataFolder(), "data");
            if (!dir.exists()) dir.mkdirs();
            
            File file = new File(dir, (info.uuid != null ? info.uuid.toString() : info.name) + ".yml");
            StringBuilder content = new StringBuilder();
            content.append("name: ").append(info.name).append("\n");
            if (info.uuid != null) {
                content.append("uuid: ").append(info.uuid).append("\n");
            }
            content.append("firstJoin: ").append(info.firstJoin).append("\n");
            content.append("lastJoin: ").append(info.lastJoin).append("\n");
            content.append("playtime: ").append(info.playtime).append("\n");
            content.append("ip: ").append(info.ip != null ? info.ip : "").append("\n");
            
            for (String alt : info.alts) {
                content.append("alt: ").append(alt).append("\n");
            }
            
            for (String note : info.notes) {
                content.append("note: '").append(note.replace("'", "''")).append("'\n");
            }
            
            Files.write(file.toPath(), content.toString().getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save player info: " + e.getMessage());
        }
    }
    
    private PlayerInfo loadPlayerInfo(UUID uuid) {
        File dir = new File(plugin.getDataFolder(), "data");
        File file = new File(dir, uuid.toString() + ".yml");
        
        if (!file.exists()) return null;
        
        // Parse YAML file
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            PlayerInfo info = new PlayerInfo(uuid, uuid.toString());
            
            // Parse lines...
            return info;
        } catch (IOException e) {
            return null;
        }
    }
    
    private PlayerInfo loadPlayerInfoFromFile(String playerName) {
        File dir = new File(plugin.getDataFolder(), "data");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        
        if (files == null) return null;
        
        for (File file : files) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines) {
                    if (line.startsWith("name: ") && line.substring(6).equals(playerName)) {
                        String uuidStr = file.getName().replace(".yml", "");
                        UUID uuid = UUID.fromString(uuidStr);
                        return loadPlayerInfo(uuid);
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        return null;
    }
    
    public void load() {
        loadBans();
        loadMutes();
    }
    
    private void loadBans() {
        File dir = new File(plugin.getDataFolder(), "punishments/bans");
        if (!dir.exists()) return;
        
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        
        for (File file : files) {
            // Load ban data
            try {
                // Parse YAML
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load ban: " + file.getName());
            }
        }
    }
    
    private void loadMutes() {
        File dir = new File(plugin.getDataFolder(), "punishments/mutes");
        if (!dir.exists()) return;
        
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        
        for (File file : files) {
            // Load mute data
            try {
                // Parse YAML
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load mute: " + file.getName());
            }
        }
    }
    
    public void save() {
        for (BanData ban : bans.values()) {
            saveBan(ban);
        }
        for (MuteData mute : mutes.values()) {
            saveMute(mute);
        }
        for (PlayerInfo info : playerInfos.values()) {
            savePlayerInfo(info);
        }
    }
}
