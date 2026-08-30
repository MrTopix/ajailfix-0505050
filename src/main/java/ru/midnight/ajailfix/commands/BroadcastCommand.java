package ru.midnight.ajailfix.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.ArrayList;
import java.util.List;

public class BroadcastCommand implements CommandExecutor, TabCompleter {
    private final AJailFixPlugin plugin;
    
    public BroadcastCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("broadcast").setExecutor(this);
        plugin.getCommand("o").setExecutor(this);
        plugin.getCommand("broadcast").setTabCompleter(this);
        plugin.getCommand("o").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ajail.global") && !(sender instanceof Player && plugin.getAdminLevelManager().hasPermission((Player) sender, "ajail.global"))) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cNo permission!");
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "&e/broadcast <message>");
            return true;
        }
        
        String message = String.join(" ", args);
        String senderName = sender instanceof Player ? ((Player) sender).getName() : "Console";
        
        plugin.broadcast(plugin.getConfigManager().getPrefix() + "&e[ANNOUNCEMENT] " + senderName + ": &f" + message);
        
        plugin.getAuditManager().log("BROADCAST", senderName, message, sender instanceof Player ? (Player) sender : null);
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
