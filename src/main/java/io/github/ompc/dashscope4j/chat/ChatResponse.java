package io.github.ompc.dashscope4j.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.chat.message.Message;
import io.github.ompc.dashscope4j.internal.algo.AlgoResponse;
import io.github.ompc.dashscope4j.internal.api.ApiData;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 对话应答
 */
public class ChatResponse extends AlgoResponse<ChatResponse.Data, ChatResponse> {

    private final Choice best;

    /**
     * 构造对话应答
     *
     * @param uuid    UUID
     * @param code    返回结果编码
     * @param message 信息结果信息
     * @param usage   使用情况
     * @param data    数据
     */
    public ChatResponse(String uuid, String code, String message, Usage usage, Data data) {
        super(uuid, code, message, usage, data);

        // 获取最好的选择
        this.best = Optional.ofNullable(data)
                .map(Data::choices)
                .flatMap(choices -> choices.stream().sorted().findFirst())
                .orElse(null);
    }

    /**
     * 构造对话应答
     *
     * @param ret   返回结果
     * @param usage 使用情况
     * @param data  数据
     */
    public ChatResponse(Ret ret, Usage usage, Data data) {
        this(ret.uuid(), ret.code(), ret.message(), usage, data);
    }

    /**
     * 获取最好的选择
     * <p>如果返回的{@link Choice}集合为空，则返回null</p>
     *
     * @return 最好的选择
     */
    public Choice best() {
        return best;
    }

    @Override
    public ChatResponse aggregate(boolean increment, ChatResponse other) {

        // 合并目标为空，则以自己为准
        if (Objects.isNull(other)) {
            return this;
        }

        // 全量替换
        if (!increment) {
            return other;
        }

        // 增量合并
        final var choice = new Choice(
                other.best().finish(),
                Message.ofAi(best().message().text() + other.best().message().text())
        );

        // 返回合并后的应答
        return new ChatResponse(other.ret(), other.usage(), new Data(List.of(choice)));
    }

    @JsonDeserialize(using = Data.DataJsonDeserializer.class)
    public record Data(List<Choice> choices) implements ApiData {

        static class DataJsonDeserializer extends JsonDeserializer<Data> {

            @Override
            public Data deserialize(JsonParser parser, DeserializationContext context) throws IOException {

                final var node = context.readTree(parser);

                //
                if (node.has("choices")) {
                    final var choiceArray = context.readTreeAsValue(node.get("choices"), Choice[].class);
                    return new Data(List.of(choiceArray));
                }

                //
                final var finish = context.readTreeAsValue(node.get("finish_reason"), Choice.Finish.class);
                final var message = Message.ofAi(node.get("text").asText());
                return new Data(List.of(new Choice(finish, message)));
            }

        }

    }

    /**
     * 响应选择
     *
     * @param finish  响应结束标识
     * @param message 响应消息
     */
    public record Choice(

            @JsonProperty("finish_reason")
            Finish finish,

            @JsonProperty("message")
            Message message

    ) implements Comparable<Choice> {

        @Override
        public int compareTo(Choice o) {
            // 按照Finish.weight从小到大排序
            return finish.weight - o.finish.weight;
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
             * 截断结束
             */
            @JsonProperty("length")
            OVERFLOW(1),

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

        @JsonCreator
        static Choice of(

                @JsonProperty("finish_reason")
                Finish finish,

                @JsonProperty("message")
                Message message

        ) {
            return new Choice(
                    Objects.nonNull(finish) ? finish : Finish.NONE,
                    message
            );
        }

    }

    @JsonCreator
    static ChatResponse of(

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String message,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("output")
            ChatResponse.Data data

    ) {
        return new ChatResponse(uuid, code, message, usage, data);
    }

}
