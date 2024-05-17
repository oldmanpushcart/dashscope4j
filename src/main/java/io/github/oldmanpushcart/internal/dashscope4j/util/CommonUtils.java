package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

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
     * @param string  字符串
     * @param message 异常信息
     * @return 字符串
     */
    public static String requireNonBlankString(String string, String message) {
        if (isNotBlankString(string)) {
            return string;
        }
        throw new IllegalArgumentException(message);
    }

    /**
     * 检查
     *
     * @param t         对象
     * @param predicate 断言
     * @param message   异常信息
     * @param <T>       对象类型
     * @return 对象
     */
    public static <T> T check(T t, Predicate<T> predicate, String message) {
        if (!predicate.test(t)) {
            throw new IllegalArgumentException(message);
        }
        return t;
    }

    /**
     * 是否为空集合
     *
     * @param collection 集合
     * @return TRUE | FALSE
     */
    public static boolean isEmptyCollection(Collection<?> collection) {
        return Objects.isNull(collection) || collection.isEmpty();
    }

    /**
     * 是否为非空集合
     *
     * @param collection 集合
     * @return TRUE | FALSE
     */
    public static boolean isNotEmptyCollection(Collection<?> collection) {
        return !isEmptyCollection(collection);
    }

    /**
     * 要求非空集合
     *
     * @param collection 集合
     * @param message    异常信息
     * @param <T>        集合类型
     * @return 集合
     */
    public static <T extends Collection<?>> T requireNotEmpty(T collection, String message) {
        check(collection, CommonUtils::isNotEmptyCollection, message);
        return collection;
    }

    /**
     * 更新列表
     *
     * @param isAppend 是否追加
     * @param target   目标列表
     * @param source   源列表
     * @param <T>      对象类型
     */
    public static <T> void updateList(boolean isAppend, List<T> target, Collection<T> source) {
        if (!isAppend) {
            target.clear();
        }
        if (!isEmptyCollection(source)) {
            target.addAll(source);
        }
    }

}
