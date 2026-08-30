package ru.midnight.ajailfix.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandListener implements Listener {
    
    private final AJailFixPlugin plugin;
    private final Map<UUID, Long> commandCooldowns = new HashMap<>();
    private static final long COMMAND_SPAM_COOLDOWN = 500; // 500ms
    
    public CommandListener(AJailFixPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        
        // Spam protection
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastCommand = commandCooldowns.get(uuid);
        
        if (lastCommand != null && (now - lastCommand) < COMMAND_SPAM_COOLDOWN) {
            // Anti-spam
            int violations = getSpamViolations(uuid) + 1;
            setSpamViolations(uuid, violations);
            
            if (violations >= 5) {
                plugin.getPunishmentManager().mute(player, "Command spam",
                    Bukkit.getConsoleSender(), 5 * 60 * 1000);
                plugin.sendMessage(player, "§cАвтоматический мут за спам команд!");
                event.setCancelled(true);
                return;
            }
        }
        
        commandCooldowns.put(uuid, now);
        
        // Check if command exists
        if (!isValidCommand(command)) {
            plugin.sendMessage(player, "§cКоманда не найдена. Введите /help для списка команд.");
        }
    }
    
    private boolean isValidCommand(String command) {
        // Get all registered commands
        var commands = Bukkit.getServer().getCommands();
        
        // Simple check - if command starts with known prefix
        String cmdName = command.split(" ")[0].substring(1); // Remove /
        
        for (var cmd : commands) {
            if (cmd.getName().equalsIgnoreCase(cmdName)) {
                return true;
            }
            if (cmd.getAliases() != null) {
                for (String alias : cmd.getAliases()) {
                    if (alias.equalsIgnoreCase(cmdName)) {
                        return true;
                    }
                }
            }
        }
        
        // Allow AJailFix commands
        return cmdName.startsWith("ajail") || cmdName.startsWith("admin") ||
               cmdName.startsWith("check") || cmdName.startsWith("report") ||
               cmdName.startsWith("staff") || cmdName.startsWith("mute") ||
               cmdName.startsWith("ban") || cmdName.startsWith("warn") ||
               cmdName.startsWith("tp") || cmdName.startsWith("goto") ||
               cmdName.startsWith("spec") || cmdName.startsWith("whois") ||
               cmdName.startsWith("seen") || cmdName.startsWith("invsee") ||
               cmdName.startsWith("fly") || cmdName.startsWith("gm") ||
               cmdName.startsWith("heal") || cmdName.startsWith("feed") ||
               cmdName.startsWith("freeze") || cmdName.startsWith("vanish") ||
               cmdName.startsWith("staffmode") || cmdName.startsWith("duty") ||
               cmdName.startsWith("helpadmin") || cmdName.startsWith("lag") ||
               cmdName.startsWith("serverinfo") || cmdName.startsWith("clearchat") ||
               cmdName.startsWith("broadcast") || cmdName.startsWith("asms") ||
               cmdName.startsWith("a") || cmdName.startsWith("o") ||
               cmdName.startsWith("anticheat") || cmdName.startsWith("ac");
    }
    
    private final Map<UUID, Integer> spamViolations = new HashMap<>();
    
    private int getSpamViolations(UUID uuid) {
        return spamViolations.getOrDefault(uuid, 0);
    }
    
    private void setSpamViolations(UUID uuid, int violations) {
        if (violations <= 0) {
            spamViolations.remove(uuid);
        } else {
            spamViolations.put(uuid, violations);
        }
    }
}
