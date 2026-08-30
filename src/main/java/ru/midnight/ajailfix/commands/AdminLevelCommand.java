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

import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class AdminLevelCommand implements CommandExecutor, TabCompleter {
    
    private final AJailFixPlugin plugin;
    
    public AdminLevelCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("adminlevel").setTabCompleter(this);
        plugin.getCommand("adminlevel").setExecutor(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ajail.adminlevels.manage") && 
            !plugin.getAdminLevelManager().hasPermission((Player) sender, "ajail.adminlevels.manage")) {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cНет прав!"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&eИспользование: &f/adminlevel <игрок> <1-6|remove>"));
            sender.sendMessage(translateAlternateColorCodes('&', 
                "&7Уровни: &a1-3 &7Модераторы, &e4-5 &7Администраторы, &c6 &7Главный Админ"));
            return true;
        }
        
        String targetName = args[0];
        String levelStr = args[1].toLowerCase();
        
        Player target = Bukkit.getPlayer(targetName);
        
        if (levelStr.equals("remove") || levelStr.equals("0")) {
            // Remove admin level
            if (target != null) {
                plugin.getAdminLevelManager().removeAdminLevel(target);
                sender.sendMessage(translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getPrefix() + "&aС игрока &e" + target.getName() + 
                    " &aснята администрация!"));
            } else {
                sender.sendMessage(translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getPrefix() + "&cИгрок не онлайн! Для снятия нужен онлайн статус."));
                sender.sendMessage(translateAlternateColorCodes('&', 
                    "&eПри снятии администрации GM, Fly, Vanish и StaffMode автоматически отключаются."));
            }
            return true;
        }
        
        int level;
        try {
            level = Integer.parseInt(levelStr);
            if (level < 1 || level > 6) {
                sender.sendMessage(translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getPrefix() + "&cУровень должен быть от 1 до 6!"));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cНеверный формат уровня!"));
            return true;
        }
        
        if (target == null) {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cИгрок не найден: " + targetName));
            return true;
        }
        
        // Set admin level
        plugin.getAdminLevelManager().setAdminLevel(target, level);
        
        sender.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&aИгроку &e" + target.getName() + 
            " &aвыдан уровень администрации: " + plugin.getAdminLevelManager().getLevelName(level)));
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("ajail.adminlevels.manage") && 
            !(sender instanceof Player && plugin.getAdminLevelManager().hasPermission((Player) sender, "ajail.adminlevels.manage"))) {
            return completions;
        }
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            String[] levels = {"1", "2", "3", "4", "5", "6", "remove"};
            for (String level : levels) {
                if (level.startsWith(input)) {
                    completions.add(level);
                }
            }
        }
        
        return completions;
    }
}
