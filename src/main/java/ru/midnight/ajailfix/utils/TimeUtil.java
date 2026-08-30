package ru.midnight.ajailfix.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimeUtil {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    
    public static String formatDuration(long millis) {
        if (millis <= 0) return "Permanent";
        
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 && days == 0) {
            sb.append(seconds).append("s");
        }
        
        return sb.toString().trim();
    }
    
    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }
    
    public static long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) return -1;
        
        duration = duration.toLowerCase().trim();
        
        if (duration.equals("perm") || duration.equals("permanent") || duration.equals("-1") || duration.equals("∞")) {
            return -1;
        }
        
        long total = 0;
        StringBuilder num = new StringBuilder();
        
        for (char c : duration.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                long value = num.length() > 0 ? Long.parseLong(num.toString()) : 1;
                
                switch (c) {
                    case 's' -> total += value * 1000;
                    case 'm' -> {
                        if (duration.indexOf(c, duration.indexOf(c) + 1) != -1 || num.length() > 0 && value < 60) {
                            total += value * 60 * 1000;
                        } else {
                            total += value * 1000;
                        }
                    }
                    case 'h' -> total += value * 60 * 60 * 1000;
                    case 'd' -> total += value * 24 * 60 * 60 * 1000;
                    case 'w' -> total += value * 7 * 24 * 60 * 60 * 1000;
                }
                
                num = new StringBuilder();
            }
        }
        
        return total;
    }
    
    public static String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        
        if (diff < 0) return "in the future";
        
        return formatDuration(diff) + " ago";
    }
}
