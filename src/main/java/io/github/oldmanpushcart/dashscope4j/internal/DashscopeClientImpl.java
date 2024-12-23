package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Cache;
import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.internal.api.ApiOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.api.InterceptionApiOp;
import io.github.oldmanpushcart.dashscope4j.internal.api.audio.AudioOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.api.chat.ChatOpImpl;
import io.github.oldmanpushcart.dashscope4j.internal.base.BaseOpImpl;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class DashscopeClientImpl implements DashscopeClient {

    private final Cache cache;
    private final OkHttpClient http;
    private final BaseOp baseOp;
    private final AudioOp audioOp;
    private final ChatOp chatOp;

    DashscopeClientImpl(final String ak,
                        final Cache cache,
                        final Collection<Interceptor> interceptors,
                        final OkHttpClient http
    ) {
        this.cache = cache;
        this.http = http;
        final ApiOp apiOp = newApiOp(ak, http, interceptors);
        this.baseOp = new BaseOpImpl(cache, apiOp);
        this.chatOp = new ChatOpImpl(apiOp);
        this.audioOp = new AudioOpImpl(apiOp);
    }

    private ApiOp newApiOp(String ak, OkHttpClient http, Collection<Interceptor> interceptors) {
        final ApiOp apiOp = new ApiOpImpl(ak, http);
        final Collection<Interceptor> merged = new ArrayList<>();
        merged.add(new ProcessChatMessageContentForUploadInterceptor());
        merged.addAll(interceptors);
        return InterceptionApiOp.group(this, apiOp, merged);
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
    public BaseOp base() {
        return baseOp;
    }

    @Override
    public void shutdown() {
        http.dispatcher().executorService().shutdown();
        try {
            cache.close();
        } catch (IOException e) {
            // ignore
        }
    }

}
