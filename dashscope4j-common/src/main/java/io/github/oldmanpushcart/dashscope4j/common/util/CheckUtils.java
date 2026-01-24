package io.github.oldmanpushcart.dashscope4j.common.util;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * 检查工具类
 */
public class CheckUtils {

    /**
     * 检查字符串是否为空
     *
     * @param str             字符串
     * @param messageSupplier 错误提示消息
     * @return 字符串
     */
    public static String requireNonBlankString(String str, Supplier<String> messageSupplier) {
        requireNonNull(messageSupplier);
        return require(str, CommonUtils::isNotBlankString, messageSupplier);
    }

    /**
     * 检查字符串是否为空
     *
     * @param str     字符串
     * @param message 错误提示消息
     * @return 字符串
     */
    public static String requireNonBlankString(String str, String message) {
        return requireNonBlankString(str, () -> message);
    }

    /**
     * 检查集合是否为空
     *
     * @param collection      集合
     * @param messageSupplier 错误提示消息
     * @param <T>             集合元素类型
     * @return 集合
     */
    public static <T extends Collection<?>> T requireNonEmptyCollection(T collection, Supplier<String> messageSupplier) {
        requireNonNull(messageSupplier);
        return require(collection, CommonUtils::isNotEmptyCollection, messageSupplier);
    }

    /**
     * 检查集合是否为空
     *
     * @param collection 集合
     * @param message    错误提示消息
     * @param <T>        集合元素类型
     * @return 集合
     */
    public static <T extends Collection<?>> T requireNonEmptyCollection(T collection, String message) {
        return requireNonEmptyCollection(collection, () -> message);
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
    public static <T> T require(T t, Predicate<T> predicate, String message) {
        return require(t, predicate, () -> message);
    }

    public static <T> T require(T t, Predicate<T> predicate, Supplier<String> messageSupplier) {
        if (!predicate.test(t)) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
        return t;
    }

}
