package io.github.ompc.internal.dashscope4j.base.api.http;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * SSE
 *
 * @param id   ID
 * @param type 类型
 * @param data 数据
 * @param meta 元数据
 */
public record HttpSsEvent(String id, String type, String data, Set<String> meta) {

    @Override
    public String toString() {
        return "SSE|%s|%s|%s|%s".formatted(id, type, data, String.join(",", meta));
    }

    /**
     * 解析SSE
     *
     * @param body HTTP BODY
     * @return SSE事件
     */
    public static HttpSsEvent parse(String body) {
        final var meta = new LinkedHashSet<String>();
        String id = null, type = null, data = null;
        try (final var scanner = new Scanner(body)) {
            while (scanner.hasNextLine()) {
                final var line = scanner.nextLine();
                if (line.startsWith("id:")) {
                    id = line.substring(3).trim();
                } else if (line.startsWith("event:")) {
                    type = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    data = line.substring(5).trim();
                } else if (line.startsWith(":")) {
                    meta.add(line.substring(1).trim());
                }
            }
        }
        return new HttpSsEvent(id, type, data, Collections.unmodifiableSet(meta));
    }

}
