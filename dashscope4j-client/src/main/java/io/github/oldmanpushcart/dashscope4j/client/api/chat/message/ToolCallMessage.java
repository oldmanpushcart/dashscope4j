package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 工具调用消息
 * <p>
 * 由大模型侧发起，表明大模型期望调用本地工具。<br/>
 * {@code LLM > Client}
 * </p>
 */
@JsonDeserialize
public final class ToolCallMessage extends Message {


    private final List<Tool.Call> calls;

    @JsonCreator
    public ToolCallMessage(

            @JsonProperty("content")
            String content,

            @JsonProperty("tool_calls")
            List<Tool.Call> calls

    ) {
        super(Role.AI, content);
        this.calls = calls;
    }

    @JsonProperty("tool_calls")
    public List<Tool.Call> calls() {
        return calls;
    }

    @Override
    public Message accumulate(Message message) {

        if (!(message instanceof ToolCallMessage next)) {
            throw new IllegalArgumentException("Not a tool call message");
        }

        final List<Tool.Call> mergedCalls = Stream.of(calls, next.calls)
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
        final var mergedText = StringUtils.concat(text(), next.text());
        return new ToolCallMessage(mergedText, mergedCalls);

    }

}
