package com.app.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommonUtils {
    private static final Logger logger = LogManager.getLogger(CommonUtils.class);

    public static boolean hasNonAlphaNumeric(String inputString) {
        if (inputString == null) return false;
        try {
            return !inputString.matches("^[a-zA-Z0-9]*$");
        } catch (Exception e) {
            logger.error("Error in hasNonAlphaNumeric: ", e);
            return false;
        }
    }

    public static String customReplaceAll(String value) {
        if (value == null) return "0";
        String result = value.replaceAll("\\D", "");
        return result.isEmpty() ? "0" : result;
    }

    public static String customReplace(String value) {
        if (value == null) return "0";
        String result = value.replaceAll("[^0-9.]", "");
        return result.isEmpty() ? "0" : result;
    }

    public static String customReplace(String value, String pattern) {
        if (value == null || pattern == null) return "";
        String result = value.replaceAll("[" + pattern + "]", " ");
        return result.trim();
    }

    public static boolean isAlphanumericSpace(String input) {
        return StringUtils.isAlphanumericSpace(input);
    }
}