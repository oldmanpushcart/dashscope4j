package io.github.oldmanpushcart.dashscope4j.api.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;


@Getter
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ChatResponse extends ApiResponse<ChatResponse.Output> {

    @JsonProperty("output")
    private final Output output;

    @JsonCreator
    public ChatResponse(

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String desc,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("output")
            Output output

    ) {
        super(uuid, code, desc, cleanUsage(usage));
        this.output = output;
    }

    /*
     * 清除无用的使用情况
     */
    private static Usage cleanUsage(Usage usage) {
        final List<Usage.Item> items = usage.items()
                .stream()

                /*
                 * Chat的系列会将tokens的使用总量以及所有子项的使用量都放在一起返回，导致使用过程中无法准确统计。
                 * 所以这里对总量进行过滤。如果想计算总量，则可直接对所有子项进行相加
                 */
                .filter(item -> !"total_tokens".equals(item.name()))

                .collect(Collectors.toList());
        return new Usage(unmodifiableList(items));
    }


    @Value
    @Accessors(fluent = true)
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    @JsonDeserialize(using = ChatResponseOutputJsonDeserializer.class)
    public static class Output {

        List<Choice> choices;

        public Output(Choice choice) {
            this(singletonList(choice));
        }

        public ChatResponse.Choice best() {
            return Optional.ofNullable(choices)
                    .flatMap(choices -> choices.stream().sorted().findFirst())
                    .orElse(null);
        }

    }


    @Getter
    @Accessors(fluent = true)
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class Choice implements Comparable<Choice> {

        private final Finish finish;
        private final List<Message> history;

        public Choice(Finish finish, Message message) {
            this(finish, singletonList(message));
        }

        public Message message() {
            return Objects.nonNull(history) && !history.isEmpty()
                    ? history.get(history.size() - 1)
                    : null;
        }

        @Override
        public int compareTo(Choice o) {
            return Integer.compare(finish().weight, o.finish().weight);
        }

    }

    /**
     * 结束标识
     */
    public enum Finish {

        /**
         * 正常结束
         */
        @JsonProperty("stop")
        NORMAL(0),

        /**
         * 工具调用
         */
        @JsonProperty("tool_calls")
        TOOL_CALLS(1),

        /**
         * 截断结束
         */
        @JsonProperty("length")
        OVERFLOW(2),

        /**
         * 尚未结束
         * <p>
         * 用于标识尚未结束，常见于开启了SSE的场景
         * </p>
         */
        @JsonProperty("null")
        NONE(100);

        private final int weight;

        Finish(int weight) {
            this.weight = weight;
        }

    }

}
