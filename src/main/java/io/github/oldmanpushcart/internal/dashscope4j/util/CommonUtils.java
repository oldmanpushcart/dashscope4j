package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.Objects;

/**
 * 通用工具类
 */
public class CommonUtils {

    /**
     * 是否为空字符串
     *
     * @param string 字符串
     * @return TRUE | FALSE
     */
    public static boolean isBlankString(String string) {
        return !isNotBlankString(string);
    }

    /**
     * 是否为非空字符串
     *
     * @param string 字符串
     * @return TRUE | FALSE
     */
    public static boolean isNotBlankString(String string) {
        return Objects.nonNull(string)
                && !string.isBlank();
    }

    /**
     * 要求非空字符串
     *
     * @param string 字符串
     * @return 字符串
     */
    public static String requireNonBlankString(String string) {
        if (isNotBlankString(string)) {
            return string;
        }
        throw new IllegalArgumentException("string is blank");
    }

}
