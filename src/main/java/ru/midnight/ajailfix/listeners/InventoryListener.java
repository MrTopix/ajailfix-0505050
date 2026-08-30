package ru.midnight.ajailfix.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import ru.midnight.ajailfix.AJailFixPlugin;

public class InventoryListener implements Listener {
    
    private final AJailFixPlugin plugin;
    
    // Track open inventories for inspection
    private final java.util.Map<Player, InventoryData> openInventories = new java.util.HashMap<>();
    
    public InventoryListener(AJailFixPlugin plugin) {
        this.plugin = plugin;
    }
    
    public static class InventoryData {
        public Player viewer;
        public Player target;
        public String type; // "invsee", "endersee", "armorsee"
        public Inventory originalInventory;
        
        public InventoryData(Player viewer, Player target, String type, Inventory originalInventory) {
            this.viewer = viewer;
            this.target = target;
            this.type = type;
            this.originalInventory = originalInventory;
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        InventoryData data = openInventories.get(player);
        if (data == null) {
            return;
        }
        
        // Check if it's an inspection inventory
        if (event.getCurrentItem() != null) {
            // Prevent taking items from inspected inventories
            if (data.type.equals("invsee") || data.type.equals("endersee") || 
                data.type.equals("armorsee")) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        openInventories.remove(player);
    }
    
    public void trackInventory(Player viewer, Player target, String type, Inventory inventory) {
        openInventories.put(viewer, new InventoryData(viewer, target, type, inventory));
    }
}
