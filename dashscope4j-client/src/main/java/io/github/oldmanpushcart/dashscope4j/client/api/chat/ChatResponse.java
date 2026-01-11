package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 对话应答
 */
public class ChatResponse extends AlgoResponse<ChatResponse.Output> implements Accumulator<ChatResponse> {

    private final Output output;

    @JsonCreator
    public ChatResponse(

            @JacksonInject("dashscope/request")
            ChatRequest request,

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

        super(request, uuid, code, desc, cleanUsage(usage));
        this.output = output;

    }

    @Override
    public ChatRequest request() {
        return (ChatRequest) super.request();
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

        final var items = usage.items()
                .stream()

                /*
                 * Chat的系列会将tokens的使用总量以及所有子项的使用量都放在一起返回，导致使用过程中无法准确统计。
                 * 所以这里对总量进行过滤。如果想计算总量，则可直接对所有子项进行相加
                 */
                .filter(item -> !"total_tokens".equals(item.name()))

                .toList();
        return new Usage(items);
    }

    @Override
    public ChatResponse accumulate(ChatResponse next) {

        /*
         * 如果是全量输出，则直接用 next 代替 curr，
         * 因为 next 中包含了全量信息
         */
        final var request = request();
        if (null == request || !request.parameters().has(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, true)) {
            return next;
        }

        /*
         * 检查等待合并的候选结果数量与当前对话应答的候选结果数量是否相等
         * 如果不相等则说明无法合并
         */
        final List<Choice> currChoices = output().choices();
        final List<Choice> nextChoices = next.output().choices();

        /*
         * 合并所有的候选结果
         * 多个候选结果的顺序应该要保持一致
         */
        final List<Choice> newChoices = new ArrayList<>();


        /*
         * 正常情况下，进行合并的 Choices 数量必须一致，这样才能逐条进行合并。
         *
         * 但有一种情况比较特殊，在 OpenAi 兼容模式中，最后一条 response 的 choices 是空集合，只用来返回 usage 信息。
         * 这种情况下应该只认有值的一方即可
         */
        if (currChoices.size() == nextChoices.size()) {
            final int length = currChoices.size();
            for (int index = 0; index < length; index++) {
                final Choice currChoice = currChoices.get(index);
                final Choice nextChoice = nextChoices.get(index);
                newChoices.add(currChoice.accumulate(nextChoice));
            }
        } else {
            if (currChoices.isEmpty()) {
                newChoices.addAll(nextChoices);
            } else if (nextChoices.isEmpty()) {
                newChoices.addAll(currChoices);
            } else {
                throw new IllegalArgumentException("The number of choices is not equal! expect:%s but %s".formatted(
                        currChoices.size(),
                        nextChoices.size()
                ));
            }
        }

        // 返回新的对话应答
        return new ChatResponse(
                next.request(),
                next.uuid(),
                next.code(),
                next.desc(),
                next.usage(),
                new Output(
                        next.output().search(),
                        newChoices
                )
        );

    }

    @Override
    public Output output() {
        return output;
    }

    /**
     * 输出
     *
     * @param search  搜索信息
     * @param choices 候选结果
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
