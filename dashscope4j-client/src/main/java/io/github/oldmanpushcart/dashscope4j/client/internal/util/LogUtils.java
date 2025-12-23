package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import org.slf4j.Logger;

import java.util.function.BiConsumer;

public class LogUtils {

    public static BiConsumer<Object, Throwable> logCompleted(Logger logger, String message, Object... args) {
        return (unused, ex) -> {
            if (null != ex) {
                final var newArgs = new Object[args.length + 1];
                System.arraycopy(args, 0, newArgs, 0, args.length);
                newArgs[newArgs.length - 1] = ex;
                logger.warn(message, newArgs);
            } else {
                logger.debug(message, args);
            }
        };
    }

}
