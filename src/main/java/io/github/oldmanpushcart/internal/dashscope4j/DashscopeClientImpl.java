package io.github.oldmanpushcart.internal.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.base.BaseOp;
import io.github.oldmanpushcart.internal.dashscope4j.api.ApiOpImpl;
import io.github.oldmanpushcart.internal.dashscope4j.api.audio.AudioOpImpl;
import io.github.oldmanpushcart.internal.dashscope4j.api.chat.ChatOpImpl;
import io.github.oldmanpushcart.internal.dashscope4j.base.BaseOpImpl;
import okhttp3.OkHttpClient;

public class DashscopeClientImpl implements DashscopeClient {

    private final OkHttpClient http;
    private final ApiOp apiOp;

    DashscopeClientImpl(DashscopeClientBuilderImpl builder, OkHttpClient http) {
        this.http = http;
        this.apiOp = new ApiOpImpl(builder.ak(), http);
    }

    @Override
    public ChatOp chat() {
        return new ChatOpImpl(apiOp);
    }

    @Override
    public AudioOp audio() {
        return new AudioOpImpl(apiOp);
    }

    @Override
    public BaseOp base() {
        return new BaseOpImpl(apiOp);
    }

    @Override
    public void shutdown() {
        http.dispatcher().executorService().shutdown();
    }

}
