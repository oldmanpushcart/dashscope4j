package io.github.oldmanpushcart.dashscope4j.util;

import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * CompletableFuture工具类
 */
public class CompletableFutureUtils {

    /**
     * 解包异常
     *
     * @param ex 异常
     * @return 解包后的异常
     */
    public static Throwable unwrapEx(Throwable ex) {
        if (Objects.isNull(ex.getCause())) {
            return ex;
        }
        if (ex instanceof CompletionException || ex instanceof ExecutionException) {
            return unwrapEx(ex.getCause());
        } else {
            return ex;
        }
    }

}
