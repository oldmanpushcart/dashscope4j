package io.github.oldmanpushcart.dashscope4j.internal.util;

public class StringUtils {

    public static String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        }
        return str.substring(start, Math.min(end, str.length()));
    }

    public static String substring(String str, int end) {
        return substring(str, 0, end);
    }

    // 消除引号
    public static String removeQuotes(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("^\"|\"$", "");
    }

}
