package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class StringUtils {

    /**
     * 驼峰转下划线
     *
     * @param camelCase 驼峰字符串
     * @return 下划线字符串
     */
    public static String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    /**
     * 判断字符串是否非空
     *
     * @param str 字符串
     * @return 是否非空
     */
    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 判断字符串是否为空
     *
     * @param str 待判断的字符串
     * @return 是否为空
     */
    public static boolean isBlank(String str) {
        return !isNotBlank(str);
    }

    /**
     * 连接字符串
     *
     * @param strings 字符串
     * @return 连接结果
     */
    public static String concat(String... strings) {
        if (null == strings) {
            return null;
        }
        return Arrays.stream(strings)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
    }

    public static String uuid() {
        return  UUID.randomUUID().toString();
    }

}
