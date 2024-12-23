package io.github.oldmanpushcart.dashscope4j.internal.util;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class HttpUtils {

    private static Map<String, String> parseHeaderMap(Headers headers) {
        final Map<String, String> headerMap = new LinkedHashMap<>();
        headers.forEach(header -> {
            final String name = header.getFirst();
            final String value = header.getSecond();
            if ("Authorization".equalsIgnoreCase(name)) {
                headerMap.put("Authorization", "Bearer ******");
                return;
            }
            headerMap.put(name, value);
        });
        return headerMap;
    }

    public static void loggingHttpRequest(Request request) {

        if (!log.isTraceEnabled()) {
            return;
        }

        log.trace("HTTP:// >>> {} {} {}",
                request.method(),
                request.url(),
                parseHeaderMap(request.headers()).entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("")
        );

    }

    public static void loggingHttpResponse(Response response, Throwable ex) {

        if (!log.isTraceEnabled()) {
            return;
        }

        // HTTP错误
        if (null != ex) {
            log.trace("HTTP:// << {}", ex.getLocalizedMessage());
        }

        // HTTP应答
        else {
            log.trace("HTTP:// <<< {} {} {}",
                    response.code(),
                    response.message(),
                    parseHeaderMap(response.headers()).entrySet().stream()
                            .map(entry -> entry.getKey() + ": " + entry.getValue())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("")
            );
        }

    }

}
