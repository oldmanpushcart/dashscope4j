package io.github.oldmanpushcart.dashscope4j.client.internal.util;

public class StringUtils {

    // 消除引号
    public static String removeQuotes(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("^\"|\"$", "");
    }

}
