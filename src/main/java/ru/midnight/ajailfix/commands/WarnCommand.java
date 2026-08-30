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

public class WarnCommand implements CommandExecutor, TabCompleter {
    private final AJailFixPlugin plugin;
    
    public WarnCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("warn").setExecutor(this);
        plugin.getCommand("unwarn").setExecutor(this);
        plugin.getCommand("warn").setTabCompleter(this);
        plugin.getCommand("unwarn").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ajail.suite.access") && !(sender instanceof Player && plugin.getAdminLevelManager().hasPermission((Player) sender, "ajail.suite.access"))) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cNo permission!");
            return true;
        }
        
        String cmd = command.getName().toLowerCase();
        
        if (cmd.equals("warn")) {
            if (args.length < 2) {
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "&e/warn <player> <reason>");
                return true;
            }
            
            String targetName = args[0];
            String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            String byName = sender instanceof Player ? ((Player) sender).getName() : "Console";
            
            int warningId = plugin.getPunishmentManager().addWarning(targetName, reason, byName);
            
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                plugin.sendMessage(target, plugin.getConfigManager().getPrefix() + "&cYou have been warned!");
                plugin.sendMessage(target, "&eReason: &f" + reason);
            }
            
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "&aWarning #" + warningId + " issued for " + targetName);
            
        } else if (cmd.equals("unwarn")) {
            if (args.length < 1) {
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "&e/unwarn <player> [id]");
                return true;
            }
            
            String targetName = args[0];
            int warningId = args.length >= 2 ? Integer.parseInt(args[1]) : -1;
            
            if (warningId > 0) {
                plugin.getPunishmentManager().removeWarning(targetName, warningId, sender instanceof Player ? (Player) sender : null);
            } else {
                var info = plugin.getPunishmentManager().getPlayerInfo(targetName);
                if (!info.warnings.isEmpty()) {
                    int latestId = info.warnings.get(info.warnings.size() - 1).id;
                    plugin.getPunishmentManager().removeWarning(targetName, latestId, sender instanceof Player ? (Player) sender : null);
                }
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(input)) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }
}
