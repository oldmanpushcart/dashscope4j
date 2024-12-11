package io.github.oldmanpushcart.internal.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.audio.AudioOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOp;
import io.github.oldmanpushcart.internal.dashscope4j.api.audio.AudioOpImpl;
import io.github.oldmanpushcart.internal.dashscope4j.api.chat.ChatOpImpl;
import okhttp3.OkHttpClient;

public class DashscopeClientImpl implements DashscopeClient {

    private final OkHttpClient http;
    private final ExecutorOp executorOp;

    DashscopeClientImpl(DashscopeClientBuilderImpl builder, OkHttpClient http) {
        this.http = http;
        this.executorOp = new ExecutorOp(builder.ak(), http);
    }

    @Override
    public ChatOp chat() {
        return new ChatOpImpl(executorOp);
    }

    @Override
    public AudioOp audio() {
        return new AudioOpImpl(executorOp);
    }

    @Override
    public void shutdown() {
        http.dispatcher().executorService().shutdown();
    }

}
