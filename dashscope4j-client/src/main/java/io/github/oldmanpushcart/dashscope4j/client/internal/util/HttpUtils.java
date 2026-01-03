package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.HTTP_HEADER_AUTHORIZATION;

public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public static HttpRequest traceLogHttpRequest(HttpRequest request) {

        if (!logger.isTraceEnabled()) {
            return request;
        }

        logger.trace("HTTP: >>> {} {} {}",
                request.method(),
                request.uri(),
                request.headers().map().entrySet().stream()
                        .filter(entry -> !Objects.equals(entry.getKey(), HTTP_HEADER_AUTHORIZATION))
                        .map(entry -> "%s: %s".formatted(entry.getKey(), String.join(", ", entry.getValue())))
                        .reduce("%s, %s"::formatted)
                        .orElse("")
        );

        return request;
    }

    public static void traceLogHttpResponse(HttpResponse<?> response, Throwable ex) {

        if (!logger.isTraceEnabled()) {
            return;
        }

        // 错误应答
        if (null != ex) {
            logger.trace("HTTP: <<< {}", ex.getLocalizedMessage());
        }

        // 成功应答
        else {
            logger.trace("HTTP: <<< {} {} {}",
                    response.statusCode(),
                    response.uri(),
                    response.headers().map().entrySet().stream()
                            .map(entry -> "%s: %s".formatted(entry.getKey(), String.join(", ", entry.getValue())))
                            .reduce("%s, %s"::formatted)
                            .orElse("")
            );
        }

    }

}
