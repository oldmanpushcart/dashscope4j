package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;

import java.time.Duration;

public interface DashscopeClient {

    OpChat chat();

    void shutdown();

    interface OpChat extends OpAsync<ChatResponse>, OpFlow<ChatResponse> {

    }

    static Builder newBuilder() {
        return new DashscopeClientImpl.BuilderImpl();
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
