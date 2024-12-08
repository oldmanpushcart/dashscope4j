package io.github.oldmanpushcart.internal.dashscope4j.util;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    private static Map<String, String> parseHeaderMap(Headers headers) {
        final Map<String, String> headerMap = new LinkedHashMap<>();
        headers.forEach(header -> {
            final String name = header.getFirst();
            final String value = header.getSecond();
            if ("Authorization".equalsIgnoreCase(name)) {
                return;
            }
            headerMap.put(name, value);
        });
        return headerMap;
    }

    public static void loggingHttpRequest(Request request) {

        if (!logger.isTraceEnabled()) {
            return;
        }

        logger.trace("HTTP:// >>> {} {} {}",
                request.method(),
                request.url(),
                parseHeaderMap(request.headers()).entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("")
        );

    }

    public static void loggingHttpResponse(Response response, Throwable ex) {

        if (!logger.isTraceEnabled()) {
            return;
        }

        // HTTP错误
        if (null != ex) {
            logger.trace("HTTP:// << {}", ex.getLocalizedMessage());
        }

        // HTTP应答
        else {
            logger.trace("HTTP:// <<< {} {} {}",
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
