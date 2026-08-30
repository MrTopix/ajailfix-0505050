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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class AdminPanelCommand implements CommandExecutor, TabCompleter {
    
    private final AJailFixPlugin plugin;
    
    public AdminPanelCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("adminpanel").setTabCompleter(this);
        plugin.getCommand("adminpanel").setExecutor(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cТолько для игроков!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!hasPermission(player, "ajail.suite.access")) return true;
        
        if (args.length >= 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                openPlayerPanel(player, target);
            } else {
                player.sendMessage(translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getPrefix() + "&cИгрок не найден: " + args[0]));
            }
        } else {
            openMainPanel(player);
        }
        
        return true;
    }
    
    private void openMainPanel(Player player) {
        String title = "§8§l[ §c§lАдмин-Панель §8§l]";
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        // Row 1 - Server management
        setItem(inv, 0, Material.PAPER, "§e§lРепорты", 
            Arrays.asList("§7Открытые репорты: §f" + plugin.getReportManager().getOpenCount(),
                         "§7Нажмите для просмотра"));
        setItem(inv, 1, Material.BOOK, "§c§lАктивные проверки", 
            Arrays.asList("§7Активных проверок: §f" + plugin.getAntiCheatManager().getActiveChecks().size(),
                         "§7Нажмите для просмотра"));
        setItem(inv, 2, Material.REDSTONE_BLOCK, "§c§lБанлист", 
            Arrays.asList("§7Активных банов: §f" + plugin.getPunishmentManager().getBans().size(),
                         "§7Нажмите для просмотра"));
        setItem(inv, 3, Material.BEACON, "§6§lМутлист", 
            Arrays.asList("§7Активных мутов: §f" + plugin.getPunishmentManager().getMutes().size(),
                         "§7Нажмите для просмотра"));
        
        // Row 2 - Quick actions
        setItem(inv, 9, Material.COMPASS, "§b§lТелепорт", 
            Arrays.asList("§7Быстрый телепорт",
                         "§7Нажмите для меню"));
        setItem(inv, 10, Material.ENDER_EYE, "§d§lПросмотр игроков", 
            Arrays.asList("§7Инвентарь, броня, эндер-сундук",
                         "§7Нажмите для меню"));
        setItem(inv, 11, Material.BLAZE_ROD, "§c§lЗаморозка", 
            Arrays.asList("§7Управление заморозкой",
                         "§7Нажмите для меню"));
        setItem(inv, 12, Material.NETHER_STAR, "§a§lРежим админа", 
            Arrays.asList("§7Staff Mode",
                         "§7Нажмите для включения"));
        
        // Row 3 - Server tools
        setItem(inv, 18, Material.BARRIER, "§c§lОчистка чата", 
            Arrays.asList("§7Очистить чат для всех"));
        setItem(inv, 19, Material.CLOCK, "§e§lИнформация", 
            Arrays.asList("§7Информация о сервере"));
        setItem(inv, 20, Material.COMMAND_BLOCK, "§a§lАудит", 
            Arrays.asList("§7Журнал действий админов"));
        setItem(inv, 21, Material.CHEST, "§6§lУправление", 
            Arrays.asList("§7Настройки плагина"));
        
        player.openInventory(inv);
    }
    
    private void openPlayerPanel(Player player, Player target) {
        String title = "§8§l[ §c§lУправление: " + target.getName() + " §8§l]";
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        // Basic info row
        setItem(inv, 0, Material.PLAYER_HEAD, "§e§l" + target.getName(), 
            Arrays.asList("§7UUID: §f" + target.getUniqueId(),
                         "§7IP: §f" + target.getAddress().getAddress().getHostAddress(),
                         "§7Мир: §f" + target.getWorld().getName()));
        
        // Actions row
        setItem(inv, 9, Material.COMPASS, "§b§lТелепорт", 
            Arrays.asList("§7Телепортироваться к игроку"));
        setItem(inv, 10, Material.ENDER_EYE, "§b§lПризвать", 
            Arrays.asList("§7Призвать игрока к себе"));
        setItem(inv, 11, Material.ENDER_PEARL, "§d§lSpectate", 
            Arrays.asList("§7Наблюдать за игроком"));
        
        // Inventory actions
        setItem(inv, 18, Material.CHEST, "§6§lИнвентарь", 
            Arrays.asList("§7Просмотр инвентаря"));
        setItem(inv, 19, Material.ENDER_CHEST, "§6§lЭндер-сундук", 
            Arrays.asList("§7Просмотр эндер-сундука"));
        setItem(inv, 20, Material.DIAMOND_CHESTPLATE, "§6§lБроня", 
            Arrays.asList("§7Просмотр брони"));
        
        // Freeze and check
        setItem(inv, 27, Material.ICE, "§b§lЗаморозить", 
            Arrays.asList("§7Заморозить игрока"));
        setItem(inv, 28, Material.BLAZE_ROD, "§c§lПроверка", 
            Arrays.asList("§7Начать проверку на читы"));
        setItem(inv, 29, Material.BOOK, "§e§lИстория", 
            Arrays.asList("§7История наказаний"));
        
        // Punishments
        setItem(inv, 36, Material.REDSTONE_BLOCK, "§c§lБан", 
            Arrays.asList("§7Забанить игрока"));
        setItem(inv, 37, Material.IRON_BARS, "§c§lМут", 
            Arrays.asList("§7Замутить игрока"));
        setItem(inv, 38, Material.BEETROOT_SOUP, "§a§lHeal", 
            Arrays.asList("§7Вылечить игрока"));
        setItem(inv, 39, Material.COOKED_BEEF, "§a§lFeed", 
            Arrays.asList("§7Накормить игрока"));
        
        // Close
        setItem(inv, 49, Material.BARRIER, "§c§lЗакрыть", 
            Arrays.asList("§7Закрыть панель"));
        
        player.openInventory(inv);
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
        }
        
        return completions;
    }
}
