package io.github.ompc.dashscope4j.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.chat.message.Message;
import io.github.ompc.dashscope4j.internal.algo.AlgoResponse;
import io.github.ompc.dashscope4j.internal.api.ApiData;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 对话应答
 */
public class ChatResponse extends AlgoResponse<ChatResponse.Data> {

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
    @JsonCreator
    public ChatResponse(

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String message,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("output")
            Data data

    ) {
        super(uuid, code, message, usage, data);

        // 获取最好的选择
        this.best = Optional.ofNullable(data)
                .map(Data::choices)
                .flatMap(choices -> choices.stream().sorted().findFirst())
                .orElse(null);
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

    @JsonDeserialize(using = Data.DataJsonDeserializer.class)
    public record Data(List<Choice> choices) implements ApiData {

        static class DataJsonDeserializer extends JsonDeserializer<Data> {

            @Override
            public Data deserialize(JsonParser parser, DeserializationContext context) throws IOException {

                final var node = context.readTree(parser);

                // openai格式
                if (node.has("choices")) {
                    final var choiceArray = context.readTreeAsValue(node.get("choices"), Choice[].class);
                    return new Data(List.of(choiceArray));
                }

                // 老格式
                final var finish = context.readTreeAsValue(node.get("finish_reason"), Finish.class);
                final var message = Message.ofAi(node.get("text").asText());
                return new Data(List.of(new Choice(finish, message)));

            }

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

    /**
     * 响应选择
     *
     * @param finish  响应结束标识
     * @param message 响应消息
     */
    public record Choice(

            @JsonProperty("finish_reason")
            ChatResponse.Finish finish,

            @JsonProperty("message")
            Message message

    ) implements Comparable<Choice> {

        @Override
        public int compareTo(Choice o) {
            return finish.weight - o.finish.weight;
        }

    }

}
