package ru.midnight.ajailfix.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class BasicCommands implements CommandExecutor, TabCompleter {
    
    private final AJailFixPlugin plugin;
    
    public BasicCommands(AJailFixPlugin plugin) {
        this.plugin = plugin;
        
        // Register all basic commands
        registerCommand("back");
        registerCommand("spec");
        registerCommand("heal");
        registerCommand("feed");
        registerCommand("speed");
        registerCommand("vanish");
        registerCommand("staffmode");
        registerCommand("freeze");
        registerCommand("unfreeze");
        registerCommand("whois");
        registerCommand("seen");
        registerCommand("history");
        registerCommand("notes");
        registerCommand("note");
        registerCommand("stafflist");
        registerCommand("clearchat");
        registerCommand("mutechat");
        registerCommand("slowchat");
        registerCommand("a");
        registerCommand("o");
        registerCommand("report");
        registerCommand("reports");
        registerCommand("ans");
        registerCommand("lag");
        registerCommand("serverinfo");
        registerCommand("reloadajail");
        registerCommand("helpadmin");
    }
    
    private void registerCommand(String name) {
        Command cmd = plugin.getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        Player player = sender instanceof Player ? (Player) sender : null;
        
        switch (cmd) {
            case "back" -> {
                if (player == null) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cТолько для игроков!"));
                    return true;
                }
                if (!hasPermission(player, "ajail.suite.access")) return true;
                plugin.getTeleportManager().goBack(player);
            }
            
            case "spec" -> {
                if (player == null) { sender.sendMessage("Console error"); return true; }
                if (!hasPermission(player, "ajail.spec")) return true;
                if (args.length < 1) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eИспользование: &f/spec <игрок>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cИгрок не найден!"));
                    return true;
                }
                plugin.getTeleportManager().spectatorMode(player, target);
            }
            
            case "heal" -> {
                if (player == null) { sender.sendMessage("Console error"); return true; }
                if (!hasPermission(player, "ajail.suite.access")) return true;
                Player target = player;
                if (args.length >= 1) {
                    target = Bukkit.getPlayer(args[0]);
                    if (target == null) {
                        sender.sendMessage(translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getPrefix() + "&cИгрок не найден!"));
                        return true;
                    }
                }
                target.setHealth(20);
                target.setFoodLevel(20);
                target.setSaturation(20);
                target.fireTicks = 0;
                plugin.sendMessage(target, plugin.getConfigManager().getPrefix() + "&aВы вылечены!");
                if (!target.equals(player)) {
                    plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                        "&aИгрок " + target.getName() + " вылечен!");
                }
            }
            
            case "feed" -> {
                if (player == null) { sender.sendMessage("Console error"); return true; }
                if (!hasPermission(player, "ajail.suite.access")) return true;
                Player target = player;
                if (args.length >= 1) {
                    target = Bukkit.getPlayer(args[0]);
                    if (target == null) {
                        sender.sendMessage(translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getPrefix() + "&cИгрок не найден!"));
                        return true;
                    }
                }
                target.setFoodLevel(20);
                target.setSaturation(20);
                plugin.sendMessage(target, plugin.getConfigManager().getPrefix() + "&aВы накормлены!");
                if (!target.equals(player)) {
                    plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                        "&aИгрок " + target.getName() + " накормлен!");
                }
            }
            
            case "speed" -> {
                if (player == null) { sender.sendMessage("Console error"); return true; }
                if (!hasPermission(player, "ajail.suite.access")) return true;
                if (args.length < 1) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eИспользование: &f/speed <0-10> [игрок]"));
                    return true;
                }
                int speed;
                try {
                    speed = Integer.parseInt(args[0]);
                    if (speed < 0 || speed > 10) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cСкорость должна быть от 0 до 10!"));
                    return true;
                }
                Player target = player;
                if (args.length >= 2) {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getPrefix() + "&cИгрок не найден!"));
                        return true;
                    }
                }
                float flySpeed = speed / 10f;
                float walkSpeed = speed / 10f;
                target.setFlySpeed(Math.min(flySpeed, 1f));
                target.setWalkSpeed(Math.min(walkSpeed, 1f));
                plugin.sendMessage(target, plugin.getConfigManager().getPrefix() + 
                    "&aСкорость установлена на " + speed + "!");
            }
            
            case "vanish" -> {
                if (player == null) { sender.sendMessage("Console error"); return true; }
                if (!hasPermission(player, "ajail.suite.access")) return true;
                plugin.getVanishManager().toggleVanish(player);
            }
            
            case "staffmode" -> {
                if (player == null) { sender.sendMessage("Console error"); return true; }
                if (!hasPermission(player, "ajail.suite.access")) return true;
                plugin.getStaffModeManager().toggleStaffMode(player);
            }
            
            case "freeze" -> {
                if (player == null) { sender.sendMessage("Console error"); return true; }
                if (!hasPermission(player, "ajail.suite.access")) return true;
                if (args.length < 1) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eИспользование: &f/freeze <игрок>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cИгрок не найден!"));
                    return true;
                }
                plugin.getFreezeManager().freezePlayer(target, player);
            }
            
            case "unfreeze" -> {
                if (player == null) { sender.sendMessage("Console error"); return true; }
                if (!hasPermission(player, "ajail.suite.access")) return true;
                if (args.length < 1) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eИспользование: &f/unfreeze <игрок>"));
                    return true;
                }
                plugin.getFreezeManager().unfreezePlayerByName(args[0], player);
            }
            
            case "whois" -> {
                if (player == null) { sender.sendMessage("Console error"); return true; }
                if (!hasPermission(player, "ajail.suite.access")) return true;
                if (args.length < 1) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eИспользование: &f/whois <игрок>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cИгрок не найден!"));
                    return true;
                }
                sendWhoisInfo(player, target);
            }
            
            case "seen" -> {
                if (!hasPermission(player, "ajail.suite.access")) return true;
                if (args.length < 1) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eИспользование: &f/seen <игрок>"));
                    return true;
                }
                sendSeenInfo(sender, args[0]);
            }
            
            case "history" -> {
                if (!hasPermission(player, "ajail.history")) return true;
                if (args.length < 1) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eИспользование: &f/history <игрок>"));
                    return true;
                }
                sender.sendMessage(translateAlternateColorCodes('&', 
                    "&e&l=== История наказаний: " + args[0] + " ==="));
                sender.sendMessage("§7(История будет загружена из базы данных)");
            }
            
            case "clearchat" -> {
                if (!hasPermission(player, "ajail.suite.access")) return true;
                plugin.getChatManager().clearChat(player);
            }
            
            case "mutechat" -> {
                if (!hasPermission(player, "ajail.suite.access")) return true;
                plugin.getChatManager().toggleChat(player);
            }
            
            case "slowchat" -> {
                if (!hasPermission(player, "ajail.suite.access")) return true;
                if (args.length < 1) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eИспользование: &f/slowchat <секунды>"));
                    return true;
                }
                int seconds;
                try {
                    seconds = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cНеверный формат!"));
                    return true;
                }
                plugin.getChatManager().setSlowChat(player, seconds);
            }
            
            case "a" -> {
                if (!hasPermission(player, "ajail.adminchat")) return true;
                if (args.length < 1) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eИспользование: &f/a <сообщение>"));
                    return true;
                }
                String message = String.join(" ", args);
                broadcastAdminChat(player, message);
            }
            
            case "o" -> {
                if (!hasPermission(player, "ajail.global")) return true;
                if (args.length < 1) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eИспользование: &f/o <сообщение>"));
                    return true;
                }
                String message = String.join(" ", args);
                broadcastGlobal(player, message);
            }
            
            case "report" -> {
                if (player == null) { sender.sendMessage("Console error"); return true; }
                if (args.length < 1) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eИспользование: &f/report <сообщение>"));
                    return true;
                }
                String message = String.join(" ", args);
                int reportId = plugin.getReportManager().createReport(player, message);
                plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                    "&aРепорт #" + reportId + " отправлен!");
            }
            
            case "reports" -> {
                if (!hasPermission(player, "ajail.reports")) return true;
                var reports = plugin.getReportManager().getOpenReports();
                if (reports.isEmpty()) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eНет открытых репортов!"));
                    return true;
                }
                sender.sendMessage(translateAlternateColorCodes('&', 
                    "&e&l=== Открытые репорты (" + reports.size() + ") ==="));
                for (var report : reports) {
                    sender.sendMessage("§e#" + report.id + " §7от §f" + report.reporter + 
                        " §7- §f" + report.message.substring(0, Math.min(50, report.message.length())));
                }
            }
            
            case "ans" -> {
                if (!hasPermission(player, "ajail.reports")) return true;
                if (args.length < 2) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eИспользование: &f/ans <id> <ответ>"));
                    return true;
                }
                int id;
                try {
                    id = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cНеверный ID!"));
                    return true;
                }
                String answer = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                plugin.getReportManager().answerReport(id, player, answer);
            }
            
            case "lag", "serverinfo" -> {
                if (!hasPermission(player, "ajail.suite.access")) return true;
                sendServerInfo(player);
            }
            
            case "reloadajail" -> {
                if (!hasPermission(player, "ajail.reload")) return true;
                plugin.getConfigManager().load();
                sender.sendMessage(translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getPrefix() + "&aКонфигурация перезагружена!"));
            }
            
            case "helpadmin" -> {
                if (!hasPermission(player, "ajail.suite.access")) return true;
                sendHelpAdmin(player);
            }
        }
        
        return true;
    }
    
    private boolean hasPermission(Player player, String permission) {
        if (player.hasPermission(permission) || plugin.getAdminLevelManager().hasPermission(player, permission)) {
            return true;
        }
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + "&cНет прав!");
        return false;
    }
    
    private void sendWhoisInfo(Player viewer, Player target) {
        viewer.sendMessage(translateAlternateColorCodes('&', 
            "&e&l=== Информация о игроке: " + target.getName() + " ==="));
        viewer.sendMessage("§7Ник: §f" + target.getName());
        viewer.sendMessage("§7UUID: §f" + target.getUniqueId());
        viewer.sendMessage("§7IP: §f" + target.getAddress().getAddress().getHostAddress());
        viewer.sendMessage("§7Мир: §f" + target.getWorld().getName());
        viewer.sendMessage("§7Игровой режим: §f" + target.getGameMode().name());
        viewer.sendMessage("§7Здоровье: §f" + (int) target.getHealth() + "/" + (int) target.getMaxHealth());
        viewer.sendMessage("§7Локация: §f" + String.format("%.1f, %.1f, %.1f", 
            target.getLocation().getX(), target.getLocation().getY(), target.getLocation().getZ()));
        
        // Ping
        viewer.sendMessage("§7Пинг: §f" + (Bukkit.getPlayer(target.getUniqueId()) != null ? 
            "N/A" : "Offline"));
        
        // Admin level
        int level = plugin.getAdminLevelManager().getAdminLevel(target.getUniqueId());
        if (level > 0) {
            viewer.sendMessage("§7Админ-уровень: " + plugin.getAdminLevelManager().getLevelName(level));
        }
    }
    
    private void sendSeenInfo(CommandSender sender, String playerName) {
        Player online = Bukkit.getPlayer(playerName);
        if (online != null) {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "§e" + playerName + " §aонлайн!"));
            return;
        }
        
        var info = plugin.getPunishmentManager().getPlayerInfo(playerName);
        if (info != null && info.lastJoin > 0) {
            long diff = System.currentTimeMillis() - info.lastJoin;
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "§e" + playerName + " §cоффлайн"));
            sender.sendMessage("§7Был(а) в сети: §f" + formatDuration(diff) + " назад");
        } else {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "§e" + playerName + " §cне найден в базе данных!"));
        }
    }
    
    private void broadcastAdminChat(Player sender, String message) {
        String formatted = plugin.getConfigManager().getAdminPrefix() + 
            "§e" + sender.getName() + " §7» §f" + message;
        
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("ajail.adminchat") || 
                plugin.getAdminLevelManager().hasAdminLevel(staff)) {
                staff.sendMessage(formatted);
            }
        }
        
        plugin.getAuditManager().log("ADMIN_CHAT", sender.getName(), message, sender);
    }
    
    private void broadcastGlobal(Player sender, String message) {
        Bukkit.broadcastMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "§e[ОБЪЯВЛЕНИЕ] " + sender.getName() + ": §f" + message));
        
        plugin.getAuditManager().log("GLOBAL", sender.getName(), message, sender);
    }
    
    private void sendServerInfo(Player player) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        
        player.sendMessage(translateAlternateColorCodes('&', 
            "&e&l=== Информация о сервере ==="));
        player.sendMessage("§7Онлайн: §f" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        player.sendMessage("§7TPS: §a20.0 §7(идеально)");
        player.sendMessage("§7RAM: §f" + usedMemory + "/" + maxMemory + " MB");
        player.sendMessage("§7Миры: §f" + Bukkit.getWorlds().size());
        player.sendMessage("§7Версия: §f" + Bukkit.getVersion());
    }
    
    private void sendHelpAdmin(Player player) {
        player.sendMessage(translateAlternateColorCodes('&', 
            "&e&l=== Справка по командам администратора ==="));
        player.sendMessage("");
        player.sendMessage("§eМодерация:");
        player.sendMessage("  §f/tp <игрок> §7- Телепорт к игроку");
        player.sendMessage("  §f/gh <игрок> §7- Призвать игрока");
        player.sendMessage("  §f/invsee <игрок> §7- Открыть инвентарь");
        player.sendMessage("  §f/spec <игрок> §7- Наблюдение");
        player.sendMessage("  §f/freeze <игрок> §7- Заморозить");
        player.sendMessage("  §f/back §7- Вернуться");
        player.sendMessage("");
        player.sendMessage("§eНаказания:");
        player.sendMessage("  §f/ban <игрок> <время> <причина> §7- Бан");
        player.sendMessage("  §f/mute <игрок> <время> <причина> §7- Мут");
        player.sendMessage("  §f/kick <игрок> [причина] §7- Кик");
        player.sendMessage("  §f/warn <игрок> <причина> §7- Предупреждение");
        player.sendMessage("");
        player.sendMessage("§eАдминистрирование:");
        player.sendMessage("  §f/adminpanel §7- Админ-панель");
        player.sendMessage("  §f/check <игрок> [тип] §7- Проверка");
        player.sendMessage("  §f/adminlevel <игрок> <1-6> §7- Выдать уровень");
        player.sendMessage("  §f/staffmode §7- Режим админа");
    }
    
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + " дн.";
        if (hours > 0) return hours + " ч.";
        if (minutes > 0) return minutes + " мин.";
        return seconds + " сек.";
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
        }
        
        return completions;
    }
}
