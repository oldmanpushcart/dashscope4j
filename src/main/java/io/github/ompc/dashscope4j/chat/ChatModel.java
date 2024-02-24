package io.github.ompc.dashscope4j.chat;

import io.github.ompc.dashscope4j.Model;

import java.net.URI;

/**
 * 对话模型
 */
public class ChatModel extends Model {

    /**
     * 构造对话模型
     *
     * @param name   名称
     * @param remote 远程地址
     */
    public ChatModel(String name, URI remote) {
        super(name, remote);
    }

    /**
     * QWEN-TURBO
     * <p>通义千问超大规模语言模型，支持中文、英文等不同语言输入。</p>
     * <p>模型支持8k tokens上下文，为了保证正常的使用和输出，API限定用户输入为6k tokens。</p>
     */
    public static ChatModel QWEN_TURBO = new ChatModel(
            "qwen-turbo",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
    );

    /**
     * QWEN-PLUS
     * <p>通义千问超大规模语言模型增强版，支持中文、英文等不同语言输入。</p>
     * <p>模型支持32k tokens上下文，为了保证正常的使用和输出，API限定用户输入为30k tokens。</p>
     */
    public static ChatModel QWEN_PLUS = new ChatModel(
            "qwen-plus",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
    );

    /**
     * QWEN-MAX
     * <p>通义千问千亿级别超大规模语言模型，支持中文、英文等不同语言输入。</p>
     * <p>模型支持8k tokens上下文，为了保证正常的使用和输出，API限定用户输入为6k tokens。</p>
     */
    public static ChatModel QWEN_MAX = new ChatModel(
            "qwen-max",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
    );

    /**
     * QWEN-MAX-LONG-CONTEXT
     * <p>通义千问千亿级别超大规模语言模型，支持中文、英文等不同语言输入。</p>
     * <p>模型支持30k tokens上下文，为了保证正常的使用和输出，API限定用户输入为28k tokens。</p>
     */
    public static ChatModel QWEN_MAX_LONGCONTEXT = new ChatModel(
            "qwen-max-longcontext",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
    );

    /**
     * QWEN-VL-PLUS
     * <p>通义千问大规模视觉语言模型增强版。</p>
     * <p>大幅提升细节识别能力和文字识别能力，支持超百万像素分辨率和任意长宽比规格的图像。在广泛的视觉任务上提供卓越的性能。</p>
     */
    public static ChatModel QWEN_VL_PLUS = new ChatModel(
            "qwen-vl-plus",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation")
    );

    /**
     * QWEN-VL-MAX
     * <p>通义千问超大规模视觉语言模型。</p>
     * <p>相比增强版，再次提升视觉推理能力和指令遵循能力，提供更高的视觉感知和认知水平。在更多复杂任务上提供最佳的性能。</p>
     */
    public static ChatModel QWEN_VL_MAX = new ChatModel(
            "qwen-vl-max",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation")
    );

    /**
     * QWEN-AUDIO-TURBO
     * <p>通义千问大规模语音模型增强版</p>
     * <p>增强了语音识别、语音定位、说话人信息识别和音乐分析鉴赏的能力。</p>
     */
    public static ChatModel QWEN_AUDIO_TURBO = new ChatModel(
            "qwen-audio-turbo",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation")
    );

    /**
     * QWEN-AUDIO-CHAT
     * <p>通义千问大规模语音模型</p>
     * <p>支持全音频类型的处理，包括多轮问答、音频推理与创作，同时还能识别说话人的情绪、性别，以及环境和音乐的多种特征。</p>
     */
    public static ChatModel QWEN_AUDIO_CHAT = new ChatModel(
            "qwen-audio-chat",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation")
    );

}
