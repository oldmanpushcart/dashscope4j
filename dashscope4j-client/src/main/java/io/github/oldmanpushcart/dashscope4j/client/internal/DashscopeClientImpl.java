package io.github.oldmanpushcart.dashscope4j.client.internal;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.embedding.EmbeddingOp;
import io.github.oldmanpushcart.dashscope4j.client.api.image.ImageOp;
import io.github.oldmanpushcart.dashscope4j.client.api.video.VideoOp;
import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.ApiOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.InterceptionApiOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.RequestInterceptionApiOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.audio.AudioOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.ChatOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.embedding.EmbeddingOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.image.ImageOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.video.VideoOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.BaseOpImpl;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class DashscopeClientImpl implements DashscopeClient {

    private final OkHttpClient http;
    private final ApiOp apiOp;
    private final BaseOp baseOp;
    private final AudioOp audioOp;
    private final ChatOp chatOp;
    private final EmbeddingOp embeddingOp;
    private final ImageOp imageOp;
    private final VideoOp videoOp;

    DashscopeClientImpl(
            final String ak,
            final Collection<Interceptor> interceptors,
            final OkHttpClient http
    ) {
        this.http = http;
        this.apiOp = newApiOp(ak, http, interceptors);
        this.baseOp = new BaseOpImpl(http, apiOp);
        this.chatOp = new ChatOpImpl(apiOp);
        this.audioOp = new AudioOpImpl(apiOp);
        this.embeddingOp = new EmbeddingOpImpl(apiOp);
        this.imageOp = new ImageOpImpl(apiOp);
        this.videoOp = new VideoOpImpl(apiOp);
    }

    private ApiOp newApiOp(String ak, OkHttpClient http, Collection<Interceptor> interceptors) {

        /*
         * 添加拦截器
         *
         * 拦截器的顺序为：系统自带最先被执行，自定义最后执行
         * 之所以要这样设计，是为了让自定义拦截器在拦截请求时，感受到的请求行为与API承诺的一致
         * 比如ToolCall的行为是内部拼接的多段请求，自定义拦截器不能感知到内部分裂多段的查询，必须感知为一个查询整体
         *
         */
        final List<Interceptor> merged = new ArrayList<>(interceptors);

        // 生成拦截器组
        final ApiOp realApiOp = new ApiOpImpl(ak, http);
        final ApiOp interceptionApiOp = InterceptionApiOp.group(this, realApiOp, merged);
        return new RequestInterceptionApiOp(this, interceptionApiOp);

    }

    @Override
    public ChatOp chat() {
        return chatOp;
    }

    @Override
    public AudioOp audio() {
        return audioOp;
    }

    @Override
    public EmbeddingOp embedding() {
        return embeddingOp;
    }

    @Override
    public ImageOp image() {
        return imageOp;
    }

    @Override
    public VideoOp video() {
        return videoOp;
    }

    @Override
    public BaseOp base() {
        return baseOp;
    }

    @Override
    public ApiOp api() {
        return apiOp;
    }

    private void closeHttp() {
        http.dispatcher().executorService().shutdown();
        http.connectionPool().evictAll();
        try {
            final okhttp3.Cache cache = http.cache();
            if (null != cache) {
                cache.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public void shutdown() {
        closeHttp();
    }

}
