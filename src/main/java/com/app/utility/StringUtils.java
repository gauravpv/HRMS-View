package com.app.utility;

public class StringUtils {
    
    public static String convertToCamelCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (currentChar == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(currentChar));
                    capitalizeNext = false;
                } else {
                    result.append(currentChar);
                }
            }
        }
        return result.toString();
    }
    
    public static boolean isAlphanumericSpace(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if ((Character.isLetterOrDigit(str.charAt(i)) == false) && (str.charAt(i) != ' ')) {
                return false;
            }
        }
        return true;
    }
}
