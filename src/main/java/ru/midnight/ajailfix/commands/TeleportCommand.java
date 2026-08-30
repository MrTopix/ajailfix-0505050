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
import java.util.UUID;
import java.util.stream.Collectors;

import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class TeleportCommand implements CommandExecutor, TabCompleter {
    
    private final AJailFixPlugin plugin;
    
    public TeleportCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("tp").setTabCompleter(this);
        plugin.getCommand("goto").setTabCompleter(this);
        plugin.getCommand("gh").setTabCompleter(this);
        plugin.getCommand("gethere").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        
        switch (cmd) {
            case "tp", "goto" -> {
                if (args.length < 1) {
                    sendHelp(sender);
                    return true;
                }
                
                String targetName = args[0];
                
                // Check for offline player
                if (targetName.startsWith("offline:")) {
                    targetName = targetName.substring(8);
                    Player player = sender instanceof Player ? (Player) sender : null;
                    if (player == null) {
                        sender.sendMessage(translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getPrefix() + "&cКонсоль не может телепортироваться!"));
                        return true;
                    }
                    plugin.getTeleportManager().teleportToOfflinePlayer(player, targetName);
                    return true;
                }
                
                Player target = Bukkit.getPlayer(targetName);
                if (target == null) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cИгрок не найден: " + targetName));
                    return true;
                }
                
                if (!(sender instanceof Player)) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cКонсоль не может телепортироваться!"));
                    return true;
                }
                
                Player player = (Player) sender;
                plugin.getTeleportManager().teleportToPlayer(player, target);
                return true;
            }
            
            case "gh", "gethere" -> {
                if (args.length < 1) {
                    sendHelp(sender);
                    return true;
                }
                
                String targetName = args[0];
                
                // Check for offline player
                if (targetName.startsWith("offline:")) {
                    targetName = targetName.substring(8);
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&eТелепортация на последнюю известную локацию оффлайн игрока..."));
                    
                    if (sender instanceof Player player) {
                        plugin.getTeleportManager().teleportToOfflinePlayer(player, targetName);
                    }
                    return true;
                }
                
                Player target = Bukkit.getPlayer(targetName);
                if (target == null) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cИгрок не найден: " + targetName));
                    return true;
                }
                
                if (!(sender instanceof Player)) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cКонсоль не может телепортировать!"));
                    return true;
                }
                
                Player player = (Player) sender;
                plugin.getTeleportManager().bringPlayer(target, player);
                return true;
            }
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&eИспользование: &f/tp <игрок>"));
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&eИспользование: &f/gh <игрок> (призвать к себе)"));
        sender.sendMessage(translateAlternateColorCodes('&', 
            "&7Для оффлайн игрока используйте: &foffline:<ник>"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            
            // Add online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
            
            // Add offline player option
            if (input.length() > 0) {
                completions.add("offline:" + input);
            }
        }
        
        return completions;
    }
}
