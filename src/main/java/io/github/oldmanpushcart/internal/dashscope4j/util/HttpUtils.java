package io.github.oldmanpushcart.internal.dashscope4j.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.HEADER_AUTHORIZATION;

public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    public static CompletableFuture<String> getAsString(URI remote, Executor executor, Duration connectTimeout, Duration timeout) {

        final var http = Building.of(HttpClient.newBuilder())
                .acceptIfNotNull(executor, HttpClient.Builder::executor)
                .acceptIfNotNull(connectTimeout, HttpClient.Builder::connectTimeout)
                .apply()
                .build();

        final var httpRequest = Building.of(HttpRequest.newBuilder())
                .acceptRequireNonNull(remote, HttpRequest.Builder::uri)
                .acceptIfNotNull(timeout, HttpRequest.Builder::timeout)
                .accept(HttpRequest.Builder::GET)
                .apply()
                .build();

        loggingHttpRequest(httpRequest);

        return http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .whenComplete(HttpUtils::loggingHttpResponse)
                .thenCompose(httpResponse -> {

                    /*
                     * 检查HTTP响应状态码
                     */
                    if (httpResponse.statusCode() != 200) {
                        return CompletableFuture.failedFuture(new IllegalStateException("http error: status=%s;body=%s;".formatted(
                                httpResponse.statusCode(),
                                httpResponse.body()
                        )));
                    }

                    return CompletableFuture.completedFuture(httpResponse.body());

                });
    }

    // 记录HTTP请求日志
    public static void loggingHttpRequest(HttpRequest request) {

        if (!logger.isTraceEnabled()) {
            return;
        }

        logger.trace("HTTP-REQUEST: >> {} {} {}",
                request.method(),
                request.uri(),
                request.headers().map().entrySet().stream()
                        .filter(entry -> !Objects.equals(entry.getKey(), HEADER_AUTHORIZATION))
                        .map(entry -> "%s: %s".formatted(entry.getKey(), String.join(", ", entry.getValue())))
                        .reduce("%s, %s"::formatted)
                        .orElse("")
        );

    }

    // 记录HTTP响应日志
    public static void loggingHttpResponse(HttpResponse<?> response, Throwable ex) {

        if (!logger.isTraceEnabled()) {
            return;
        }

        // HTTP错误
        if (null != ex) {
            logger.trace("HTTP-RESPONSE: << {}", ex.getLocalizedMessage());
        }

        // HTTP应答
        else {
            logger.trace("HTTP-RESPONSE: << {} {} {}",
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
