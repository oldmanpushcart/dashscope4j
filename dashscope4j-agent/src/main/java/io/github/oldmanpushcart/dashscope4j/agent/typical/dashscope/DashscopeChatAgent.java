package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.function.*;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;

/**
 * DashScope 智能体
 */
public class DashscopeChatAgent extends BaseChatAgent {

    private final boolean autoUploadEnabled;

    private DashscopeChatAgent(Builder builder) {
        super(builder);
        this.autoUploadEnabled = builder.autoUploadEnabled;
    }

    @Override
    protected CompletionStage<ChatResponse> baseAsync(ChatRequest request) {
        return client().chat().async(request);
    }

    @Override
    protected CompletionStage<Flowable<ChatResponse>> baseFlow(ChatRequest request) {
        return client().chat().flow(request);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseChatAgent.Builder<DashscopeChatAgent, Builder> {

        private boolean autoUploadEnabled;

        public Builder() {

        }

        public Builder(DashscopeChatAgent agent) {
            super(agent);
            this.autoUploadEnabled = agent.autoUploadEnabled;
        }

        public Builder enableAutoUpload(boolean enabled) {
            this.autoUploadEnabled = enabled;
            return self();
        }

        @Override
        public DashscopeChatAgent build() {
            addFunctions(Arrays.asList(

                    // 图生图
                    new DashscopeGenImageByImageFunction()
                            .autoUploadEnabled(autoUploadEnabled),

                    // 文生图
                    new DashscopeGenImageByTextFunction()
                            .autoUploadEnabled(autoUploadEnabled),

                    // 图生视频
                    new DashscopeGenVideoByImageFunction()
                            .autoUploadEnabled(autoUploadEnabled),

                    // 文生视频
                    new DashscopeGenVideoByTextFunction(),

                    // 文档解析
                    new DashscopeUnderstandingForDocumentFunction()
                            .autoUploadEnabled(autoUploadEnabled),

                    // 视觉解析
                    new DashscopeUnderstandingForVisualFunction()
                            .autoUploadEnabled(autoUploadEnabled),

                    // 网络搜索
                    new DashscopeWebSearchFunction()

            ));
            return new DashscopeChatAgent(this);
        }

    }

}
