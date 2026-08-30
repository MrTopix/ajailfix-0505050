package ru.midnight.ajailfix.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {
    private final AJailFixPlugin plugin;
    
    public ReloadCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("reloadajail").setExecutor(this);
        plugin.getCommand("reloadajail").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ajail.reload") && !(sender instanceof Player && plugin.getAdminLevelManager().hasPermission((Player) sender, "ajail.reload"))) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cNo permission!");
            return true;
        }
        
        plugin.getConfigManager().load();
        plugin.getAdminLevelManager().load();
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "&aConfiguration reloaded!");
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
