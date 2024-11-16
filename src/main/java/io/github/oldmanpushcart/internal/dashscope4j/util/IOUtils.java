package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.net.URI;
import java.util.Objects;

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

    public static boolean isLocalFile(URI resource) {
        return Objects.nonNull(resource)
               && "file".equals(resource.getScheme());
    }

}
