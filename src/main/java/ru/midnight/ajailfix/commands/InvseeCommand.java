package ru.midnight.ajailfix.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class InvseeCommand implements CommandExecutor, TabCompleter {
    
    private final AJailFixPlugin plugin;
    
    public InvseeCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("invsee").setTabCompleter(this);
        plugin.getCommand("endersee").setTabCompleter(this);
        plugin.getCommand("armorsee").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cТолько для игроков!"));
            return true;
        }
        
        if (args.length < 1) {
            sendHelp((Player) sender, command.getName());
            return true;
        }
        
        Player player = (Player) sender;
        String targetName = args[0];
        
        // Handle offline players
        if (targetName.startsWith("offline:")) {
            targetName = targetName.substring(8);
            player.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&eПросмотр инвентаря оффлайн игрока: &f" + targetName));
            
            // For offline players, we would need to load their data from file
            // For now, just show a message
            player.sendMessage(translateAlternateColorCodes('&', 
                "&cОффлайн инвентарь не реализован в этой версии. Используйте онлайн игроков."));
            return true;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cИгрок не найден: " + targetName));
            return true;
        }
        
        switch (command.getName().toLowerCase()) {
            case "invsee" -> openInventory(player, target);
            case "endersee" -> openEnderChest(player, target);
            case "armorsee" -> openArmorInventory(player, target);
        }
        
        return true;
    }
    
    private void openInventory(Player viewer, Player target) {
        String title = "§5§lИнвентарь: §f" + target.getName();
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        // Copy target's inventory
        inv.setContents(target.getInventory().getContents());
        
        viewer.openInventory(inv);
        
        viewer.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&aОткрыт инвентарь игрока: &e" + target.getName()));
    }
    
    private void openEnderChest(Player viewer, Player target) {
        String title = "§6§lЭндер-сундук: §f" + target.getName();
        Inventory inv = Bukkit.createInventory(null, 27, title);
        
        // Copy ender chest contents
        inv.setContents(target.getEnderChest().getContents());
        
        viewer.openInventory(inv);
        
        viewer.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&aОткрыт эндер-сундук игрока: &e" + target.getName()));
    }
    
    private void openArmorInventory(Player viewer, Player target) {
        String title = "§9§lБроня: §f" + target.getName();
        Inventory inv = Bukkit.createInventory(null, 9, title);
        
        // Copy armor contents
        inv.setContents(target.getInventory().getArmorContents());
        
        // Add off-hand
        inv.setItem(8, target.getInventory().getItemInOffHand());
        
        viewer.openInventory(inv);
        
        viewer.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&aОткрыта броня игрока: &e" + target.getName()));
    }
    
    private void sendHelp(Player player, String command) {
        player.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&eИспользование: &f/" + command + " <игрок>"));
        player.sendMessage(translateAlternateColorCodes('&', 
            "&7Для оффлайн игрока используйте: &foffline:<ник>"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
            
            if (input.length() > 0) {
                completions.add("offline:" + input);
            }
        }
        
        return completions;
    }
}
