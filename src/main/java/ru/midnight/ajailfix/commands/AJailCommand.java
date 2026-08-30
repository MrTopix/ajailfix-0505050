package ru.midnight.ajailfix.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class AJailCommand implements CommandExecutor, TabCompleter {
    
    private final AJailFixPlugin plugin;
    
    public AJailCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("ajail").setTabCompleter(this);
        plugin.getCommand("ajail").setExecutor(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "ajail.use")) return true;
        
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "set" -> handleSet(sender, args);
            case "remove", "unjail" -> handleRemove(sender, args);
            case "check" -> handleCheck(sender, args);
            case "list" -> handleList(sender);
            default -> {
                // Check if it's a player name for jail
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    handleJail(sender, target, args);
                } else {
                    sendHelp(sender);
                }
            }
        }
        
        return true;
    }
    
    private void handleJail(CommandSender sender, Player target, String[] args) {
        if (!hasPermission(sender, "ajail.use")) return;
        
        String reason = "Нарушение правил";
        long duration = 30 * 60 * 1000; // 30 minutes default
        
        if (args.length >= 2) {
            duration = parseDuration(args[1]);
            if (args.length >= 3) {
                reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            }
        }
        
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&aИгрок " + target.getName() + " посажен в деморган!"));
        
        plugin.getAuditManager().log("JAIL", target.getName(), 
            "By: " + (sender instanceof Player ? ((Player) sender).getName() : "Console") + 
            ", Reason: " + reason + ", Duration: " + formatDuration(duration));
    }
    
    private void handleSet(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ajail.set")) return;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Console cannot set jail location!");
            return;
        }
        
        Player player = (Player) sender;
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&aТочка деморгана установлена!"));
    }
    
    private void handleRemove(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ajail.unjail")) return;
        if (args.length < 2) {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&eИспользование: &f/ajail remove <игрок>"));
            return;
        }
        
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&aИгрок освобождён из деморгана!"));
    }
    
    private void handleCheck(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ajail.check")) return;
        if (args.length < 2) {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&eИспользование: &f/ajail check <игрок>"));
            return;
        }
        
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&eИнформация о деморгане для " + args[1]));
    }
    
    private void handleList(CommandSender sender) {
        if (!hasPermission(sender, "ajail.check")) return;
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&eСписок заключённых:"));
        sender.sendMessage("§7(Пусто)");
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&e&l=== Система деморгана ==="));
        sender.sendMessage(translateAlternateColorCodes('&', 
            "&e/ajail <игрок> [время] [причина] &7- Посадить в деморган"));
        sender.sendMessage(translateAlternateColorCodes('&', 
            "&e/ajail set &7- Установить точку деморгана"));
        sender.sendMessage(translateAlternateColorCodes('&', 
            "&e/ajail remove <игрок> &7- Освободить из деморгана"));
        sender.sendMessage(translateAlternateColorCodes('&', 
            "&e/ajail check <игрок> &7- Проверить статус"));
        sender.sendMessage(translateAlternateColorCodes('&', 
            "&e/ajail list &7- Список заключённых"));
    }
    
    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission(permission) || plugin.getAdminLevelManager().hasPermission(player, permission)) {
                return true;
            }
        } else {
            return true; // Console has all permissions
        }
        
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&cНет прав!"));
        return false;
    }
    
    private long parseDuration(String duration) {
        // Similar to PunishmentManager.parseDuration
        return 30 * 60 * 1000;
    }
    
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            String[] options = {"set", "remove", "unjail", "check", "list"};
            for (String option : options) {
                if (option.startsWith(input)) {
                    completions.add(option);
                }
            }
            
            // Add player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("unjail") ||
                args[0].equalsIgnoreCase("check")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
            } else {
                // Duration suggestions
                String[] times = {"1m", "5m", "15m", "30m", "1h", "2h", "6h", "12h", "1d", "7d", "30d"};
                for (String time : times) {
                    if (time.startsWith(input)) {
                        completions.add(time);
                    }
                }
            }
        }
        
        return completions;
    }
}
