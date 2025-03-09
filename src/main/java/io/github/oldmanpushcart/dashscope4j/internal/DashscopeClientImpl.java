package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Cache;
import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.api.embedding.EmbeddingOp;
import io.github.oldmanpushcart.dashscope4j.api.image.ImageOp;
import io.github.oldmanpushcart.dashscope4j.api.video.VideoOp;
import io.github.oldmanpushcart.dashscope4j.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.internal.api.ApiOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.api.InterceptionApiOp;
import io.github.oldmanpushcart.dashscope4j.internal.api.audio.AudioOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.api.chat.ChatOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.api.embedding.EmbeddingOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.api.image.ImageOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.api.video.VideoOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.base.BaseOpImpl;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class DashscopeClientImpl implements DashscopeClient {

    private final Cache cache;
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
            final Cache cache,
            final Collection<Interceptor> interceptors,
            final OkHttpClient http
    ) {
        this.cache = cache;
        this.http = http;
        this.apiOp = newApiOp(ak, http, interceptors);
        this.baseOp = new BaseOpImpl(http, cache, apiOp);
        this.chatOp = new ChatOpImpl(this, apiOp);
        this.audioOp = new AudioOpImpl(apiOp);
        this.embeddingOp = new EmbeddingOpImpl(apiOp);
        this.imageOp = new ImageOpImpl(apiOp);
        this.videoOp = new VideoOpImpl(apiOp);
    }

    private ApiOp newApiOp(String ak, OkHttpClient http, Collection<Interceptor> interceptors) {

        /*
         * 添加拦截器
         * 拦截器的顺序为：自定义最前，系统自带最后
         */
        final List<Interceptor> merged = new ArrayList<>(interceptors);

        merged.add(new ProcessMmEmbeddingContentForUploadInterceptor());
        merged.add(new ProcessTranscriptionForUploadInterceptor());
        merged.add(new ProcessVoiceForUploadInterceptor());
        merged.add(new ProcessImageGenVideoForUploadInterceptor());
        merged.add(new ProcessChatMessageContentForUploadInterceptor());
        merged.add(new ProcessChatMessageContentForQwenLongInterceptor());

        // 倒置merged中的顺序，因为拦截生效的顺序为倒序
        Collections.reverse(merged);

        // 生成拦截器组
        return InterceptionApiOp.group(this, new ApiOpImpl(ak, http), merged);

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

        try {
            cache.close();
        } catch (IOException e) {
            // ignore
        }
    }

}
