package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.Interceptor;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@Slf4j
public class DashscopeClientBuilderImpl implements DashscopeClient.Builder {

    private String ak;
    private final List<Interceptor> interceptors = new ArrayList<>();
    private final OkHttpClient.Builder okHttpClientBuilder
            = new OkHttpClient.Builder()
            .addInterceptor(new LogHttpInterceptor());

    @Override
    public DashscopeClient.Builder ak(String ak) {
        this.ak = requireNonNull(ak);
        return this;
    }

    @Override
    public DashscopeClient.Builder interceptors(Collection<Interceptor> interceptors) {
        requireNonNull(interceptors);
        this.interceptors.clear();
        this.interceptors.addAll(interceptors);
        return this;
    }

    @Override
    public DashscopeClient.Builder addInterceptor(Interceptor interceptor) {
        requireNonNull(interceptor);
        this.interceptors.add(interceptor);
        return this;
    }

    @Override
    public DashscopeClient.Builder addInterceptors(Collection<Interceptor> interceptors) {
        requireNonNull(interceptors);
        this.interceptors.addAll(interceptors);
        return this;
    }

    @Override
    public DashscopeClient.Builder customizeOkHttpClient(Consumer<OkHttpClient.Builder> consumer) {
        consumer.accept(okHttpClientBuilder);
        return this;
    }

    @Override
    public DashscopeClient build() {
        OkHttpClient http = null;
        try {
            http = okHttpClientBuilder.build();
            return new DashscopeClientImpl(ak, interceptors, http);

        } catch (Throwable ex) {

            if (null != http) {
                http.dispatcher()
                        .executorService()
                        .shutdown();
            }

            throw new RuntimeException("Init dashscope4j client failed", ex);

        }

    }

    private static class LogHttpInterceptor implements okhttp3.Interceptor {
        @NotNull
        @Override
        public Response intercept(@NotNull Chain chain) throws IOException {
            try {
                final Request request = chain.request();
                loggingHttpRequest(request);
                final Response response = chain.proceed(request);
                loggingHttpResponse(response, null);
                return response;
            } catch (Exception ex) {
                loggingHttpResponse(null, ex);
                if (ex instanceof IOException) {
                    throw (IOException) ex;
                } else {
                    throw new IOException(ex);
                }
            }
        }

        private Map<String, String> parseHeaderMap(Headers headers) {
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

        private void loggingHttpRequest(Request request) {

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

        private void loggingHttpResponse(Response response, Throwable ex) {

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
}
