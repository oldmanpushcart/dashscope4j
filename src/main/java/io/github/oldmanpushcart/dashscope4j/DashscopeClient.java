package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.chat.OpChat;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.internal.dashscope4j.DashscopeClientBuilderImpl;

import java.time.Duration;

public interface DashscopeClient {

    OpChat chat();

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

        Builder connectTimeout(Duration duration);

        Builder readTimeout(Duration duration);

        Builder writeTimeout(Duration duration);

    }

}