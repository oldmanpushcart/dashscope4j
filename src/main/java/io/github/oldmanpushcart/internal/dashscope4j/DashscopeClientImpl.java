package io.github.oldmanpushcart.internal.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.OpExchange;
import io.github.oldmanpushcart.dashscope4j.api.audio.OpAudio;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.RecognitionResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.OpChat;
import io.github.oldmanpushcart.internal.dashscope4j.api.chat.ChatOp;
import okhttp3.OkHttpClient;

public class DashscopeClientImpl implements DashscopeClient {

    private final OkHttpClient http;
    private final ExecutorOp executorOp;

    DashscopeClientImpl(DashscopeClientBuilderImpl builder, OkHttpClient http) {
        this.http = http;
        this.executorOp = new ExecutorOp(builder.ak(), http);
    }
    
    @Override
    public OpChat chat() {
        return new ChatOp(executorOp);
    }

    @Override
    public OpAudio audio() {
        return new OpAudio() {

            @Override
            public OpExchange<RecognitionRequest, RecognitionResponse> recognition() {
                return executorOp::executeExchange;
            }

        };
    }

    @Override
    public void shutdown() {
        http.dispatcher().executorService().shutdown();
    }

}
