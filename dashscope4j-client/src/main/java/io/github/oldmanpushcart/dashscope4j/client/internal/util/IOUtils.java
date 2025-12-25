package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

public class IOUtils {

    public static byte[] toByteArray(URI endpoint) throws IOException {
        final var imageURL = endpoint.toURL();
        final var imageConnection = imageURL.openConnection();
        try (final var output = new ByteArrayOutputStream();
             final var input = imageConnection.getInputStream()) {
            byte[] data = new byte[8192]; // 8KB 缓冲区
            int bytesRead;
            while ((bytesRead = input.read(data, 0, data.length)) != -1) {
                output.write(data, 0, bytesRead);
            }
            return output.toByteArray();
        }
    }

}
