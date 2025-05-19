package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.Option;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.net.URI;

/**
 * 对话模型
 */
public interface ChatModel extends Model {

    /**
     * 文本模型地址
     */
    URI TEXT_REMOTE = URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation");

    /**
     * 多磨太模型地址
     */
    URI MULTIMODAL_REMOTE = URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation");

    /**
     * @return 对话模型模式
     */
    Mode mode();

    /**
     * 对话模型模式
     */
    enum Mode {

        /**
         * 文本模式
         */
        TEXT,

        /**
         * 多模态模式
         */
        MULTIMODAL
    }

    @Getter
    @Accessors(fluent = true)
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    class BaseChatModel extends BaseModel implements ChatModel {

        private final Mode mode;

        public BaseChatModel(Mode mode, String name, URI remote, Option option) {
            super(name, remote, option);
            this.mode = mode;
        }

        public BaseChatModel(Mode mode, String name, URI remote) {
            super(name, remote);
            this.mode = mode;
        }

        /**
         * 构建文本模型
         *
         * @param name 模型名称
         * @return 文本模型
         */
        public static BaseChatModel ofText(String name) {
            return new BaseChatModel(
                    Mode.TEXT,
                    name,
                    TEXT_REMOTE
            );
        }

        /**
         * 构建文本模型
         *
         * @param name   模型名称
         * @param option 模型参数
         * @return 文本模型
         */
        public static BaseChatModel ofText(String name, Option option) {
            return new BaseChatModel(
                    Mode.TEXT,
                    name,
                    TEXT_REMOTE,
                    option
            );
        }

        /**
         * 构建多模态模型
         *
         * @param name 模型名称
         * @return 多模态模型
         */
        public static BaseChatModel ofMultimodal(String name) {
            return new BaseChatModel(
                    Mode.MULTIMODAL,
                    name,
                    MULTIMODAL_REMOTE
            );
        }

        /**
         * 构建多模态模型
         *
         * @param name   模型名称
         * @param option 模型参数
         * @return 多模态模型
         */
        public static BaseChatModel ofMultimodal(String name, Option option) {
            return new BaseChatModel(
                    Mode.MULTIMODAL,
                    name,
                    MULTIMODAL_REMOTE,
                    option
            );
        }

    }


    /**
     * QWEN-TURBO
     * <p>通义千问超大规模语言模型，支持中文、英文等不同语言输入。</p>
     * <p>模型支持8k tokens上下文，为了保证正常的使用和输出，API限定用户输入为6k tokens。</p>
     */
    ChatModel QWEN_TURBO = BaseChatModel.ofText("qwen-turbo");

    /**
     * QWEN-PLUS
     * <p>通义千问超大规模语言模型增强版，支持中文、英文等不同语言输入。</p>
     * <p>模型支持32k tokens上下文，为了保证正常的使用和输出，API限定用户输入为30k tokens。</p>
     */
    ChatModel QWEN_PLUS = BaseChatModel.ofText("qwen-plus");

    /**
     * QWEN-MAX
     * <p>通义千问千亿级别超大规模语言模型，支持中文、英文等不同语言输入。</p>
     * <p>模型支持8k tokens上下文，为了保证正常的使用和输出，API限定用户输入为6k tokens。</p>
     */
    ChatModel QWEN_MAX = BaseChatModel.ofText("qwen-max");

    /**
     * QWEN-LONG
     * <p>通义千问超大规模语言模型，支持长文本上下文，以及基于长文档、多文档等多个场景的对话功能。</p>
     */
    ChatModel QWEN_LONG = BaseChatModel.ofText("qwen-long");

    /**
     * QWEN-VL-PLUS
     * <p>通义千问大规模视觉语言模型增强版。</p>
     * <p>大幅提升细节识别能力和文字识别能力，支持超百万像素分辨率和任意长宽比规格的图像。在广泛的视觉任务上提供卓越的性能。</p>
     */
    ChatModel QWEN_VL_PLUS = BaseChatModel.ofMultimodal("qwen-vl-plus");

    /**
     * QWEN-VL-MAX
     * <p>通义千问超大规模视觉语言模型。</p>
     * <p>相比增强版，再次提升视觉推理能力和指令遵循能力，提供更高的视觉感知和认知水平。在更多复杂任务上提供最佳的性能。</p>
     */
    ChatModel QWEN_VL_MAX = BaseChatModel.ofMultimodal("qwen-vl-max");

    /**
     * QWEN-AUDIO-TURBO
     * <p>通义千问大规模语音模型增强版</p>
     * <p>增强了语音识别、语音定位、说话人信息识别和音乐分析鉴赏的能力。</p>
     */
    ChatModel QWEN_AUDIO_TURBO = BaseChatModel.ofMultimodal("qwen-audio-turbo");

    /**
     * QWEN-AUDIO-CHAT
     * <p>通义千问大规模语音模型</p>
     * <p>支持全音频类型的处理，包括多轮问答、音频推理与创作，同时还能识别说话人的情绪、性别，以及环境和音乐的多种特征。</p>
     */
    ChatModel QWEN_AUDIO_CHAT = BaseChatModel.ofMultimodal("qwen-audio-chat");

    /**
     * QWEN2_AUDIO_INSTRUCT
     * <p>通义千问Audio更新增强版。</p>
     * <p>拓展音频多模态理解和生成能力，额外提供语音聊天与音频分析能力，能够实现自由灵活的音频交互。</p>
     */
    ChatModel QWEN2_AUDIO_INSTRUCT = BaseChatModel.ofMultimodal("qwen2-audio-instruct");

    /**
     * QWQ-PLUS
     * <p>通义千问对话模型推理（稳定版）</p>
     */
    ChatModel QWQ_PLUS = BaseChatModel.ofText("qwq-plus");

    /**
     * QWQ-PLUS-LATEST
     * <p>通义千问对话模型推理（最新版）</p>
     */
    ChatModel QWQ_PLUS_LATEST = BaseChatModel.ofText("qwq-plus-latest");

    /**
     * QVQ-MAX
     * <p>是视觉推理模型，支持视觉输入及思维链输出，在数学、编程、视觉分析、创作以及通用任务上都表现了更强的能力。</p>
     */
    ChatModel QVQ_MAX = BaseChatModel.ofMultimodal("qvq-max");

    /**
     * QWEN3-235B-A22B
     */
    ChatModel QWEN3_235B_A22B = BaseChatModel.ofText("qwen3-235b-a22b", new Option()
            .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
            .option(ChatOptions.ENABLE_THINKING, false)
            .unmodifiable());

}
