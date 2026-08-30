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

public class StaffListCommand implements CommandExecutor, TabCompleter {
    private final AJailFixPlugin plugin;
    
    public StaffListCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("stafflist").setExecutor(this);
        plugin.getCommand("stafflist").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ajail.suite.access") && !(sender instanceof Player && plugin.getAdminLevelManager().hasPermission((Player) sender, "ajail.suite.access"))) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cNo permission!");
            return true;
        }
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "&e&l=== Staff Online ===");
        int count = 0;
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("ajail.suite.access") || plugin.getAdminLevelManager().hasAdminLevel(p)) {
                count++;
                StringBuilder status = new StringBuilder();
                
                if (plugin.isInStaffMode(p.getUniqueId())) {
                    status.append(" &a[StaffMode]");
                }
                if (plugin.isVanished(p.getUniqueId())) {
                    status.append(" &5[Vanish]");
                }
                if (plugin.isFrozen(p.getUniqueId())) {
                    status.append(" &b[Frozen]");
                }
                
                int level = plugin.getAdminLevelManager().getAdminLevel(p.getUniqueId());
                if (level > 0) {
                    sender.sendMessage("&e" + p.getName() + " &7- Level " + level + status);
                } else {
                    sender.sendMessage("&e" + p.getName() + status);
                }
            }
        }
        
        sender.sendMessage("&7Total: &f" + count + " staff online");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
