package io.github.ompc.dashscope4j.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.chat.message.Message;
import io.github.ompc.dashscope4j.internal.algo.AlgoResponse;
import io.github.ompc.dashscope4j.internal.api.ApiResponse;

import java.util.List;
import java.util.Optional;

/**
 * 对话应答
 */
public interface ChatResponse extends AlgoResponse<ChatResponse.Output> {

    /**
     * 获取最好的选择
     * <p>如果返回的{@link Choice}集合为空，则返回null</p>
     *
     * @return 最好的选择
     */
    default Choice best() {
        return Optional.ofNullable(output())
                .map(Output::choices)
                .flatMap(choices -> choices.stream().sorted().findFirst())
                .orElse(null);
    }

    /**
     * 对话应答数据
     */
    interface Output extends ApiResponse.Output {

        List<Choice> choices();

    }

    /**
     * 结束标识
     */
    enum Finish {

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
     */
    interface Choice extends Comparable<Choice> {

        /**
         * 获取响应结束标识
         *
         * @return 响应结束标识
         */
        Finish finish();

        /**
         * 获取响应消息
         *
         * @return 响应消息
         */
        Message message();

        @Override
        default int compareTo(Choice o) {
            return finish().weight - o.finish().weight;
        }

    }

}
