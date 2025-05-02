package io.github.oldmanpushcart.dashscope4j.agent.internal.util;

import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class IOUtils {

    public static String resourceToString(String resourceName) {
        return resourceToString(resourceName, IOUtils.class.getClassLoader());
    }

    public static String resourceToString(String resourceName, ClassLoader loader) {
        try (final InputStream input = loader.getResourceAsStream(resourceName);
             final ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            if (input == null) {
                throw new ResourceNotFoundException(resourceName);
            }

            final byte[] buf = new byte[8192];
            for (int read; (read = input.read(buf)) >= 0; ) {
                output.write(buf, 0, read);
            }

            return new String(output.toByteArray(), UTF_8);

        } catch (Exception ex) {
            throw new ResourceException(resourceName, ex);
        }

    }

    public static class ResourceNotFoundException extends ResourceException {

        public ResourceNotFoundException(String resourceName) {
            super(resourceName, String.format("Resource not found: %s", resourceName));
        }

    }

    public static class ResourceException extends RuntimeException {

        @Getter
        private final String resourceName;

        public ResourceException(String resourceName, Throwable cause) {
            super(cause);
            this.resourceName = resourceName;
        }

        public ResourceException(String resourceName, String message) {
            super(message);
            this.resourceName = resourceName;
        }

    }

}
