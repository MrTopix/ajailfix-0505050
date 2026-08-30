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

public class SpecCommand implements CommandExecutor, TabCompleter {
    private final AJailFixPlugin plugin;
    
    public SpecCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("spec").setExecutor(this);
        plugin.getCommand("spec").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "Console cannot use this!");
            return true;
        }
        
        if (!player.hasPermission("ajail.spec") && !plugin.getAdminLevelManager().hasPermission(player, "ajail.spec")) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + "&cNo permission!");
            return true;
        }
        
        if (args.length < 1) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + "&e/spec <player>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + "&cPlayer not found!");
            return true;
        }
        
        plugin.getTeleportManager().spectatorMode(player, target);
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
