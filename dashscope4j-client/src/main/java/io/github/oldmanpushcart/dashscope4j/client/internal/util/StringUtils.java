package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import java.util.Arrays;
import java.util.Objects;
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

}
