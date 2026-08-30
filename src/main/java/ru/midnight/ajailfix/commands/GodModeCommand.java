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

public class GodModeCommand implements CommandExecutor, TabCompleter {
    
    private final AJailFixPlugin plugin;
    
    public GodModeCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("gm").setTabCompleter(this);
        plugin.getCommand("godmode").setTabCompleter(this);
        plugin.getCommand("fly").setTabCompleter(this);
        plugin.getCommand("gm").setExecutor(this);
        plugin.getCommand("godmode").setExecutor(this);
        plugin.getCommand("fly").setExecutor(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        
        switch (cmd) {
            case "gm", "godmode" -> {
                if (!sender.hasPermission("ajail.godmode") && 
                    !(sender instanceof Player && plugin.getAdminLevelManager().hasPermission((Player) sender, "ajail.godmode"))) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cНет прав!"));
                    return true;
                }
                
                if (args.length >= 1 && sender.hasPermission("ajail.godmode.others")) {
                    // Toggle god for another player
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target == null) {
                        sender.sendMessage(translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getPrefix() + "&cИгрок не найден!"));
                        return true;
                    }
                    
                    if (target.isInvulnerable()) {
                        target.setInvulnerable(false);
                        sender.sendMessage(translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getPrefix() + "&aGod Mode выключен для &e" + target.getName()));
                    } else {
                        target.setInvulnerable(true);
                        sender.sendMessage(translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getPrefix() + "&aGod Mode включён для &e" + target.getName()));
                    }
                } else if (sender instanceof Player) {
                    // Toggle god for self
                    Player player = (Player) sender;
                    if (player.isInvulnerable()) {
                        player.setInvulnerable(false);
                        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                            "&cGod Mode выключен!");
                    } else {
                        player.setInvulnerable(true);
                        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                            "&aGod Mode включён!");
                    }
                } else {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cУкажите игрока!"));
                }
            }
            
            case "fly" -> {
                if (!sender.hasPermission("ajail.fly") && 
                    !(sender instanceof Player && plugin.getAdminLevelManager().hasPermission((Player) sender, "ajail.fly"))) {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cНет прав!"));
                    return true;
                }
                
                if (args.length >= 1 && sender.hasPermission("ajail.fly.others")) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target == null) {
                        sender.sendMessage(translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getPrefix() + "&cИгрок не найден!"));
                        return true;
                    }
                    
                    if (target.getAllowFlight()) {
                        target.setAllowFlight(false);
                        target.setFlying(false);
                        sender.sendMessage(translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getPrefix() + "&cFly выключен для &e" + target.getName()));
                    } else {
                        target.setAllowFlight(true);
                        sender.sendMessage(translateAlternateColorCodes('&', 
                            plugin.getConfigManager().getPrefix() + "&aFly включён для &e" + target.getName()));
                    }
                } else if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (player.getAllowFlight()) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                            "&cFly выключен!");
                    } else {
                        player.setAllowFlight(true);
                        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                            "&aFly включён!");
                    }
                } else {
                    sender.sendMessage(translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getPrefix() + "&cУкажите игрока!"));
                }
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender.hasPermission("ajail.godmode.others")) {
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
