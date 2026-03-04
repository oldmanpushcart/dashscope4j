package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.interceptor.ReActInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;

import java.util.ArrayList;
import java.util.List;

public class ReActChatAgent extends BaseChatAgent {

    private static final List<Interceptor> interceptors = List.of(
            new ReActInterceptor()
    );

    protected ReActChatAgent(Builder builder) {
        super(builder);
    }

    @Override
    protected List<Interceptor> interceptors() {
        final var superInterceptors = super.interceptors();
        if(superInterceptors == null) {
            return interceptors;
        } else {
            final var merged = new ArrayList<Interceptor>();
            merged.addAll(superInterceptors);
            merged.addAll(interceptors);
            return merged;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseChatAgent.Builder<ReActChatAgent, Builder> {

        @Override
        public ReActChatAgent build() {
            return new ReActChatAgent(this);
        }

    }

}
