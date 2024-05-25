package io.github.oldmanpushcart.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.Model;

import java.net.URI;

import static io.github.oldmanpushcart.dashscope4j.chat.ChatModel.Mode.MULTIMODAL;

/**
 * 对话模型
 *
 * @param mode   对话模型
 * @param name   模型名称
 * @param remote 模型地址
 */
public record ChatModel(Mode mode, String name, URI remote) implements Model {

    @Override
    public String label() {
        return "chat";
    }

    /**
     * 对话模型模式
     */
    public enum Mode {

        /**
         * 文本模式
         */
        TEXT,

        /**
         * 多模态模式
         */
        MULTIMODAL
    }

    /**
     * 获取模式
     *
     * @return 模式
     */
    public Mode mode() {
        return mode;
    }

    /**
     * 构建文本模型
     *
     * @param name 模型名称
     * @return 文本模型
     */
    public static ChatModel ofText(String name) {
        return new ChatModel(
                Mode.TEXT,
                name,
                URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
        );
    }

    /**
     * 构建多模态模型
     *
     * @param name 模型名称
     * @return 多模态模型
     */
    public static ChatModel ofMultimodal(String name) {
        return new ChatModel(
                MULTIMODAL,
                name,
                URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation")
        );
    }


    /**
     * QWEN-TURBO
     * <p>通义千问超大规模语言模型，支持中文、英文等不同语言输入。</p>
     * <p>模型支持8k tokens上下文，为了保证正常的使用和输出，API限定用户输入为6k tokens。</p>
     */
    public static ChatModel QWEN_TURBO = ofText("qwen-turbo");

    /**
     * QWEN-PLUS
     * <p>通义千问超大规模语言模型增强版，支持中文、英文等不同语言输入。</p>
     * <p>模型支持32k tokens上下文，为了保证正常的使用和输出，API限定用户输入为30k tokens。</p>
     */
    public static ChatModel QWEN_PLUS = ofText("qwen-plus");

    /**
     * QWEN-MAX
     * <p>通义千问千亿级别超大规模语言模型，支持中文、英文等不同语言输入。</p>
     * <p>模型支持8k tokens上下文，为了保证正常的使用和输出，API限定用户输入为6k tokens。</p>
     */
    public static ChatModel QWEN_MAX = ofText("qwen-max");

    /**
     * QWEN-MAX-LONG-CONTEXT
     * <p>通义千问千亿级别超大规模语言模型，支持中文、英文等不同语言输入。</p>
     * <p>模型支持30k tokens上下文，为了保证正常的使用和输出，API限定用户输入为28k tokens。</p>
     */
    public static ChatModel QWEN_MAX_LONGCONTEXT = ofText("qwen-max-longcontext");

    /**
     * QWEN-LONG
     * <p>通义千问超大规模语言模型，支持长文本上下文，以及基于长文档、多文档等多个场景的对话功能。</p>
     *
     * @since 1.4.2
     */
    public static ChatModel QWEN_LONGCONTEXT = ofText("qwen-long");

    /**
     * QWEN-VL-PLUS
     * <p>通义千问大规模视觉语言模型增强版。</p>
     * <p>大幅提升细节识别能力和文字识别能力，支持超百万像素分辨率和任意长宽比规格的图像。在广泛的视觉任务上提供卓越的性能。</p>
     */
    public static ChatModel QWEN_VL_PLUS = ofMultimodal("qwen-vl-plus");

    /**
     * QWEN-VL-MAX
     * <p>通义千问超大规模视觉语言模型。</p>
     * <p>相比增强版，再次提升视觉推理能力和指令遵循能力，提供更高的视觉感知和认知水平。在更多复杂任务上提供最佳的性能。</p>
     */
    public static ChatModel QWEN_VL_MAX = ofMultimodal("qwen-vl-max");

    /**
     * QWEN-AUDIO-TURBO
     * <p>通义千问大规模语音模型增强版</p>
     * <p>增强了语音识别、语音定位、说话人信息识别和音乐分析鉴赏的能力。</p>
     */
    public static ChatModel QWEN_AUDIO_TURBO = ofMultimodal("qwen-audio-turbo");

    /**
     * QWEN-AUDIO-CHAT
     * <p>通义千问大规模语音模型</p>
     * <p>支持全音频类型的处理，包括多轮问答、音频推理与创作，同时还能识别说话人的情绪、性别，以及环境和音乐的多种特征。</p>
     */
    public static ChatModel QWEN_AUDIO_CHAT = ofMultimodal("qwen-audio-chat");

}
