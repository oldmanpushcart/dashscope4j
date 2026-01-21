package io.github.oldmanpushcart.dashscope4j.client.chat;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.client.Usage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;

import java.util.ArrayList;
import java.util.List;

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

        super(request, uuid, code, desc, usage);
        this.output = output;

    }

    @Override
    public ChatRequest request() {
        return (ChatRequest) super.request();
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



    ) {



    }





}
