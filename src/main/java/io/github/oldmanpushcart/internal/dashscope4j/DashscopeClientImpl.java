package io.github.oldmanpushcart.internal.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.OpChat;
import io.github.oldmanpushcart.internal.dashscope4j.api.chat.OpChatImpl;
import okhttp3.OkHttpClient;

public class DashscopeClientImpl implements DashscopeClient {

    private final OkHttpClient http;
    private final OpChat opChat;

    DashscopeClientImpl(DashscopeClientBuilderImpl builder) {
        final OkHttpClient http = newOkHttpClient(builder);
        final OpExecutor opExecutor = new OpExecutor(builder.ak(), http);
        this.http = http;
        this.opChat = new OpChatImpl(opExecutor);
    }

    private static OkHttpClient newOkHttpClient(DashscopeClientBuilderImpl builder) {
        return new OkHttpClient.Builder()
                .connectTimeout(builder.connectTimeout())
                .readTimeout(builder.readTimeout())
                .writeTimeout(builder.writeTimeout())
                .build();
    }


    @Override
    public OpChat chat() {
        return opChat;
    }

    @Override
    public void shutdown() {
        http.dispatcher().executorService().shutdown();
    }

}
