package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils;

import java.util.*;
import java.util.function.Function;
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
public final class AssistantMessage implements Message, Accumulator<AssistantMessage> {

    private final List<Content> contents;
    private final String reasoningContent;
    private final boolean partial;
    private final List<Tool.Call> calls;

    @JsonCreator
    private AssistantMessage(

            @JsonProperty("content")
            @JsonDeserialize(using = ContentListJsonDeserializer.class)
            List<Content> contents,

            @JsonProperty("reasoning_content")
            String reasoningContent,

            @JsonProperty("partial")
            boolean partial,

            @JsonProperty("tool_calls")
            List<Tool.Call> calls

    ) {
        this.contents = contents;
        this.reasoningContent = reasoningContent;
        this.partial = partial;
        this.calls = calls;
    }

    @Override
    public Role role() {
        return Role.AI;
    }

    @JsonProperty("content")
    public List<Content> contents() {
        return contents;
    }

    @JsonProperty("reasoning_content")
    public String reasoningContent() {
        return reasoningContent;
    }

    @JsonProperty("partial")
    public boolean partial() {
        return partial;
    }

    @JsonProperty("tool_calls")
    public List<Tool.Call> calls() {
        return calls;
    }

    @Override
    public String text() {
        return contents.stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .map(TextContent::text)
                .collect(Collectors.joining());
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
        final var mergedCalls = Stream.of(calls, next.calls)
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

        return new AssistantMessage(newContents, newReasoningContent, partial, mergedCalls);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(AssistantMessage message) {
        return new Builder(message);
    }

    public static class Builder implements Buildable<AssistantMessage, Builder> {

        private final List<Content> contents = new ArrayList<>();
        private final List<Tool.Call> calls = new ArrayList<>();
        private String reasoningContent;
        private boolean partial;

        public Builder() {

        }

        public Builder(AssistantMessage message) {
            this.contents.addAll(message.contents);
            this.calls.addAll(message.calls);
            this.partial = message.partial;
            this.reasoningContent = message.reasoningContent;
        }

        public Builder reasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
            return this;
        }

        public Builder contents(List<Content> contents) {
            this.contents.clear();
            this.contents.addAll(contents);
            return this;
        }

        public Builder addContent(Content content) {
            contents.add(content);
            return this;
        }

        public Builder addContents(List<Content> contents) {
            this.contents.addAll(contents);
            return this;
        }

        public Builder calls(List<Tool.Call> calls) {
            this.calls.clear();
            this.calls.addAll(calls);
            return this;
        }

        public Builder addCall(Tool.Call call) {
            this.calls.add(call);
            return this;
        }

        public Builder addCalls(List<Tool.Call> calls) {
            this.calls.addAll(calls);
            return this;
        }

        public Builder partial(boolean partial) {
            this.partial = partial;
            return this;
        }

        @Override
        public AssistantMessage build() {
            return new AssistantMessage(contents, reasoningContent, partial, calls);
        }

    }

}
