package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 助手消息
 * <p>
 * 模型对用户消息的回复。支持多模态回复、{@code Tool Call}
 * </p>
 * <p>
 * 指定前缀续写（Partial Mode）
 * 在代码补全、文本续写等场景中，需要模型从已有的文本片段（前缀）开始继续生成。
 * Partial Mode 可提供精确控制能力，确保模型输出的内容紧密衔接提供的前缀，提升生成结果的准确性与可控性。
 * 参考：<a href="https://help.aliyun.com/zh/model-studio/partial-mode">指定前缀续写（Partial Mode）</a>
 * </p>
 *
 */
public record AssistantMessage(

        @JsonProperty("content")
        @JsonDeserialize(using = ContentListJsonDeserializer.class)
        List<Content> contents,

        @JsonProperty("reasoning_content")
        String reasoningContent,

        @JsonProperty("partial")
        boolean partial,

        @JsonProperty("tool_calls")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<Tool.Call> calls,

        @JsonIgnore
        Set<String> tags

) implements Message, Accumulator<AssistantMessage> {

    private AssistantMessage(Builder builder) {
        this(
                CommonUtils.unmodifiableCopy(builder.contents),
                builder.reasoningContent,
                builder.partial,
                CommonUtils.unmodifiableCopy(builder.calls),
                CommonUtils.unmodifiableCopy(builder.tags)
        );
    }

    @Override
    public Role role() {
        return Role.AI;
    }

    @Override
    public String text() {
        return contents.stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .map(TextContent::text)
                .collect(Collectors.joining());
    }

    @Override
    public Message withCache(Content.CacheControl control) {
        return newBuilder(this)
                .contents(contents -> contents.stream()
                        .map(content -> content.withCache(control))
                        .toList())
                .build();
    }

    /**
     * @return 是否为工具调用
     */
    @JsonIgnore
    public boolean isToolCall() {
        return calls != null && !calls.isEmpty();
    }

    @Override
    public AssistantMessage accumulate(AssistantMessage next) {

        // 合并所有内容
        final var newContents = Stream.of(contents, next.contents)
                .flatMap(Collection::stream)
                .toList();

        // 合并理论推理内容
        final var newReasoningContent = CommonUtils.joinStrings(reasoningContent, next.reasoningContent);

        // 合并工具调用
        final var newCalls = Stream.of(calls, next.calls)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(
                        Tool.Call::index,
                        Function.identity(),
                        Accumulator::accumulate,
                        TreeMap::new
                ))
                .values()
                .stream()
                .toList();

        final var newTags = Stream.of(tags, next.tags)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        return AssistantMessage.newBuilder()
                .contents(newContents)
                .reasoningContent(newReasoningContent)
                .partial(partial)
                .calls(newCalls)
                .tags(newTags)
                .build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(AssistantMessage message) {
        return new Builder(message);
    }

    public static class Builder implements Buildable<AssistantMessage, Builder> {

        private List<Content> contents;
        private List<Tool.Call> calls;
        private String reasoningContent;
        private boolean partial;
        private Set<String> tags;

        public Builder() {

        }

        public Builder(AssistantMessage message) {
            this.contents = message.contents;
            this.calls = message.calls;
            this.partial = message.partial;
            this.reasoningContent = message.reasoningContent;
            this.tags = message.tags;
        }

        public Builder reasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
            return this;
        }

        public Builder contents(List<Content> contents) {
            this.contents = contents;
            return this;
        }

        public Builder contents(UnaryOperator<List<Content>> operator) {
            this.contents = operator.apply(CommonUtils.mutableCopy(this.contents));
            return this;
        }

        public Builder calls(List<Tool.Call> calls) {
            this.calls = calls;
            return this;
        }

        public Builder calls(UnaryOperator<List<Tool.Call>> operator) {
            this.calls = operator.apply(CommonUtils.mutableCopy(this.calls));
            return this;
        }

        public Builder tags(Set<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder tags(UnaryOperator<Set<String>> operator) {
            this.tags = operator.apply(CommonUtils.mutableCopy(this.tags));
            return this;
        }

        public Builder partial(boolean partial) {
            this.partial = partial;
            return this;
        }

        @Override
        public AssistantMessage build() {
            return new AssistantMessage(this);
        }

    }

}
