package io.github.oldmanpushcart.dashscope4j.agent.plugin.session;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.session.store.FragmentStore;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.concurrent.CompletionStage;

/**
 * 拦截器，设置会话
 */
class SettingInterceptor implements ChatInterceptor {

    private final ChatModel model;
    private final int maxTokens;
    private final int retainTokens;

    private final FragmentStore store;

    public SettingInterceptor(ChatModel model, FragmentStore store, int maxTokens, double gcRatio) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.retainTokens = (int) (maxTokens * gcRatio);
        this.store = store;
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

        /*
         * 获取会话ID
         * 会话插件必须要有会话ID，否则无法进行会话
         */
        final var sessionId = (String) (request.context().get("SESSION-ID"));
        if (null == sessionId) {
            return chain.proceed(request);
        }

        // 重新构建请求，埋入必要信息
        final var newRequest = AigcRequest.newBuilder(request)
                .context(context -> {

                    // 创建会话
                    final var session = new CompressSession(
                            sessionId,
                            store,
                            chain.client(),
                            model,
                            maxTokens,
                            retainTokens
                    );

                    // 放入上下文中，后续会使用
                    context.put("session", session);

                    return context;
                })
                .build();
        return chain.proceed(newRequest);
    }

}
