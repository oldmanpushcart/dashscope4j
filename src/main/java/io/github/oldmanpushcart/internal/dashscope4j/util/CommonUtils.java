package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 通用工具类
 */
public class CommonUtils {

    public static <T> boolean isIn(T target, Set<T> values) {
        return values.stream()
                .anyMatch(value -> Objects.equals(target, value));
    }

    /**
     * 要求非空字符串
     *
     * @param string  字符串
     * @param message 异常信息
     * @return 字符串
     */
    public static String requireNonBlankString(String string, String message) {
        if (StringUtils.isNotBlank(string)) {
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
     * 检查
     *
     * @param t                 对象
     * @param predicate         断言
     * @param exceptionSupplier 异常提供器
     * @param <T>               对象类型
     * @param <X>               异常类型
     * @return 对象
     * @throws X 异常
     */
    public static <T, X extends Throwable> T check(T t, Predicate<T> predicate, Supplier<X> exceptionSupplier) throws X {
        if (!predicate.test(t)) {
            throw exceptionSupplier.get();
        }
        return t;
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
        check(collection, CollectionUtils::isNotEmptyCollection, message);
        return collection;
    }


    /**
     * 强制转换
     *
     * @param object 对象
     * @param <T>    类型
     * @return 对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object object) {
        return (T) object;
    }

}
