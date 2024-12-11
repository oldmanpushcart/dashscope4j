package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.audio.OpAudio;
import io.github.oldmanpushcart.dashscope4j.api.chat.OpChat;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.internal.dashscope4j.DashscopeClientBuilderImpl;
import okhttp3.OkHttpClient;

import java.util.function.Consumer;

public interface DashscopeClient {

    OpChat chat();

    OpAudio audio();

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

        Builder customizeOkHttpClient(Consumer<OkHttpClient.Builder> consumer);

    }

}
