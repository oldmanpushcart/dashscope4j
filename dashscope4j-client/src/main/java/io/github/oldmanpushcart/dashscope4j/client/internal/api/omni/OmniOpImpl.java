package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni;

import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniRealtimeConversation;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.event.OmniRealtimeEvent;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.HttpWsExchangeExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.JacksonJsonUtils;

import java.net.http.HttpClient;

public class OmniOpImpl implements OmniOp {

    private final HttpWsExchangeExecutor exchangeExecutor;

    public OmniOpImpl(HttpWsExchangeExecutor exchangeExecutor) {
        this.exchangeExecutor = exchangeExecutor;
    }


    @Override
    public OmniRealtimeConversation newRealtimeConversation(OmniRealtimeModel model) {
        final var exchange = exchangeExecutor.<OmniRealtimeEvent, String>newExchange(
                model.endpoint(),
                JacksonJsonUtils::toJson,
                s -> s
        );
        return new OmniRealtimeConversationImpl(exchange);
    }

    public static class BuilderImpl implements OmniOp.Builder {

        private String ak;
        private HttpClient http;

        @Override
        public Builder ak(String ak) {
            this.ak = ak;
            return this;
        }

        @Override
        public Builder http(HttpClient http) {
            this.http = http;
            return this;
        }

        @Override
        public OmniOp build() {
            final var exchangeExecutor = new HttpWsExchangeExecutor(ak, http);
            return new OmniOpImpl(exchangeExecutor);
        }

    }

}
