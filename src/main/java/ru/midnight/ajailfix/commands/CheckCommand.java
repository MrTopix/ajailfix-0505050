package ru.midnight.ajailfix.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;
import ru.midnight.ajailfix.managers.AntiCheatManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class CheckCommand implements CommandExecutor, TabCompleter {
    
    private final AJailFixPlugin plugin;
    
    public CheckCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("check").setTabCompleter(this);
        plugin.getCommand("uncheck").setTabCompleter(this);
        plugin.getCommand("checks").setTabCompleter(this);
        plugin.getCommand("check").setExecutor(this);
        plugin.getCommand("uncheck").setExecutor(this);
        plugin.getCommand("checks").setExecutor(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cТолько для игроков!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("ajail.checks") && 
            !plugin.getAdminLevelManager().hasPermission(player, "ajail.checks")) {
            player.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cНет прав!"));
            return true;
        }
        
        String cmd = command.getName().toLowerCase();
        
        switch (cmd) {
            case "check" -> handleCheck(player, args);
            case "uncheck" -> handleUncheck(player, args);
            case "checks" -> handleChecks(player);
        }
        
        return true;
    }
    
    private void handleCheck(Player player, String[] args) {
        if (args.length < 1) {
            sendCheckHelp(player);
            return;
        }
        
        String targetName = args[0];
        
        // Check for offline player
        if (targetName.startsWith("offline:")) {
            player.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cНельзя проверять оффлайн игроков!"));
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cИгрок не найден: " + targetName));
            return;
        }
        
        // Get check type
        AntiCheatManager.CheckType checkType = AntiCheatManager.CheckType.FLY;
        String reason = null;
        
        if (args.length >= 2) {
            String typeStr = args[1].toLowerCase();
            reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;
            
            checkType = parseCheckType(typeStr);
        }
        
        // Start check
        plugin.getAntiCheatManager().startCheck(target, checkType, reason, player);
        
        player.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&aПроверка начата на игрока: &e" + target.getName()));
    }
    
    private void handleUncheck(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&eИспользование: &f/uncheck <игрок> [вердикт]"));
            return;
        }
        
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            player.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&cИгрок не найден: " + targetName));
            return;
        }
        
        String verdict = args.length >= 2 ? args[1] : "§eЗавершён";
        String notes = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";
        
        plugin.getAntiCheatManager().endCheck(target, verdict, notes);
        
        player.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&aПроверка завершена!"));
    }
    
    private void handleChecks(Player player) {
        var checks = plugin.getAntiCheatManager().getActiveChecks();
        
        if (checks.isEmpty()) {
            player.sendMessage(translateAlternateColorCodes('&', 
                plugin.getConfigManager().getPrefix() + "&eНет активных проверок!"));
            return;
        }
        
        player.sendMessage(translateAlternateColorCodes('&', 
            "&e&l=== Активные проверки (" + checks.size() + ") ==="));
        
        for (AntiCheatManager.CheckData check : checks) {
            String status = check.isActive ? "§aАктивна" : "§cЗавершена";
            player.sendMessage(translateAlternateColorCodes('&', 
                "§e" + check.playerName + " §7- §c" + check.checkType.getDisplayName() + 
                " §7(§f" + status + "§7)"));
        }
    }
    
    private AntiCheatManager.CheckType parseCheckType(String typeStr) {
        for (AntiCheatManager.CheckType type : AntiCheatManager.CheckType.values()) {
            if (type.getName().equalsIgnoreCase(typeStr) || 
                type.getDisplayName().equalsIgnoreCase(typeStr)) {
                return type;
            }
        }
        return AntiCheatManager.CheckType.FLY;
    }
    
    private void sendCheckHelp(Player player) {
        player.sendMessage(translateAlternateColorCodes('&', 
            plugin.getConfigManager().getPrefix() + "&e&l=== Проверка на читы ==="));
        player.sendMessage(translateAlternateColorCodes('&', 
            "&e/check <игрок> [тип] [причина] &7- Начать проверку"));
        player.sendMessage(translateAlternateColorCodes('&', 
            "&e/uncheck <игрок> [вердикт] &7- Завершить проверку"));
        player.sendMessage(translateAlternateColorCodes('&', 
            "&e/checks &7- Список активных проверок"));
        player.sendMessage(translateAlternateColorCodes('&', 
            "&e&l=== Типы проверок ==="));
        
        for (AntiCheatManager.CheckCategory category : AntiCheatManager.CheckCategory.values()) {
            player.sendMessage("");
            player.sendMessage(translateAlternateColorCodes('&', 
                category.getColor() + category.name()));
            
            for (AntiCheatManager.CheckType type : AntiCheatManager.CheckType.values()) {
                if (type.getCategory() == category) {
                    player.sendMessage("  §c" + type.getName() + " §7- §f" + type.getDisplayName());
                }
            }
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player)) return completions;
        Player player = (Player) sender;
        
        if (!player.hasPermission("ajail.checks") && 
            !plugin.getAdminLevelManager().hasPermission(player, "ajail.checks")) {
            return completions;
        }
        
        String cmd = command.getName().toLowerCase();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(input)) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 2 && cmd.equals("check")) {
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
