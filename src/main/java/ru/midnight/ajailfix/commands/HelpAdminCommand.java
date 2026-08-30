package ru.midnight.ajailfix.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.ArrayList;
import java.util.List;

public class HelpAdminCommand implements CommandExecutor, TabCompleter {
    private final AJailFixPlugin plugin;
    
    public HelpAdminCommand(AJailFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("helpadmin").setExecutor(this);
        plugin.getCommand("adminhelp").setExecutor(this);
        plugin.getCommand("helpadmin").setTabCompleter(this);
        plugin.getCommand("adminhelp").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ajail.suite.access") && !(sender instanceof Player && plugin.getAdminLevelManager().hasPermission((Player) sender, "ajail.suite.access"))) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "&cNo permission!");
            return true;
        }
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "&e&l=== Admin Commands ===");
        sender.sendMessage("");
        sender.sendMessage("&eModeration:");
        sender.sendMessage("  &f/tp <player> &7- Teleport to player");
        sender.sendMessage("  &f/gh <player> &7- Bring player to you");
        sender.sendMessage("  &f/invsee <player> &7- View inventory");
        sender.sendMessage("  &f/spec <player> &7- Spectate player");
        sender.sendMessage("  &f/freeze <player> &7- Freeze player");
        sender.sendMessage("  &f/back &7- Return to last location");
        sender.sendMessage("");
        sender.sendMessage("&ePunishments:");
        sender.sendMessage("  &f/ban <player> [time] [reason] &7- Ban player");
        sender.sendMessage("  &f/mute <player> [time] [reason] &7- Mute player");
        sender.sendMessage("  &f/kick <player> [reason] &7- Kick player");
        sender.sendMessage("  &f/warn <player> <reason> &7- Warn player");
        sender.sendMessage("");
        sender.sendMessage("&eAdministration:");
        sender.sendMessage("  &f/adminpanel [player] &7- Admin panel");
        sender.sendMessage("  &f/check <player> [type] &7- Start check");
        sender.sendMessage("  &f/adminlevel <player> <1-6> &7- Set admin level");
        sender.sendMessage("  &f/staffmode &7- Toggle staff mode");
        sender.sendMessage("  &f/vanish &7- Toggle vanish");
        sender.sendMessage("");
        sender.sendMessage("&eChat:");
        sender.sendMessage("  &f/a <msg> &7- Admin chat");
        sender.sendMessage("  &f/o <msg> &7- Global announcement");
        sender.sendMessage("  &f/clearchat &7- Clear chat");
        sender.sendMessage("  &f/mutechat &7- Toggle chat mute");
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
