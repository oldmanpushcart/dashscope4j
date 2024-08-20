package io.github.oldmanpushcart.internal.dashscope4j.util;

public class IOUtils {

    public static void closeQuietly(AutoCloseable closeable) {
        if (null == closeable) {
            return;
        }
        try {
            closeable.close();
        } catch (Throwable t) {
            // ignore
        }
    }

}
