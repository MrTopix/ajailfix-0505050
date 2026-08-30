package ru.midnight.ajailfix.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.midnight.ajailfix.AJailFixPlugin;

import java.util.*;

public class StaffModeManager {
    
    private final AJailFixPlugin plugin;
    private final Map<UUID, StaffModeData> staffModeData = new HashMap<>();
    
    public static class StaffModeData {
        public UUID uuid;
        public String playerName;
        public Location preStaffLocation;
        public GameMode preGameMode;
        public boolean preAllowFlight;
        public float preFlySpeed;
        public ItemStack[] preInventory;
        public ItemStack[] preArmor;
        public long startTime;
        
        public StaffModeData(UUID uuid, String playerName) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.startTime = System.currentTimeMillis();
        }
    }
    
    public StaffModeManager(AJailFixPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void enableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (staffModeData.containsKey(uuid)) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cВы уже в режиме Staff Mode!");
            return;
        }
        
        // Save pre-staff mode data
        StaffModeData data = new StaffModeData(uuid, player.getName());
        data.preStaffLocation = player.getLocation().clone();
        data.preGameMode = player.getGameMode();
        data.preAllowFlight = player.getAllowFlight();
        data.preFlySpeed = player.getFlySpeed();
        data.preInventory = player.getInventory().getContents().clone();
        data.preArmor = player.getInventory().getArmorContents().clone();
        
        staffModeData.put(uuid, data);
        plugin.setStaffMode(uuid, true);
        
        // Apply staff mode effects
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(0.2f);
        
        // Clear and give staff items
        player.getInventory().clear();
        giveStaffItems(player);
        
        // Effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        
        // Vanish
        if (!plugin.getVanishManager().isVanished(uuid)) {
            plugin.getVanishManager().vanish(player);
        }
        
        // Notify player
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "§a✓ Staff Mode включён!");
        plugin.sendMessage(player, "§7Используйте §e/duty §7или §e/staffmode §7чтобы выключить.");
        
        // Broadcast to other staff
        String broadcastMsg = "§7[§aSTAFF§7] §e" + player.getName() + " §aвышел на работу";
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("ajail.suite.access") && !staff.equals(player)) {
                plugin.sendMessage(staff, broadcastMsg);
            }
        }
        
        plugin.getAuditManager().log("STAFFMODE_ON", player.getName(), "", player);
    }
    
    public void disableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();
        
        StaffModeData data = staffModeData.remove(uuid);
        if (data == null) {
            plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
                "§cВы не в режиме Staff Mode!");
            return;
        }
        
        plugin.setStaffMode(uuid, false);
        
        // Restore pre-staff mode data
        if (data.preStaffLocation != null && data.preStaffLocation.getWorld() != null) {
            player.teleport(data.preStaffLocation);
        }
        
        player.setGameMode(data.preGameMode);
        player.setAllowFlight(data.preAllowFlight);
        player.setFlySpeed(data.preFlySpeed);
        
        if (!data.preAllowFlight) {
            player.setFlying(false);
        }
        
        // Restore inventory
        player.getInventory().clear();
        player.getInventory().setContents(data.preInventory);
        player.getInventory().setArmorContents(data.preArmor);
        
        // Remove effects
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        
        // Unvanish
        if (plugin.getVanishManager().isVanished(uuid)) {
            plugin.getVanishManager().unvanish(player);
        }
        
        // Notify
        plugin.sendMessage(player, plugin.getConfigManager().getPrefix() + 
            "§c✗ Staff Mode выключен!");
        
        // Broadcast
        String broadcastMsg = "§7[§cSTAFF§7] §e" + player.getName() + " §cушёл с работы";
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("ajail.suite.access") && !staff.equals(player)) {
                plugin.sendMessage(staff, broadcastMsg);
            }
        }
        
        plugin.getAuditManager().log("STAFFMODE_OFF", player.getName(), "", player);
    }
    
    public void toggleStaffMode(Player player) {
        if (staffModeData.containsKey(player.getUniqueId())) {
            disableStaffMode(player);
        } else {
            enableStaffMode(player);
        }
    }
    
    public boolean isInStaffMode(UUID uuid) {
        return staffModeData.containsKey(uuid);
    }
    
    public StaffModeData getStaffModeData(UUID uuid) {
        return staffModeData.get(uuid);
    }
    
    public Set<UUID> getPlayersInStaffMode() {
        return new HashSet<>(staffModeData.keySet());
    }
    
    public int getStaffModeCount() {
        return staffModeData.size();
    }
    
    private void giveStaffItems(Player player) {
        var inv = player.getInventory();
        
        // Compass - Random Teleport
        ItemStack compass = new ItemStack(org.bukkit.Material.COMPASS);
        var compassMeta = compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.setDisplayName("§6§lСлучайный ТП");
            compassMeta.setLocalizedName("staff:rtp");
            compass.setItemMeta(compassMeta);
        }
        inv.setItem(0, compass);
        
        // Book - Player List
        ItemStack book = new ItemStack(org.bukkit.Material.BOOK);
        var bookMeta = book.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setDisplayName("§e§lСписок игроков");
            bookMeta.setLocalizedName("staff:players");
            book.setItemMeta(bookMeta);
        }
        inv.setItem(1, book);
        
        // Blaze Rod - Freeze
        ItemStack blazeRod = new ItemStack(org.bukkit.Material.BLAZE_ROD);
        var blazeMeta = blazeRod.getItemMeta();
        if (blazeMeta != null) {
            blazeMeta.setDisplayName("§b§lЗамораживание");
            blazeMeta.setLocalizedName("staff:freeze");
            blazeRod.setItemMeta(blazeMeta);
        }
        inv.setItem(2, blazeRod);
        
        // Eye of Ender - Vanish Toggle
        ItemStack eye = new ItemStack(org.bukkit.Material.ENDER_EYE);
        var eyeMeta = eye.getItemMeta();
        if (eyeMeta != null) {
            eyeMeta.setDisplayName("§5§lНевидимость");
            eyeMeta.setLocalizedName("staff:vanish");
            eye.setItemMeta(eyeMeta);
        }
        inv.setItem(3, eye);
        
        // Clock - Server Info
        ItemStack clock = new ItemStack(org.bukkit.Material.CLOCK);
        var clockMeta = clock.getItemMeta();
        if (clockMeta != null) {
            clockMeta.setDisplayName("§a§lИнформация о сервере");
            clockMeta.setLocalizedName("staff:serverinfo");
            clock.setItemMeta(clockMeta);
        }
        inv.setItem(4, clock);
        
        // Paper - Reports
        ItemStack paper = new ItemStack(org.bukkit.Material.PAPER);
        var paperMeta = paper.getItemMeta();
        if (paperMeta != null) {
            paperMeta.setDisplayName("§c§lРепорты");
            paperMeta.setLocalizedName("staff:reports");
            paper.setItemMeta(paperMeta);
        }
        inv.setItem(5, paper);
        
        // Skull - Inspect
        ItemStack skull = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
        var skullMeta = skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setDisplayName("§d§lИнспекция");
            skullMeta.setLocalizedName("staff:inspect");
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(6, skull);
        
        // Redstone - Close
        ItemStack redstone = new ItemStack(org.bukkit.Material.REDSTONE);
        var redstoneMeta = redstone.getItemMeta();
        if (redstoneMeta != null) {
            redstoneMeta.setDisplayName("§c§lВыйти");
            redstoneMeta.setLocalizedName("staff:close");
            redstone.setItemMeta(redstoneMeta);
        }
        inv.setItem(8, redstone);
    }
}
