package io.github.oldmanpushcart.dashscope4j.client.base.files;

import java.time.Instant;
import java.util.Objects;

public class FilesOpHelper {

    public static String encodeFilename(String filename) {
        return "test_%s_%s".formatted(System.currentTimeMillis(), filename);
    }

    public static Instant parseInstantFromEncodedFilename(String encodedFilename) {
        Objects.requireNonNull(encodedFilename);
        if(!isEncodedFilename(encodedFilename)) {
            throw new IllegalArgumentException("encoded filename is illegal format: %s".formatted(encodedFilename));
        }
        return Instant.ofEpochMilli(Long.parseLong(encodedFilename.split("_")[1]));
    }

    public static boolean isEncodedFilename(String filename) {
        return filename.matches("test_\\d+_.+");
    }

}
