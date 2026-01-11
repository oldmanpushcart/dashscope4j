package io.github.oldmanpushcart.dashscope4j.common.util;

import java.util.*;
import java.util.stream.Collectors;

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

    public static String joinStrings(String... strings) {
        if (null == strings) {
            return null;
        }
        return Arrays.stream(strings)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
    }

    /**
     * 返回一个新列表，其元素顺序为原列表的倒序。
     * 原列表不受影响。
     *
     * @param list 要反转的列表（可为 null）
     * @param <T>  元素类型
     * @return 返回不可变列表
     */
    public static <T> List<T> reverseListImmutable(List<T> list) {
        if (list == null) {
            return null;
        }
        final var reversed = new ArrayList<>(list);
        Collections.reverse(reversed);
        return Collections.unmodifiableList(reversed);
    }

    public static boolean hasKeyValue(Map<?, ?> map, Object key, Object value) {
        return null != map
                && map.containsKey(key)
                && Objects.equals(map.get(key), value);
    }

}
