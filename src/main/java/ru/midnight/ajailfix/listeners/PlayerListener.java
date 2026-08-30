package ru.midnight.ajailfix.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.midnight.ajailfix.AJailFixPlugin;

public class PlayerListener implements Listener {
    
    private final AJailFixPlugin plugin;
    
    public PlayerListener(AJailFixPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Update player data
        var info = plugin.getPunishmentManager().getPlayerInfo(player);
        info.lastJoin = System.currentTimeMillis();
        info.setIpAddress(player.getAddress().getAddress().getHostAddress());
        
        // Update vanish visibility
        plugin.getVanishManager().onPlayerJoin(player);
        
        // Check if banned
        if (plugin.getPunishmentManager().isBanned(player.getName())) {
            var ban = plugin.getPunishmentManager().getBan(player.getName());
            String kickMsg = plugin.getConfigManager().getPrefix() + "§cВы заблокированы!\n" +
                "§eПричина: §f" + ban.reason + "\n" +
                "§eАдминистратор: §f" + ban.bannedBy + "\n" +
                "§eСрок: §f" + ban.getTimeLeft();
            player.kickPlayer(kickMsg.replace("§", ""));
            event.setJoinMessage(null);
            return;
        }
        
        // Staff mode players notification
        if (plugin.getAdminLevelManager().hasAdminLevel(player)) {
            // Player has admin level
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Update playtime
        var info = plugin.getPunishmentManager().getPlayerInfo(player);
        if (info.lastJoin > 0) {
            info.addPlaytime(System.currentTimeMillis() - info.lastJoin);
        }
        
        // Disable staff mode if active
        if (plugin.getStaffModeManager().isInStaffMode(uuid)) {
            plugin.getStaffModeManager().disableStaffMode(player);
        }
        
        // Disable vanish if active
        if (plugin.getVanishManager().isVanished(uuid)) {
            plugin.getVanishManager().unvanish(player);
        }
        
        // Unfreeze if frozen
        if (plugin.getFreezeManager().isFrozen(uuid)) {
            plugin.getFreezeManager().unfreezePlayer(player);
        }
        
        // Remove from spy sets
        plugin.setSpying(uuid, false);
        
        // Clear TAB cache
        plugin.getTabCache("online_players").remove(player.getName());
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Check if muted
        if (plugin.getPunishmentManager().isMuted(player.getName())) {
            var mute = plugin.getPunishmentManager().getMute(player.getName());
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cВы замучены!");
            plugin.sendMessage(player, "§eПричина: §f" + mute.reason);
            plugin.sendMessage(player, "§eОсталось: §f" + mute.getTimeLeft());
            event.setCancelled(true);
            return;
        }
        
        // Check chat cooldown
        if (!plugin.getChatManager().canSendMessage(player)) {
            int remaining = plugin.getChatManager().getSlowChatSeconds();
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cПодождите " + remaining + " секунд перед следующим сообщением!");
            event.setCancelled(true);
            return;
        }
        
        // Check if chat is muted
        if (plugin.getChatManager().isChatMuted() && !player.hasPermission("ajail.mutechat.bypass")) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cЧат закрыт администрацией!");
            event.setCancelled(true);
            return;
        }
        
        // Process message
        plugin.getChatManager().onPlayerMessage(player, message);
        
        // Spy on messages
        for (UUID spyUUID : plugin.getPlayerData(uuid).getClass().getDeclaredFields()) {
            // Check for msgspy players
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        
        // Block commands if muted (except essential commands)
        if (plugin.getPunishmentManager().isMuted(player.getName())) {
            String[] allowedCommands = {"/msg", "/r", "/tell", "/whisper", "/mail", "/report"};
            boolean allowed = false;
            for (String allowedCmd : allowedCommands) {
                if (command.startsWith(allowedCmd)) {
                    allowed = true;
                    break;
                }
            }
            
            if (!allowed && !player.hasPermission("ajail.*")) {
                plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                    "§cВы замучены! Команды заблокированы.");
                event.setCancelled(true);
                return;
            }
        }
        
        // Check if frozen
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cВы заморожены!");
            event.setCancelled(true);
            return;
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Check freeze
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            plugin.getFreezeManager().checkMovement(player, event.getFrom(), event.getTo());
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Staff mode item handling
        if (plugin.getStaffModeManager().isInStaffMode(player.getUniqueId())) {
            if (event.getItem() != null && event.getItem().hasItemMeta() && 
                event.getItem().getItemMeta().hasLocalizedName()) {
                
                String localizedName = event.getItem().getItemMeta().getLocalizedName();
                
                if (localizedName.startsWith("staff:")) {
                    event.setCancelled(true);
                    handleStaffItemClick(player, localizedName.substring(6));
                }
            }
        }
    }
    
    private void handleStaffItemClick(Player player, String action) {
        switch (action) {
            case "rtp" -> plugin.getTeleportManager().randomTeleport(player);
            case "players" -> player.performCommand("stafflist");
            case "freeze" -> player.sendMessage("§eВыберите игрока для заморозки");
            case "vanish" -> plugin.getVanishManager().toggleVanish(player);
            case "serverinfo" -> player.performCommand("serverinfo");
            case "reports" -> player.performCommand("reports");
            case "inspect" -> player.sendMessage("§eВыберите игрока для инспекции");
            case "close" -> plugin.getStaffModeManager().disableStaffMode(player);
        }
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getStaffModeManager().isInStaffMode(player.getUniqueId()) && 
            event.getRightClicked() instanceof Player target) {
            
            if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                event.setCancelled(true);
                player.performCommand("apanel " + target.getName());
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Prevent damage to frozen players
        if (event.getEntity() instanceof Player player) {
            if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
                event.setCancelled(true);
            }
            
            // Prevent damage in staff mode
            if (plugin.getStaffModeManager().isInStaffMode(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {
            // Check if attacker is frozen
            if (plugin.getFreezeManager().isFrozen(attacker.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            
            // Check if victim is in staff mode
            if (plugin.getStaffModeManager().isInStaffMode(victim.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // Block in staff mode (unless creative)
        if (plugin.getStaffModeManager().isInStaffMode(player.getUniqueId()) && 
            player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
        
        // Block for frozen players
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        // Block in staff mode (unless creative)
        if (plugin.getStaffModeManager().isInStaffMode(player.getUniqueId()) && 
            player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
        
        // Block for frozen players
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            // Prevent inventory changes for frozen players
            if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            // Prevent inventory opening for frozen players
            if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Disable vanish on respawn
        if (plugin.getVanishManager().isVanished(player.getUniqueId())) {
            plugin.getVanishManager().unvanish(player);
        }
        
        // Disable staff mode on respawn
        if (plugin.getStaffModeManager().isInStaffMode(player.getUniqueId())) {
            plugin.getStaffModeManager().disableStaffMode(player);
        }
    }
    
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        
        // Update vanish visibility in new world
        if (plugin.getVanishManager().isVanished(player.getUniqueId())) {
            plugin.getVanishManager().onPermissionChange(player);
        }
    }
}
