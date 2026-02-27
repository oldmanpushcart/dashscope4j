package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import java.net.URI;

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

    public static boolean isFileURI(URI resourceURI) {
        return "file".equalsIgnoreCase(resourceURI.getScheme());
    }

}
