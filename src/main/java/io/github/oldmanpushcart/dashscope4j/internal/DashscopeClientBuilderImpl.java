package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Cache;
import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.github.oldmanpushcart.dashscope4j.internal.util.HttpUtils.loggingHttpRequest;
import static io.github.oldmanpushcart.dashscope4j.internal.util.HttpUtils.loggingHttpResponse;
import static java.util.Objects.requireNonNull;

public class DashscopeClientBuilderImpl implements DashscopeClient.Builder {

    private String ak;
    private Supplier<Cache> cacheFactory = () -> new LruCacheImpl(4096);
    private final OkHttpClient.Builder okHttpClientBuilder
            = new OkHttpClient.Builder()
            .addInterceptor(new Interceptor() {
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
            });

    @Override
    public DashscopeClient.Builder ak(String ak) {
        this.ak = requireNonNull(ak);
        return this;
    }

    @Override
    public DashscopeClient.Builder cacheFactory(Supplier<Cache> factory) {
        this.cacheFactory = requireNonNull(factory);
        return this;
    }

    @Override
    public DashscopeClient.Builder customizeOkHttpClient(Consumer<OkHttpClient.Builder> consumer) {
        consumer.accept(okHttpClientBuilder);
        return this;
    }

    @Override
    public DashscopeClient build() {
        requireNonNull(ak, "require ak");
        Cache cache = null;
        OkHttpClient http = null;
        try {

            cache = cacheFactory.get();
            http = okHttpClientBuilder.build();
            return new DashscopeClientImpl(ak, cache, http);

        } catch (Throwable ex) {

            if (null != cache) {
                try {
                    cache.close();
                } catch (IOException e) {
                    // ignore
                }
            }

            if (null != http) {
                http.dispatcher()
                        .executorService()
                        .shutdown();
            }

            throw new RuntimeException("Init dashscope4j client failed", ex);

        }

    }

}
