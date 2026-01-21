package io.github.oldmanpushcart.dashscope4j.client.aigc.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.Model;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;

public record ChatModel(String name, String path) implements Model<ChatModel.Input, ChatModel.Output> {

    /**
     * 输入参数
     */
    public static class Input {

        private final List<Message> messages;

        private Input(Builder builder) {
            this.messages = unmodifiableList(builder.messages);
        }

        @JsonProperty("messages")
        public List<Message> messages() {
            return messages;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static Builder newBuilder(Input input) {
            return new Builder(input);
        }

        public static class Builder implements Buildable<Input, Builder> {

            private final List<Message> messages = new ArrayList<>();

            public Builder() {

            }

            public Builder(Input input) {
                this.messages.addAll(input.messages);
            }

            public Builder messages(List<Message> messages) {
                this.messages.clear();
                this.messages.addAll(messages);
                return this;
            }

            public Builder addMessage(Message message) {
                messages.add(message);
                return this;
            }

            public Builder addMessages(List<Message> messages) {
                this.messages.addAll(messages);
                return this;
            }

            @Override
            public Input build() {
                return new Input(this);
            }
        }

    }


    /**
     * 输出参数
     */
    public record Output(

            @JsonProperty("search_info")
            Search search,

            @JsonProperty("choices")
            List<Choice> choices

    ) {

        /**
         * @return 最佳候选结果
         */
        public Choice best() {
            return Optional.ofNullable(choices)
                    .flatMap(choices -> choices.stream().sorted().findFirst())
                    .orElseThrow(() -> new IllegalArgumentException("No choices found!"));
        }

        /**
         * 候选结果
         *
         * @param finish  结束类型
         * @param message 消息
         */
        public record Choice(

                @JsonProperty("finish_reason")
                Finish finish,

                @JsonProperty("message")
                AssistantMessage message

        ) implements Comparable<Choice>, Accumulator<Choice> {

            /**
             * 合并两个候选结果
             *
             * @param next 合并对象
             * @return 合并后的候选结果
             */
            @Override
            public Choice accumulate(Choice next) {
                if (null == next) {
                    return this;
                }
                final var newMessage = message.accumulate(next.message);
                return new Choice(next.finish(), newMessage);
            }

            /**
             * 候选结果排序
             * <p>
             * 为了方便调用方从众多候选结果中获取到最优的结果，这里提供了一个默认排序方法。
             * 参与到排序的权重因子有：index、finish，但其中通义千问只返回了finish，所以这里只对finish状态不同值的权重进行排序。
             * </p>
             *
             * @param o another
             * @return compare result
             */
            @Override
            public int compareTo(Choice o) {
                return Integer.compare(finish().weight, o.finish().weight);
            }

        }

        /**
         * 结束类型
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


        /**
         * 搜索信息
         */
        public record Search(

                @JsonProperty("search_results")
                List<Result> results

        ) {

            /**
             * 搜索结果
             */
            public record Result(

                    @JsonProperty("index")
                    int index,

                    @JsonProperty("site_name")
                    String name,

                    @JsonProperty("title")
                    String title,

                    @JsonProperty("icon")
                    URI icon,

                    @JsonProperty("url")
                    URI site

            ) {

            }

        }

    }

}
