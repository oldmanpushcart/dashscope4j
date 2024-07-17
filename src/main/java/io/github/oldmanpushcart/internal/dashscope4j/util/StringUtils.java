package io.github.oldmanpushcart.internal.dashscope4j.util;

import static java.util.Objects.nonNull;

/**
 * 字符串工具类
 */
public class StringUtils {

    /**
     * 是否为空字符串
     *
     * @param string 字符串
     * @return TRUE | FALSE
     */
    public static boolean isBlank(String string) {
        return !isNotBlank(string);
    }

    /**
     * 是否为非空字符串
     *
     * @param string 字符串
     * @return TRUE | FALSE
     */
    public static boolean isNotBlank(String string) {
        return nonNull(string)
               && !string.isBlank();
    }

    /**
     * 是否匹配
     *
     * @param string 字符串
     * @param regex  正则表达式
     * @return TRUE | FALSE
     */
    public static boolean matches(String string, String regex) {
        return nonNull(string)
               && nonNull(regex)
               && string.matches(regex);
    }

}
