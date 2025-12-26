package io.github.oldmanpushcart.dashscope4j.client.internal.util;

public class IOUtils {

    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

}
