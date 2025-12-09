package io.github.oldmanpushcart.dashscope4j.common.util;

import java.util.Collection;
import java.util.Map;

public class CommonUtils {

    public static boolean isBlankString(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotBlankString(String str) {
        return !isBlankString(str);
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmptyCollection(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(Map<?,?> map) {
        return null == map || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?,?> map) {
        return !isEmpty(map);
    }

}
