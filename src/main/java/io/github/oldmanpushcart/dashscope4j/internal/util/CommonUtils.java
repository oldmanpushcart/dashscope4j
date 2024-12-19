package io.github.oldmanpushcart.dashscope4j.internal.util;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CommonUtils {

    public static String requireNonBlankString(String str, Supplier<String> messageSupplier) {
        Objects.requireNonNull(messageSupplier);
        if (StringUtils.isNotBlank(str)) {
            return str;
        }
        throw new IllegalArgumentException(messageSupplier.get());
    }

    public static String requireNonBlankString(String str, String message) {
        return requireNonBlankString(str, () -> message);
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
