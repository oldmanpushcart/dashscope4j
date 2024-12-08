package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.Objects;
import java.util.function.Supplier;

public class CommonUtils {

    public static String requireNonBlankString(String str, Supplier<String> messageSupplier) {
        Objects.requireNonNull(messageSupplier);
        if (!StringUtils.isBlank(str)) {
            return str;
        }
        throw new IllegalArgumentException(messageSupplier.get());
    }

    public static String requireNonBlankString(String str, String message) {
        return requireNonBlankString(str, () -> message);
    }

}
