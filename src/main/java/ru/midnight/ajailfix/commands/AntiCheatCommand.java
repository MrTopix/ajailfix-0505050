package ru.midnight.ajailfix.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.midnight.ajailfix.AJailFixPlugin;
import ru.midnight.ajailfix.managers.AntiCheatManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class AntiCheatCommand implements CommandExecutor, TabCompleter {
    
    private final AJailFixPlugin plugin;
    
    public AntiCheatCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("ac").setTabCompleter(this);
        plugin.getCommand("anticheat").setTabCompleter(this);
        plugin.getCommand("ac").setExecutor(this);
        plugin.getCommand("anticheat").setExecutor(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cТолько для игроков!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!hasPermission(player, "ajail.checks")) return true;
        
        if (args.length >= 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                openCheckMenu(player, target, args);
            } else {
                player.sendMessage(translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getPrefix() + "&cИгрок не найден: " + args[0]));
            }
        } else {
            openAntiCheatMenu(player);
        }
        
        return true;
    }
    
    private void openAntiCheatMenu(Player player) {
        String title = "§8§l[ §c§lАнтичит §8§l]";
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        // Active checks
        int checkCount = plugin.getAntiCheatManager().getActiveChecks().size();
        setItem(inv, 0, Material.BLAZE_ROD, "§c§lАктивные проверки", 
            Arrays.asList("§7Количество: §f" + checkCount,
                         "§7Нажмите для просмотра"));
        
        // Check categories
        int slot = 9;
        for (AntiCheatManager.CheckCategory category : AntiCheatManager.CheckCategory.values()) {
            Material mat = switch (category) {
                case MOVEMENT -> Material.FEATHER;
                case COMBAT -> Material.DIAMOND_SWORD;
                case PLAYER -> Material.PLAYER_HEAD;
                case OTHER -> Material.BOOK;
            };
            
            setItem(inv, slot, mat, category.getColor() + "§l" + category.name(),
                Arrays.asList("§7Типы проверок:",
                             getCheckTypesForCategory(category)));
            
            slot++;
        }
        
        // Stats
        setItem(inv, 45, Material.BAR_CHART, "§e§lСтатистика", 
            Arrays.asList("§7Активных проверок: §f" + checkCount));
        
        player.openInventory(inv);
    }
    
    private void openCheckMenu(Player player, Player target, String[] args) {
        String title = "§8§l[ §c§lПроверка: " + target.getName() + " §8§l]";
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        // Check types by category
        int slot = 0;
        for (AntiCheatManager.CheckCategory category : AntiCheatManager.CheckCategory.values()) {
            setItem(inv, slot, Material.STAINED_GLASS_PANE, 
                category.getColor() + "§l" + category.name(),
                Arrays.asList("§7Проверки категории:"));
            
            for (AntiCheatManager.CheckType type : AntiCheatManager.CheckType.values()) {
                if (type.getCategory() == category) {
                    slot++;
                    setItem(inv, slot, Material.BLAZE_ROD, 
                        "§c" + type.getDisplayName(),
                        Arrays.asList("§7Нажмите для начала проверки",
                                     "§7на §f" + type.getDisplayName()));
                }
            }
            slot++;
        }
        
        // Player info
        setItem(inv, 45, Material.PLAYER_HEAD, "§e§l" + target.getName(),
            Arrays.asList("§7Проверка игрока: §f" + target.getName(),
                         "§7UUID: §f" + target.getUniqueId()));
        
        // Start with reason
        setItem(inv, 49, Material.EMERALD_BLOCK, "§a§lНачать проверку",
            Arrays.asList("§7Выберите тип проверки",
                         "§7из меню выше"));
        
        player.openInventory(inv);
    }
    
    private String getCheckTypesForCategory(AntiCheatManager.CheckCategory category) {
        List<String> types = new ArrayList<>();
        for (AntiCheatManager.CheckType type : AntiCheatManager.CheckType.values()) {
            if (type.getCategory() == category) {
                types.add("§c- " + type.getDisplayName());
            }
        }
        return String.join("\n", types);
    }
    
    private void setItem(Inventory inv, int slot, Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }
    
    private boolean hasPermission(Player player, String permission) {
        if (player.hasPermission(permission) || plugin.getAdminLevelManager().hasPermission(player, permission)) {
            return true;
        }
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + "&cНет прав!");
        return false;
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
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            for (AntiCheatManager.CheckType type : AntiCheatManager.CheckType.values()) {
                if (type.getName().toLowerCase().startsWith(input)) {
                    completions.add(type.getName());
                }
            }
        }
        
        return completions;
    }
}
