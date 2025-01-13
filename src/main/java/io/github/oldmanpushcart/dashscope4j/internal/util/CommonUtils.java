package io.github.oldmanpushcart.dashscope4j.internal.util;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class CommonUtils {

    public static String requireNonBlankString(String str, Supplier<String> messageSupplier) {
        requireNonNull(messageSupplier);
        if (StringUtils.isNotBlank(str)) {
            return str;
        }
        throw new IllegalArgumentException(messageSupplier.get());
    }

    public static String requireNonBlankString(String str, String message) {
        return requireNonBlankString(str, () -> message);
    }

    public static <T extends Collection<?>> T requireNonEmptyCollection(T collection, Supplier<String> messageSupplier) {
        requireNonNull(messageSupplier);
        if (null == collection || collection.isEmpty()) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
        return collection;
    }

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
    public static <T> T check(T t, Predicate<T> predicate, String message) {
        if (!predicate.test(t)) {
            throw new IllegalArgumentException(message);
        }
        return t;
    }

}
