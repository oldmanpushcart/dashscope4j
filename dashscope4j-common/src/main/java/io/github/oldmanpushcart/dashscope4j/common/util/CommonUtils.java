package io.github.oldmanpushcart.dashscope4j.common.util;

import java.util.Collection;

public class CommonUtils {

    public static boolean isBlankString(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotBlankString(String str) {
        return !isBlankString(str);
    }

    public static boolean isEmptyCollection(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmptyCollection(Collection<?> collection) {
        return !isEmptyCollection(collection);
    }

}
