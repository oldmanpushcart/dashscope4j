package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import java.util.Arrays;
import java.util.Set;

public class TagUtils {

    public static boolean contains(Set<String> tags, String key, String value) {
        return tags.stream()
                .filter(tag -> tag.startsWith(key + ":"))
                .flatMap(tag -> Arrays.stream(tag.substring(key.length() + 1).split(",")))
                .anyMatch(v -> v.equals(value));
    }

}
