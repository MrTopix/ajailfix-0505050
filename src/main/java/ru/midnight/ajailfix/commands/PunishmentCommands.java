package ru.midnight.ajailfix.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;
import ru.midnight.ajailfix.managers.PunishmentManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class PunishmentCommands implements CommandExecutor, TabCompleter {
    
    private final AJailFixPlugin plugin;
    
    public PunishmentCommands(AJailFixPlugin plugin) {
        this.plugin = plugin;
        // Register commands
        plugin.getCommand("ban").setTabCompleter(this);
        plugin.getCommand("unban").setTabCompleter(this);
        plugin.getCommand("mute").setTabCompleter(this);
        plugin.getCommand("unmute").setTabCompleter(this);
        plugin.getCommand("kick").setTabCompleter(this);
        plugin.getCommand("warn").setTabCompleter(this);
        plugin.getCommand("unwarn").setTabCompleter(this);
        
        plugin.getCommand("ban").setExecutor(this);
        plugin.getCommand("unban").setExecutor(this);
        plugin.getCommand("mute").setExecutor(this);
        plugin.getCommand("unmute").setExecutor(this);
        plugin.getCommand("kick").setExecutor(this);
        plugin.getCommand("warn").setExecutor(this);
        plugin.getCommand("unwarn").setExecutor(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        Player player = sender instanceof Player ? (Player) sender : null;
        
        switch (cmd) {
            case "ban" -> handleBan(sender, player, args);
            case "unban" -> handleUnban(sender, player, args);
            case "mute" -> handleMute(sender, player, args);
            case "unmute" -> handleUnmute(sender, player, args);
            case "kick" -> handleKick(sender, player, args);
            case "warn" -> handleWarn(sender, player, args);
            case "unwarn" -> handleUnwarn(sender, player, args);
        }
        
        return true;
    }
    
    private void handleBan(CommandSender sender, Player executor, String[] args) {
        if (!hasPermission(sender, "ajail.ban")) return;
        if (args.length < 1) {
            sendUsage(sender, "/ban <игрок> [время] [причина]");
            return;
        }
        
        String targetName = args[0];
        long duration = -1; // Permanent
        String reason = "Нарушение правил";
        
        if (args.length >= 2) {
            duration = PunishmentManager.parseDuration(args[1]);
            if (args.length >= 3) {
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
        }
        
        Player target = Bukkit.getPlayer(targetName);
        String executorName = executor != null ? executor.getName() : "Console";
        
        if (target != null) {
            plugin.getPunishmentManager().ban(target, reason, executor, duration);
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&aИгрок забанен!"));
        } else {
            UUID uuid = plugin.getPunishmentManager().getPlayerUUID(targetName);
            if (uuid != null) {
                plugin.getPunishmentManager().ban(targetName, uuid, reason, executorName, duration, null);
                sender.sendMessage(translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getPrefix() + "&aИгрок забанен!"));
            } else {
                sender.sendMessage(translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getPrefix() + "&cИгрок не найден!"));
            }
        }
    }
    
    private void handleUnban(CommandSender sender, Player executor, String[] args) {
        if (!hasPermission(sender, "ajail.unban")) return;
        if (args.length < 1) {
            sendUsage(sender, "/unban <игрок>");
            return;
        }
        
        String targetName = args[0];
        plugin.getPunishmentManager().unban(targetName, executor);
    }
    
    private void handleMute(CommandSender sender, Player executor, String[] args) {
        if (!hasPermission(sender, "ajail.mute")) return;
        if (args.length < 1) {
            sendUsage(sender, "/mute <игрок> [время] [причина]");
            return;
        }
        
        String targetName = args[0];
        long duration = -1;
        String reason = "Нарушение правил чата";
        
        if (args.length >= 2) {
            duration = PunishmentManager.parseDuration(args[1]);
            if (args.length >= 3) {
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
        }
        
        Player target = Bukkit.getPlayer(targetName);
        String executorName = executor != null ? executor.getName() : "Console";
        
        if (target != null) {
            plugin.getPunishmentManager().mute(target, reason, executor, duration);
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&aИгрок замучен!"));
        } else {
            UUID uuid = plugin.getPunishmentManager().getPlayerUUID(targetName);
            if (uuid != null) {
                plugin.getPunishmentManager().mute(targetName, uuid, reason, executorName, duration);
                sender.sendMessage(translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getPrefix() + "&aИгрок замучен!"));
            } else {
                sender.sendMessage(translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getPrefix() + "&cИгрок не найден!"));
            }
        }
    }
    
    private void handleUnmute(CommandSender sender, Player executor, String[] args) {
        if (!hasPermission(sender, "ajail.unmute")) return;
        if (args.length < 1) {
            sendUsage(sender, "/unmute <игрок>");
            return;
        }
        
        plugin.getPunishmentManager().unmute(args[0], executor);
    }
    
    private void handleKick(CommandSender sender, Player executor, String[] args) {
        if (!hasPermission(sender, "ajail.ban")) return;
        if (args.length < 1) {
            sendUsage(sender, "/kick <игрок> [причина]");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cИгрок не найден!"));
            return;
        }
        
        String reason = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Кик администратором";
        String executorName = executor != null ? executor.getName() : "Console";
        
        target.kickPlayer(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "§cВы кикнуты!\n§eПричина: §f" + reason + "\n§eАдминистратор: §f" + executorName));
        
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&aИгрок кикнут!"));
        
        plugin.getAuditManager().log("KICK", target.getName(), "By: " + executorName + ", Reason: " + reason);
    }
    
    private void handleWarn(CommandSender sender, Player executor, String[] args) {
        if (!hasPermission(sender, "ajail.suite.access")) return;
        if (args.length < 2) {
            sendUsage(sender, "/warn <игрок> <причина>");
            return;
        }
        
        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String executorName = executor != null ? executor.getName() : "Console";
        
        int warningId = plugin.getPunishmentManager().addWarning(targetName, reason, executorName);
        
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&aПредупреждение #" + warningId + " выдано!"));
    }
    
    private void handleUnwarn(CommandSender sender, Player executor, String[] args) {
        if (!hasPermission(sender, "ajail.suite.access")) return;
        if (args.length < 1) {
            sendUsage(sender, "/unwarn <игрок> [id]");
            return;
        }
        
        String targetName = args[0];
        int warningId = args.length >= 2 ? Integer.parseInt(args[1]) : -1;
        
        if (warningId > 0) {
            plugin.getPunishmentManager().removeWarning(targetName, warningId, executor);
        } else {
            // Remove latest warning
            var info = plugin.getPunishmentManager().getPlayerInfo(targetName);
            if (!info.warnings.isEmpty()) {
                int latestId = info.warnings.get(info.warnings.size() - 1).id;
                plugin.getPunishmentManager().removeWarning(targetName, latestId, executor);
            }
        }
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
    
    private void sendUsage(CommandSender sender, String usage) {
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&eИспользование: &f" + usage));
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
        } else if (args.length == 2 && (command.getName().equals("ban") || command.getName().equals("mute"))) {
            String input = args[1].toLowerCase();
            String[] times = {"1m", "5m", "15m", "30m", "1h", "2h", "6h", "12h", "1d", "7d", "30d", "perm"};
            for (String time : times) {
                if (time.startsWith(input)) {
                    completions.add(time);
                }
            }
        }
        
        return completions;
    }
}
