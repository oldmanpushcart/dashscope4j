package io.github.oldmanpushcart.internal.dashscope4j.base.api.http;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.check;
import static java.util.Collections.unmodifiableSet;

/**
 * SSE
 *
 * @param id    ID
 * @param type  类型
 * @param data  数据
 * @param metas 元数据集
 */
public record HttpSsEvent(String id, String type, String data, Set<String> metas) {

    @Override
    public String toString() {
        return "%s|%s|%s|%s".formatted(id, type, data, String.join(",", metas));
    }

    /**
     * 解析SSE
     *
     * @param body HTTP BODY
     * @return SSE事件
     */
    public static HttpSsEvent parse(String body) {
        final var metas = new LinkedHashSet<String>();
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
                    metas.add(line.substring(1).trim());
                } else {
                    throw new ParseException(body, "unsupported line: %s".formatted(line));
                }
            }
        }

        return new HttpSsEvent(
                check(id, Objects::nonNull, () -> new ParseException(body, "id: is missed!")),
                check(type, Objects::nonNull, () -> new ParseException(body, "event: is missed!")),
                check(data, Objects::nonNull, () -> new ParseException(body, "data: is missed!")),
                unmodifiableSet(metas)
        );
    }

    /**
     * 解析异常
     */
    private static class ParseException extends RuntimeException {

        private final String body;

        private ParseException(String body, String message) {
            super(message);
            this.body = body;
        }

        @Override
        public String getLocalizedMessage() {
            return "parse Http-SsEvent error: %s\n%s".formatted(super.getLocalizedMessage(), body);
        }

    }

}
