package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.AlgoModel;
import io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents;

import java.util.Set;

import static io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModelTags.*;

public class ChatModel extends AlgoModel {

    public ChatModel(String name, String path, Set<String> tags) {
        super(name, path, tags);
    }

    public ChatModel(String name, String path) {
        this(name, path, Set.of());
    }

    /**
     * QWEN-FLASH
     * <p>通义千问系列速度最快、成本极低的模型，适合简单任务。</p>
     */
    public static final ChatModel QWEN_FLASH = new ChatModel("qwen-flash", InternalContents.PATH_TEXT);

    /**
     * QWEN-TURBO
     * <p>通义千问超大规模语言模型，支持中文、英文等不同语言输入。</p>
     * <p>模型支持8k tokens上下文，为了保证正常的使用和输出，API限定用户输入为6k tokens。</p>
     */
    public static final ChatModel QWEN_TURBO = new ChatModel("qwen-turbo", InternalContents.PATH_TEXT);

    /**
     * QWEN-PLUS
     * <p>能力均衡，推理效果、成本和速度介于通义千问Max和通义千问Flash之间，适合中等复杂任务。</p>
     * <p>通义千问超大规模语言模型增强版，支持中文、英文等不同语言输入。</p>
     * <p>模型支持32k tokens上下文，为了保证正常的使用和输出，API限定用户输入为30k tokens。</p>
     */
    public static final ChatModel QWEN_PLUS = new ChatModel("qwen-plus", InternalContents.PATH_TEXT);

    /**
     * QWEN-PLUS-LATEST
     * <p>{@link #QWEN_PLUS}最新快照版</p>
     */
    public static final ChatModel QWEN_PLUS_LATEST = new ChatModel("qwen-plus-latest", InternalContents.PATH_TEXT);

    /**
     * QWEN-MAX
     * <p>通义千问千亿级别超大规模语言模型，支持中文、英文等不同语言输入。</p>
     * <p>模型支持8k tokens上下文，为了保证正常的使用和输出，API限定用户输入为6k tokens。</p>
     */
    public static final ChatModel QWEN_MAX = new ChatModel("qwen-max", InternalContents.PATH_TEXT);

    /**
     * QWEN-LONG
     * <p>通义千问超大规模语言模型，支持长文本上下文，以及基于长文档、多文档等多个场景的对话功能。</p>
     */
    public static final ChatModel QWEN_LONG = new ChatModel("qwen-long", InternalContents.PATH_TEXT, Set.of(
            COMPAT_PLAINTEXT
    ));

    /**
     * QWEN-LONG-LATEST
     * <p>{@link #QWEN_LONG}的最新快照版</p>
     */
    public static final ChatModel QWEN_LONG_LATEST = new ChatModel("qwen-long-latest", InternalContents.PATH_TEXT, Set.of(
            COMPAT_PLAINTEXT
    ));

    /**
     * QWQ-PLUS
     * <p>通义千问对话模型推理（稳定版）</p>
     */
    public static final ChatModel QWQ_PLUS = new ChatModel("qwq-plus", InternalContents.PATH_TEXT, Set.of(
            FLOW_OUTPUT_ONLY,
            INCREMENTAL_OUTPUT_ONLY
    ));

    /**
     * QWQ-PLUS-LATEST
     * <p>通义千问对话模型推理（最新版）</p>
     */
    public static final ChatModel QWQ_PLUS_LATEST = new ChatModel("qwq-plus-latest", InternalContents.PATH_TEXT, Set.of(
            FLOW_OUTPUT_ONLY,
            INCREMENTAL_OUTPUT_ONLY
    ));

    /**
     * QWEN-VL-PLUS
     * <p>通义千问大规模视觉语言模型增强版。</p>
     * <p>大幅提升细节识别能力和文字识别能力，支持超百万像素分辨率和任意长宽比规格的图像。在广泛的视觉任务上提供卓越的性能。</p>
     */
    public static final ChatModel QWEN_VL_PLUS = new ChatModel("qwen-vl-plus", InternalContents.PATH_MULTIMODAL);

    /**
     * QWEN-VL-MAX
     * <p>通义千问超大规模视觉语言模型。</p>
     * <p>相比增强版，再次提升视觉推理能力和指令遵循能力，提供更高的视觉感知和认知水平。在更多复杂任务上提供最佳的性能。</p>
     */
    public static final ChatModel QWEN_VL_MAX = new ChatModel("qwen-vl-max", InternalContents.PATH_MULTIMODAL);

    /**
     * QVQ-MAX
     * <p>视觉推理模型，支持视觉输入及思维链输出，在数学、编程、视觉分析、创作以及通用任务上都表现了更强的能力。</p>
     */
    public static final ChatModel QVQ_MAX = new ChatModel("qvq-max", InternalContents.PATH_MULTIMODAL, Set.of(
            FLOW_OUTPUT_ONLY,
            INCREMENTAL_OUTPUT_ONLY
    ));

    /**
     * QVQ-MAX-LATEST
     * <p>{@link #QVQ_MAX}最新快照</p>
     */
    public static final ChatModel QVQ_MAX_LATEST = new ChatModel("qvq-max-latest", InternalContents.PATH_MULTIMODAL, Set.of(
            FLOW_OUTPUT_ONLY,
            INCREMENTAL_OUTPUT_ONLY
    ));

    /**
     * QVQ-PLUS
     * <p>视觉推理模型，支持视觉输入及思维链输出，在数学、编程、视觉分析、创作以及通用任务上都表现了更强的能力。</p>
     */
    public static final ChatModel QVQ_PLUS = new ChatModel("qvq-plus", InternalContents.PATH_MULTIMODAL, Set.of(
            FLOW_OUTPUT_ONLY,
            INCREMENTAL_OUTPUT_ONLY
    ));

    /**
     * QVQ-PLUS-LATEST
     * <p>{@link #QVQ_PLUS}最新快照</p>
     */
    public static final ChatModel QVQ_PLUS_LATEST = new ChatModel("qvq-plus-latest", InternalContents.PATH_MULTIMODAL, Set.of(
            FLOW_OUTPUT_ONLY,
            INCREMENTAL_OUTPUT_ONLY
    ));

    /**
     * QWEN3-OMNI-FLASH
     * <p>能够接收文本、图片、音频、视频等多种模态的组合输入，并生成文本或语音形式的回复， 提供多种拟人音色，支持多语言和方言的语音输出。</p>
     * <p>可应用于文本创作、视觉识别、语音助手等场景。</p>
     */
    public static final ChatModel QWEN3_OMNI_FLASH = new ChatModel("qwen3-omni-flash", InternalContents.PATH_COMPAT_OPENAI, Set.of(
            COMPAT_OPENAI,
            FLOW_OUTPUT_ONLY,
            INCREMENTAL_OUTPUT_ONLY
    ));

    public static final ChatModel QWEN_IMAGE = new ChatModel("qwen-image", InternalContents.PATH_MULTIMODAL);

    public static final ChatModel QWEN_WAN = new ChatModel("wan2.6-t2i", InternalContents.PATH_MULTIMODAL, Set.of(
            ASYNC_OUTPUT_ONLY
    ));

}
