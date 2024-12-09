package io.github.oldmanpushcart.dashscope4j.api.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.singletonList;


@Getter
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newBuilder")
@Jacksonized
public class ChatResponse extends ApiResponse<ChatResponse.Output> {

    @JsonProperty("output")
    private final Output output;

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
