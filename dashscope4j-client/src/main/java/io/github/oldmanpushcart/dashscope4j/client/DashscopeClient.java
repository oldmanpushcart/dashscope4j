package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.embedding.EmbeddingOp;
import io.github.oldmanpushcart.dashscope4j.client.api.image.ImageOp;
import io.github.oldmanpushcart.dashscope4j.client.api.video.VideoOp;
import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.DashscopeClientBuilderImpl;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import okhttp3.OkHttpClient;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Dashscope 客户端
 */
public interface DashscopeClient {

    /**
     * @return 对话操作
     */
    ChatOp chat();

    /**
     * @return 语音操作
     */
    AudioOp audio();

    /**
     * @return 向量操作
     */
    EmbeddingOp embedding();

    /**
     * @return 图像操作
     */
    ImageOp image();

    /**
     * @return 视频操作
     * @since 3.1.0
     */
    VideoOp video();

    /**
     * @return 基础操作
     */
    BaseOp base();

    /**
     * @return API操作
     * @since 3.1.0
     */
    ApiOp api();

    /**
     * 关闭客户端
     */
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

        Builder interceptors(Collection<Interceptor> interceptors);

        Builder addInterceptor(Interceptor interceptor);

        Builder addInterceptors(Collection<Interceptor> interceptors);

        Builder customizeOkHttpClient(Consumer<OkHttpClient.Builder> consumer);

    }

}
