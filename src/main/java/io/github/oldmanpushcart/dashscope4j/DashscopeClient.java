package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.internal.DashscopeClientBuilderImpl;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import okhttp3.OkHttpClient;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface DashscopeClient {

    ChatOp chat();

    AudioOp audio();

    BaseOp base();

    void shutdown();

    static Builder newBuilder() {
        return new DashscopeClientBuilderImpl();
    }

    interface Builder extends Buildable<DashscopeClient, Builder> {

        /**
         * 设置AK
         *
         * @param ak AK
         * @return this
         */
        Builder ak(String ak);

        /**
         * 设置缓存工厂
         *
         * @param factory 缓存工厂
         * @return this
         */
        Builder cacheFactory(Supplier<Cache> factory);

        Builder interceptors(Collection<Interceptor> interceptors);

        Builder addInterceptor(Interceptor interceptor);

        Builder addInterceptors(Collection<Interceptor> interceptors);

        Builder customizeOkHttpClient(Consumer<OkHttpClient.Builder> consumer);

    }

}
