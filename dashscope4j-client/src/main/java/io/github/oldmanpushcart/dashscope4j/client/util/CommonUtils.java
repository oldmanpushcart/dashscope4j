package io.github.oldmanpushcart.dashscope4j.client.util;

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

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return null == map || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isStringStartWith(String str, String prefix) {
        return Objects.equals(str, prefix)
                || (null != str && str.startsWith(prefix));
    }

    public static String joinStrings(String... strings) {
        if (null == strings) {
            return null;
        }
        return Arrays.stream(strings)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
    }

    public static <T> Set<T> mutableCopy(Set<T> set) {
        return null != set ? new HashSet<>(set) : new HashSet<>();
    }

    public static <T> Set<T> unmodifiableCopy(Set<T> set) {
        return null != set ? Collections.unmodifiableSet(set) : Collections.emptySet();
    }

    public static <T> List<T> mutableCopy(List<T> list) {
        return null != list ? new ArrayList<>(list) : new ArrayList<>();
    }

    public static <T> List<T> unmodifiableCopy(List<T> list) {
        return null != list ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public static <K, V> Map<K, V> mutableCopy(Map<K, V> map) {
        return null != map ? new HashMap<>(map) : new HashMap<>();
    }

    public static <K, V> Map<K, V> unmodifiableCopy(Map<K, V> map) {
        return null != map ? Collections.unmodifiableMap(map) : Collections.emptyMap();
    }


}
