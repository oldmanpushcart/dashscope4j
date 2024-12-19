package io.github.oldmanpushcart.dashscope4j.api.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.api.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

/**
 * 对话应答
 * <pre><code>
 *
 * </code></pre>
 */
@Getter
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class ChatResponse extends AlgoResponse<ChatResponse.Output> {

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

        /*
         * 当chat调用出错（限流、命中敏感词等原因）时，usage为null
         * 此时需要进行特殊处理
         */
        if (null == usage) {
            return null;
        }
        
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


    /**
     * 输出
     */
    @Getter
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    @JsonDeserialize(using = ChatResponseOutputJsonDeserializer.class)
    public static class Output {

        /**
         * 候选结果集
         */
        private final List<Choice> choices;

        /**
         * 构造输出
         *
         * @param choice 候选结果
         */
        public Output(Choice choice) {
            this(singletonList(choice));
        }

        /**
         * 构造输出
         *
         * @param choices 候选结果集
         */
        public Output(List<Choice> choices) {
            this.choices = unmodifiableList(choices);
        }

        public ChatResponse.Choice best() {
            return Optional.ofNullable(choices)
                    .flatMap(choices -> choices.stream().sorted().findFirst())
                    .orElse(null);
        }

    }


    /**
     * 候选结果
     */
    @Getter
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Choice implements Comparable<Choice> {

        private final Finish finish;
        private final List<Message> history;

        /**
         * 构造候选结果
         *
         * @param finish  结束类型
         * @param message 结果消息
         */
        public Choice(Finish finish, Message message) {
            this(finish, singletonList(message));
        }

        /**
         * 构造候选结果
         *
         * @param finish  结束类型
         * @param history 历史消息列表
         *                <p>
         *                部分场景中候选结果会带多个消息出现，其主要记录了本次请求历史上曾经出现过的消息。<br/>
         *                比如Plugin、Tool的调用中会将PlugCallMessage/PlugMessage、ToolCallMessage/ToolMessage带入
         *                </p>
         */
        public Choice(Finish finish, List<Message> history) {
            this.finish = finish;
            this.history = unmodifiableList(history);
        }

        /**
         * @return 最新消息
         * <p>
         * 在部分对话场景中会将历史上出现过的消息也一并传入，但只有最后一个消息（最新消息）才是调用方关心的。
         * 所以这里提供了一个方法，方便调用方获取到最新的消息。
         * </p>
         */
        public Message message() {
            return Objects.nonNull(history) && !history.isEmpty()
                    ? history.get(history.size() - 1)
                    : null;
        }

        /**
         * 候选结果排序
         * <p>
         * 为了方便调用方从众多候选结果中获取到最优的结果，这里提供了一个默认排序方法。
         * 参与到排序的权重因子有：index、logProbs、finish，但其中通义千问只返回了finish，所以这里只对finish状态不同值的权重进行排序。
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

}
