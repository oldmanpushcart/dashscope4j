package io.github.oldmanpushcart.dashscope4j.agent.tool.router;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolRegistry;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基于 Prompt 的工具路由
 */
public class PromptBaseToolRouter implements ToolRouter {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DashscopeClient client;
    private final float threshold;
    private final int limit;
    private final ChatModel model;

    private final PromptTemplate template = PromptTemplate.newBuilder()
            .template(ToolRegistry.class.getResourceAsStream("/prompt/PROMPT_BASE_TOOL_ROUTER.md"))
            .build();
    private final Type recommendListType = new TypeReference<List<Recommend>>() {
    }.getType();

    private PromptBaseToolRouter(Builder builder) {
        this.client = builder.client;
        this.threshold = builder.threshold;
        this.limit = builder.limit;
        this.model = builder.model;
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/tool/router/prompt-base";
    }

    @Override
    public CompletionStage<Map<String, Tool>> routing(Map<String, Tool> repository, String intent) {

        final var metas = repository.values()
                .stream()
                .filter(FunctionTool.class::isInstance)
                .map(FunctionTool.class::cast)
                .map(tool -> Map.of(
                        "name", tool.meta().name(),
                        "description", tool.meta().description()
                ))
                .toList();

        final var prompt = template.render(
                Map.of(
                        "THRESHOLD", threshold,
                        "LIMIT", limit,
                        "INTENT", intent,
                        "TOOLS", JacksonJsonUtils.toJson(metas)
                )
        );

        final var request = AigcRequest.newBuilder(model)
                .input(ChatModel.Input.newBuilder()
                        .addMessage(Message.system(prompt))
                        .build())
                .build();

        return client.async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(json -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("{} routing, intent: {}, recommend: {}", this, intent, JacksonJsonUtils.toNode(json));
                    }
                    return JacksonJsonUtils.<List<Recommend>>toObject(json, recommendListType);
                })
                .thenApply(recommends -> {

                    // 转换为工具仓库，为下一轮路由做准备
                    return recommends.stream()
                            .filter(recommend -> recommend.score() >= threshold)
                            .sorted((o1, o2) -> Float.compare(o2.score(), o1.score()))
                            .limit(limit)
                            .map(Recommend::identity)
                            .filter(repository::containsKey)
                            .collect(Collectors.toMap(Function.identity(), repository::get));

                });
    }

    private record Recommend(

            @JsonProperty("rank")
            int rank,

            @JsonProperty("name")
            String identity,

            @JsonProperty("score")
            float score,

            @JsonProperty("reason")
            String reason

    ) {


    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<PromptBaseToolRouter, Builder> {

        private DashscopeClient client;
        private float threshold = 0.5f;
        private int limit = 5;
        private ChatModel model = ChatModel.QWEN_FLASH;

        public Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        public Builder threshold(float threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        @Override
        public PromptBaseToolRouter build() {
            return new PromptBaseToolRouter(this);
        }

    }

}
