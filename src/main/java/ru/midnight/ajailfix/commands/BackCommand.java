package ru.midnight.ajailfix.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.ArrayList;
import java.util.List;

public class BackCommand implements CommandExecutor, TabCompleter {
    private final AJailFixPlugin plugin;
    
    public BackCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("back").setExecutor(this);
        plugin.getCommand("back").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "Console cannot use this!");
            return true;
        }
        
        if (!player.hasPermission("ajail.suite.access") && !plugin.getAdminLevelManager().hasPermission(player, "ajail.suite.access")) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + "&cNo permission!");
            return true;
        }
        
        plugin.getTeleportManager().goBack(player);
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
