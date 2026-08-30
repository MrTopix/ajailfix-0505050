package ru.midnight.ajailfix.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TextUtil {
    
    public static String join(String[] args, int start) {
        return String.join(" ", java.util.Arrays.copyOfRange(args, start, args.length));
    }
    
    public static String join(String[] args, int start, String separator) {
        return String.join(separator, java.util.Arrays.copyOfRange(args, start, args.length));
    }
    
    public static List<String> wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        
        if (text.length() <= maxLength) {
            lines.add(text);
            return lines;
        }
        
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxLength) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
    
    public static String plural(int count, String singular, String plural1, String plural2) {
        int mod10 = count % 10;
        int mod100 = count % 100;
        
        if (mod10 == 1 && mod100 != 11) {
            return singular;
        } else if ((mod10 >= 2 && mod10 <= 4) && (mod100 < 12 || mod100 > 14)) {
            return plural1;
        } else {
            return plural2;
        }
    }
    
    public static List<String> filterMatches(String input, List<String> options) {
        if (input == null || input.isEmpty()) {
            return options;
        }
        
        String lowerInput = input.toLowerCase();
        return options.stream()
            .filter(opt -> opt.toLowerCase().startsWith(lowerInput))
            .collect(Collectors.toList());
    }
}
