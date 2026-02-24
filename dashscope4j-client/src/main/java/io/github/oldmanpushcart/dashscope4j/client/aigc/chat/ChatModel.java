package io.github.oldmanpushcart.dashscope4j.client.aigc.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.InlineFilesInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.SettingInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.UploadFilesInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.compat.openai.CompatOpenAiInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.compat.plaintext.CompatPlaintextInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.tool.ToolCallInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcModelTags;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.dashscope4j.common.Constants.*;
import static java.util.Collections.unmodifiableList;

public record ChatModel(
        String name,
        String path,
        Set<String> tags
) implements AigcModel<ChatModel.Input, ChatModel.Output> {

    public static final ChatModel QWEN_FLASH = new ChatModel("qwen-flash", TEXT_GENERATION_PATH);
    public static final ChatModel QWEN_PLUS = new ChatModel("qwen-plus", TEXT_GENERATION_PATH);
    public static final ChatModel QWEN_MAX = new ChatModel("qwen-max", TEXT_GENERATION_PATH);
    public static final ChatModel QWEN_LONG = new ChatModel("qwen-long", TEXT_GENERATION_PATH, Set.of(
            ChatModelTags.COMPAT_PLAINTEXT
    ));

    public static final ChatModel QWEN_VL_PLUS = new ChatModel("qwen-vl-plus", MULTIMODAL_GENERATION_PATH);
    public static final ChatModel QWEN_VL_MAX = new ChatModel("qwen-vl-max", MULTIMODAL_GENERATION_PATH);

    public static final ChatModel QWQ_PLUS = new ChatModel("qwq-plus", TEXT_GENERATION_PATH, Set.of(
            AigcModelTags.RESPONSE_MODE_FLOW,
            AigcModelTags.INCREMENTAL_OUTPUT_ONLY
    ));
    public static final ChatModel QVQ_MAX = new ChatModel("qvq-max", MULTIMODAL_GENERATION_PATH, Set.of(
            AigcModelTags.RESPONSE_MODE_FLOW,
            AigcModelTags.INCREMENTAL_OUTPUT_ONLY
    ));
    public static final ChatModel QWEN3_OMNI_FLASH = new ChatModel("qwen3-omni-flash", COMPAT_OPENAI_PATH, Set.of(
            ChatModelTags.COMPAT_OPENAI,
            AigcModelTags.RESPONSE_MODE_FLOW,
            AigcModelTags.INCREMENTAL_OUTPUT_ONLY
    ));

    public static final ChatModel QWEN_IMAGE_MAX = new ChatModel("qwen-image-max", MULTIMODAL_GENERATION_PATH);

    public static final ChatModel WAN_T2I = new ChatModel("wan2.6-t2i", MULTIMODAL_GENERATION_PATH, Set.of(
            AigcModelTags.RESPONSE_MODE_ASYNC,
            AigcModelTags.RESPONSE_MODE_TASK
    ));


    /*
     * 对话模型的拦截器列表
     * 这里实现了 ToolCall、自动上传、BASE64内联等对话模型的增强功能。
     *
     * PS：请务必注意拦截器的顺序
     */
    private static final List<Interceptor> interceptors = List.of(
            new SettingInterceptor(),
            new ToolCallInterceptor(),
            new InlineFilesInterceptor(),
            new UploadFilesInterceptor(),
            new CompatPlaintextInterceptor(),
            new CompatOpenAiInterceptor()
    );

    /**
     * 构造对话模型
     *
     * @param name 名称
     * @param path 路径
     */
    public ChatModel(String name, String path) {
        this(name, path, Set.of());
    }

    @Override
    public List<Interceptor> interceptors() {
        return Stream.of(AigcModel.super.interceptors(), interceptors)
                .flatMap(List::stream)
                .toList();
    }

    /**
     * 输入参数
     */
    public static class Input {

        private final List<Message> messages;
        private final boolean uploadEnabled;
        private final boolean inlineEnabled;
        private final boolean failOnToolError;

        private Input(Builder builder) {
            this.messages = unmodifiableList(builder.messages);
            this.uploadEnabled = builder.uploadEnabled;
            this.inlineEnabled = builder.inlineEnabled;
            this.failOnToolError = builder.failOnToolError;
        }

        /**
         * @return 消息列表
         */
        @JsonProperty("messages")
        public List<Message> messages() {
            return messages;
        }

        /**
         * @return 上传文件是否启用
         */
        @JsonIgnore
        public boolean uploadEnabled() {
            return uploadEnabled;
        }

        /**
         * @return 内联文件是否启用
         */
        @JsonIgnore
        public boolean inlineEnabled() {
            return inlineEnabled;
        }

        /**
         * @return 是否在工具调用出错时失败
         */
        @JsonIgnore
        public boolean failOnToolError() {
            return failOnToolError;
        }

        /**
         * @return 最后一条消息
         */
        public Message lastMessage() {
            return !messages.isEmpty()
                    ? messages.get(messages.size() - 1)
                    : null;
        }

        /**
         * 是否有用户输入信息
         *
         * @return TRUE | FALSE
         */
        public boolean hasUserInputMessage() {
            return !messages.isEmpty()
                    && null != userInputMessage();
        }

        /**
         * 提取用户输入信息
         * <p>
         * 消息列表中最后一条信息，且{@code role == USER}，为用户输入信息。
         * </p>
         *
         * @return 用户输入信息
         */
        public UserMessage userInputMessage() {
            final var last = lastMessage();
            return last instanceof UserMessage userMessage
                    ? userMessage
                    : null;

        }

        /**
         * 提取历史信息
         * <p>
         * 消息列表中下标范围[0,n-1]信息为历史信息
         * </p>
         *
         * @return 历史信息
         */
        public List<Message> historyMessages() {
            return hasUserInputMessage()
                    ? messages.subList(0, messages.size() - 1)
                    : messages;
        }


        public static Builder newBuilder() {
            return new Builder();
        }

        public static Builder newBuilder(Input input) {
            return new Builder(input);
        }

        public static class Builder implements Buildable<Input, Builder> {

            private final List<Message> messages = new ArrayList<>();
            private boolean uploadEnabled;
            private boolean inlineEnabled;
            private boolean failOnToolError;

            public Builder() {

            }

            public Builder(Input input) {
                this.messages.addAll(input.messages);
                this.uploadEnabled = input.uploadEnabled;
                this.inlineEnabled = input.inlineEnabled;
                this.failOnToolError = input.failOnToolError;
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

            public Builder addMessages(List<? extends Message> messages) {
                this.messages.addAll(messages);
                return this;
            }

            public Builder uploadEnabled(boolean uploadEnabled) {
                this.uploadEnabled = uploadEnabled;
                return this;
            }

            public Builder inlineEnabled(boolean inlineEnabled) {
                this.inlineEnabled = inlineEnabled;
                return this;
            }

            public Builder failOnToolError(boolean failOnToolError) {
                this.failOnToolError = failOnToolError;
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

    ) implements Accumulator<Output> {

        /**
         * @return 最佳候选结果
         */
        public Choice best() {
            return Optional.ofNullable(choices)
                    .flatMap(choices -> choices.stream().sorted().findFirst())
                    .orElseThrow(() -> new IllegalArgumentException("No choices found!"));
        }

        @Override
        public Output accumulate(Output next) {

            /*
             * 这里做一个特殊的兼容
             * 当 OpenAi 格式的返回流中，最后一个承载 usage 的 response 是没有 choices 的
             */
            final var newChoices = new ArrayList<Choice>();
            if (choices.isEmpty() || next.choices.isEmpty()) {
                newChoices.addAll(next.choices);
                newChoices.addAll(choices);
            } else {
                /*
                 * 检查等待合并的候选结果数量与当前对话应答的候选结果数量是否相等
                 * 如果不相等则说明无法合并
                 */
                final List<Choice> currChoices = choices;
                final List<Choice> nextChoices = next.choices;
                if (currChoices.size() != nextChoices.size()) {
                    throw new IllegalArgumentException("The size of choices is not equal! expect:%s but %s".formatted(
                            currChoices.size(),
                            nextChoices.size()
                    ));
                }

                /*
                 * 合并所有的候选结果
                 * 多个候选结果的顺序应该要保持一致
                 */
                final int length = currChoices.size();
                for (int index = 0; index < length; index++) {
                    final Choice currChoice = currChoices.get(index);
                    final Choice nextChoice = nextChoices.get(index);
                    newChoices.add(currChoice.accumulate(nextChoice));
                }
            }

            return new Output(
                    next.search,
                    newChoices
            );
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
