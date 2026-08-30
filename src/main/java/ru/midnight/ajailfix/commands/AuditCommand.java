package ru.midnight.ajailfix.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.ArrayList;
import java.util.List;

public class AuditCommand implements CommandExecutor, TabCompleter {
    private final AJailFixPlugin plugin;
    
    public AuditCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("audit").setExecutor(this);
        plugin.getCommand("audit").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ajail.suite.access") && !(sender instanceof Player && plugin.getAdminLevelManager().hasPermission((Player) sender, "ajail.suite.access"))) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cNo permission!");
            return true;
        }
        
        int limit = 10;
        if (args.length >= 1) {
            try {
                limit = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "&e&l=== Audit Log ===");
        
        var entries = plugin.getAuditManager().getRecentEntries(limit);
        for (var entry : entries) {
            sender.sendMessage("&7[" + entry.action + "] &e" + entry.playerName + " &7" + 
                (entry.details != null ? "(" + entry.details + ")" : ""));
        }
        
        if (entries.isEmpty()) {
            sender.sendMessage("&7No recent entries");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
