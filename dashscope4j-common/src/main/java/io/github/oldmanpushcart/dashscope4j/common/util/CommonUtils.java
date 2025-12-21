package io.github.oldmanpushcart.dashscope4j.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class CommonUtils {

    public static <T> boolean isIn(T t, T... ts) {
        if (null == ts) {
            return false;
        }
        for (final var _t : ts) {
            if (Objects.equals(t, _t)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlankString(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isEmptyString(String str) {
        return str == null || str.isEmpty();
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

    public static boolean isEmpty(Map<?, ?> map) {
        return null == map || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

}
