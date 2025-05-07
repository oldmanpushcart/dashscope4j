package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.Usage;
import io.github.oldmanpushcart.dashscope4j.client.api.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * 对话应答
 * <pre><code>
 *
 * </code></pre>
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ChatResponse extends AlgoResponse<ChatResponse.Output> {

    @JsonProperty("output")
    Output output;

    @JsonCreator
    private ChatResponse(

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
     * 修改候选结果
     *
     * @param operator 修改操作
     * @return 修改后的对话应答
     */
    public ChatResponse changeChoice(UnaryOperator<Choice> operator) {
        final List<ChatResponse.Choice> newChoices = output().choices()
                .stream()
                .map(choice -> requireNonNull(operator.apply(choice)))
                .collect(Collectors.toList());
        return new ChatResponse(
                (ChatRequest) request(),
                uuid(),
                code(),
                desc(),
                usage(),
                new ChatResponse.Output(
                        output().searchInfo(),
                        newChoices
                )
        );
    }

    /**
     * 合并对话应答
     *
     * @param next 等待被合并的对话应答
     * @return 合并后的应答
     */
    public ChatResponse accumulate(ChatResponse next) {

        /*
         * 检查等待合并的候选结果数量与当前对话应答的候选结果数量是否相等
         * 如果不相等则说明无法合并
         */
        final List<Choice> currChoices = output().choices();
        final List<Choice> nextChoices = next.output().choices();
        if (currChoices.size() != nextChoices.size()) {
            throw new IllegalArgumentException(String.format("The number of choices is not equal! expect:%s but %s",
                    currChoices.size(),
                    nextChoices.size()
            ));
        }

        /*
         * 合并所有的候选结果
         * 多个候选结果的顺序应该要保持一致
         */
        final List<Choice> newChoices = new ArrayList<>();
        final int length = currChoices.size();
        for (int index = 0; index < length; index++) {
            final Choice currChoice = currChoices.get(index);
            final Choice nextChoice = nextChoices.get(index);
            newChoices.add(currChoice.accumulate(nextChoice));
        }

        // 返回新的对话应答
        return new ChatResponse(
                (ChatRequest) next.request(),
                next.uuid(),
                next.code(),
                next.desc(),
                next.usage(),
                new ChatResponse.Output(
                        next.output().searchInfo(),
                        newChoices
                )
        );

    }

    /**
     * 输出
     */
    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    @JsonDeserialize(using = ChatResponseOutputJsonDeserializer.class)
    public static class Output {

        SearchInfo searchInfo;

        /**
         * 候选结果集
         */
        List<Choice> choices;

        /**
         * 构造输出
         *
         * @param search 搜索信息
         * @param choice 候选结果
         * @since 3.1.0
         */
        Output(SearchInfo search, Choice choice) {
            this(search, singletonList(choice));
        }

        /**
         * 构造输出
         *
         * @param search  搜索信息
         * @param choices 候选结果集
         * @since 3.1.0
         */
        Output(SearchInfo search, List<Choice> choices) {
            this.searchInfo = search;
            this.choices = unmodifiableList(choices);
        }

        /**
         * @return 最佳候选结果
         */
        public ChatResponse.Choice best() {
            return Optional.ofNullable(choices)
                    .flatMap(choices -> choices.stream().sorted().findFirst())
                    .orElse(null);
        }

        /**
         * @return 是否有搜索信息
         * @since 3.1.0
         */
        public boolean hasSearchInfo() {
            return Objects.nonNull(searchInfo)
                   && Objects.nonNull(searchInfo.results())
                   && !searchInfo.results().isEmpty();
        }

    }


    /**
     * 候选结果
     */
    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Choice implements Comparable<Choice> {

        Finish finish;
        List<Message> messages;

        /**
         * 构造候选结果
         *
         * @param finish  结束类型
         * @param message 结果消息
         */
        Choice(Finish finish, Message message) {
            this(finish, singletonList(message));
        }

        /**
         * 构造候选结果
         *
         * @param finish   结束类型
         * @param messages 消息列表
         *                 <p>
         *                 部分场景中候选结果会带多个消息出现，其主要记录了本次请求历史上曾经出现过的消息。<br/>
         *                 比如Plugin、Tool的调用中会将PlugCallMessage/PlugMessage、ToolCallMessage/ToolMessage带入
         *                 </p>
         */
        Choice(Finish finish, List<Message> messages) {
            this.finish = finish;
            this.messages = unmodifiableList(messages);
        }

        /**
         * @return 最新消息
         * <p>
         * 在部分对话场景中会将历史上出现过的消息也一并传入，但只有最后一个消息（最新消息）才是调用方关心的。
         * 所以这里提供了一个方法，方便调用方获取到最新的消息。
         * </p>
         * <p>
         * 与最新消息对应的则是历史消息，可以参考 {@link #history()}
         * </p>
         */
        public Message message() {
            return Objects.nonNull(messages) && !messages.isEmpty()
                    ? messages.get(messages.size() - 1)
                    : null;
        }

        /**
         * @return 历史消息
         * {@link #message()}
         */
        public List<Message> history() {
            return Objects.nonNull(messages) && !messages.isEmpty()
                    ? messages.subList(0, messages.size() - 1)
                    : Collections.emptyList();
        }

        /**
         * 合并两个候选结果
         *
         * @param next 合并对象
         * @return 合并后的候选结果
         */
        public Choice accumulate(Choice next) {
            final List<Message> newMessage = new ArrayList<>();
            newMessage.addAll(history());
            newMessage.addAll(next.history());
            newMessage.add(message().accumulate(next.message()));
            return new Choice(next.finish(), newMessage);
        }

        /**
         * 修改消息
         *
         * @param operator 修改操作
         * @return 修改后的候选结果
         */
        public Choice changeMessages(UnaryOperator<List<Message>> operator) {
            final List<Message> newMessages = operator.apply(messages);
            requireNonNull(newMessages);
            return new Choice(finish, newMessages);
        }

        /**
         * 修改消息
         *
         * @param operator 修改操作
         * @return 修改后的候选结果
         */
        public Choice changeMessage(UnaryOperator<Message> operator) {
            final List<Message> newMessages = new ArrayList<>(history());
            newMessages.add(requireNonNull(operator.apply(message())));
            return new Choice(finish(), newMessages);
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

    /**
     * 搜索信息
     *
     * @since 3.1.0
     */
    @Getter
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class SearchInfo {

        private final List<SearchResult> results;

        @JsonCreator
        private SearchInfo(

                @JsonProperty("search_results")
                List<SearchResult> results

        ) {
            this.results = Objects.isNull(results)
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(results);
        }

    }

    /**
     * 搜索结果
     *
     * @since 3.1.0
     */
    @Getter
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class SearchResult {

        private final int index;
        private final String name;
        private final String title;
        private final URI icon;
        private final URI site;

        @JsonCreator
        private SearchResult(

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
            this.index = index;
            this.title = title;
            this.site = site;

            if (null != name && !name.isEmpty()) {
                this.name = name;
            } else {
                this.name = site.getHost();
            }

            if (null != icon && !icon.toString().isEmpty()) {
                this.icon = icon;
            } else {
                this.icon = null;
            }

        }

    }

}
