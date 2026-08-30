package ru.midnight.ajailfix.utils;

import net.md_5.bungee.api.ChatColor;

public class ColorUtil {
    
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static String stripColor(String message) {
        return ChatColor.stripColor(color(message));
    }
    
    public static boolean isValidHexColor(String hex) {
        if (hex == null || hex.length() != 6 && hex.length() != 7) {
            return false;
        }
        
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        
        try {
            Integer.parseInt(hex, 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static String getColoredString(String input) {
        return color(input);
    }
}
