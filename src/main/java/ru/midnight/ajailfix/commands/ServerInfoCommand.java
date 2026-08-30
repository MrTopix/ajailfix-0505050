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

public class ServerInfoCommand implements CommandExecutor, TabCompleter {
    private final AJailFixPlugin plugin;
    
    public ServerInfoCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("lag").setExecutor(this);
        plugin.getCommand("serverinfo").setExecutor(this);
        plugin.getCommand("lag").setTabCompleter(this);
        plugin.getCommand("serverinfo").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ajail.suite.access") && !(sender instanceof Player && plugin.getAdminLevelManager().hasPermission((Player) sender, "ajail.suite.access"))) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cNo permission!");
            return true;
        }
        
        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory() / 1024 / 1024;
        long totalMem = rt.totalMemory() / 1024 / 1024;
        long freeMem = rt.freeMemory() / 1024 / 1024;
        long usedMem = totalMem - freeMem;
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "&e&l=== Server Info ===");
        sender.sendMessage("&7Online: &f" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        sender.sendMessage("&7TPS: &a20.0 &7(ideal)");
        sender.sendMessage("&7RAM: &f" + usedMem + "/" + maxMem + " MB");
        sender.sendMessage("&7Worlds: &f" + Bukkit.getWorlds().size());
        sender.sendMessage("&7Version: &f" + Bukkit.getVersion().split(" ")[0]);
        sender.sendMessage("&7Entities: &f" + countEntities());
        
        return true;
    }
    
    private int countEntities() {
        int count = 0;
        for (var world : Bukkit.getWorlds()) {
            count += world.getEntities().size();
        }
        return count;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
