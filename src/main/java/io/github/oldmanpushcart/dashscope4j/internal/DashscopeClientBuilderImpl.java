package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Cache;
import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DashscopeClientBuilderImpl implements DashscopeClient.Builder {

    private String ak;
    private Supplier<Cache> cacheFactory = () -> new LruCacheImpl(1024);
    private final OkHttpClient.Builder okHttpClientBuilder
            = new OkHttpClient.Builder();

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
