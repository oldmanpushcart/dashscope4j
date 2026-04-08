package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.ImageContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.GeneralAigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.BinaryFileSink;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Dashscope AI 工具加载器
 * <p>
 * 提供通义千问系列 AI 能力工具：
 * - analyze_document: 文档分析理解（PDF、Word 等）
 * - analyze_vision: 图片/视频内容理解
 * - edit_image: 图像编辑
 * - t2i: 文生图
 * - t2v: 文生视频
 * - tts: 文本转语音
 */
public class DashscopeToolLoader implements ToolLoader {

    public static final ToolLoader INSTANCE = new DashscopeToolLoader();

    /**
     * 文档理解模型配置
     */
    private static final GeneralAigcModel DOCUMENT_MODEL = GeneralAigcModel.newBuilder()
            .name("qwen-doc-turbo")
            .path("/api/v1/services/aigc/text-generation/generation")
            .uploadEnabled(true)
            .build();

    /**
     * 文档分析理解工具
     *
     * @return FunctionTool 实例
     */
    public static FunctionTool analyzeDocument() {
        return FunctionTool.newBuilder()
                .name("dashscope$analyze_document")
                .description("""
                        分析和理解文档内容（支持 PDF、Word、TXT、EXCEL 等格式）。
                        
                        【使用场景】
                        - 提取文档关键信息
                        - 总结文档内容
                        - 回答基于文档的问题
                        - 解析结构化数据
                        
                        【参数说明】
                        - prompt: 问题或指令（必需），例如：
                          * "总结这份文档的主要内容"
                          * "提取文档中的所有日期和事件"
                          * "这份文档的核心观点是什么"
                        - files: 文档路径列表（必需），支持：
                          * 本地文件路径（绝对路径或相对路径）：
                            - Windows: C:/Users/Name/Documents/doc.pdf 或 ./doc.pdf
                            - Linux/Mac: /home/user/doc.pdf 或 ./doc.pdf
                          * 网络 URL: https://example.com/doc.pdf
                          * 已上传的文件 URI
                        
                        【返回结果】
                        - 对 prompt 的文本回复
                        - 包含从文档中提取的信息
                        
                        【注意事项】
                        - 支持多种文档格式（PDF、DOCX、TXT、EXCEL 等）
                        - 可以提供多个文档进行交叉分析
                        - 确保 URI 可访问且文件格式正确
                        - 大文档可能需要更长的处理时间
                        """)
                .parameterType(AnalyzeDocumentSpec.class)
                .<AnalyzeDocumentSpec>function((caller, spec) -> {

                    final var input = new HashMap<String, Object>() {{
                        put("messages", new ArrayList<>() {{
                            add(new HashMap<>() {{
                                put("role", "user");
                                put("content", new ArrayList<>() {{
                                    add(new HashMap<>() {{
                                        put("type", "text");
                                        put("text", spec.prompt());
                                    }});
                                    add(new HashMap<>() {{
                                        put("type", "doc_url");
                                        put("file_parsing_strategy", "auto");
                                        put("doc_url", new ArrayList<>() {{
                                            addAll(spec.toUris());
                                        }});
                                    }});
                                }});
                            }});
                        }});
                    }};

                    final var request = AigcRequest.newBuilder(DOCUMENT_MODEL)
                            .input(input)
                            .build();

                    return caller.client().async(request)
                            .thenApply(AigcResponse::output);
                })
                .build();
    }

    /**
     * 文档分析规格
     */
    record AnalyzeDocumentSpec(

            @JsonPropertyDescription("问题或指令")
            @JsonProperty(value = "prompt", required = true)
            String prompt,

            @JsonPropertyDescription("文档路径列表")
            @JsonProperty(value = "files", required = true)
            List<String> files

    ) {

        List<URI> toUris() {
            return files.stream()
                    .map(DashscopeToolLoader::parseUri)
                    .toList();
        }
    }

    /**
     * 图片和视频理解工具
     *
     * @return FunctionTool 实例
     */
    public static FunctionTool analyzeVision() {
        return FunctionTool.newBuilder()
                .name("dashscope$analyze_vision")
                .description("""
                        分析和理解图片、视频内容（通义千问 VL 模型）。
                        
                        【使用场景】
                        - 识别图片中的物体、场景、文字
                        - 描述图片内容
                        - 回答关于图片的问题
                        - 理解视频内容和动作
                        - OCR 文字识别
                        
                        【参数说明】
                        - prompt: 问题或指令（必需），例如：
                          * "描述这张图片的内容"
                          * "图片中有哪些人？他们在做什么"
                          * "提取图片中的所有文字"
                        - images: 图片路径列表（可选）
                          * 本地文件路径（绝对路径或相对路径）：
                            - Windows: C:/Users/Name/Pictures/image.jpg 或 ./image.jpg
                            - Linux/Mac: /home/user/pictures/image.jpg 或 ./image.jpg
                          * 网络图片 URL: https://example.com/image.jpg
                        - videos: 视频路径列表（可选）
                          * 本地文件路径（绝对路径或相对路径）：C:/path/to/video.mp4 或 ./video.mp4
                          * 网络视频 URL: https://example.com/video.mp4
                        
                        【返回结果】
                        - 对图片/视频内容的文本描述
                        - 回答相关问题的答案
                        
                        【注意事项】
                        - 至少提供一张图片或一个视频
                        - 支持同时分析多张图片/多个视频
                        - 支持本地文件和网络 URL
                        - 视频文件大小和时长可能有限制
                        """)
                .parameterType(AnalyzeVisionSpec.class)
                .<AnalyzeVisionSpec>function((caller, spec) -> {
                    final var client = caller.client();
                    final var request = AigcRequest.newBuilder(ChatModel.QWEN_VL_MAX)
                            .input(ChatModel.Input.newBuilder()
                                    .addMessage(spec.toUserMessage())
                                    .uploadEnabled(true)
                                    .build())
                            .build();
                    return client.async(request)
                            .thenApply(response -> response.output().best().message().text());
                })
                .build();
    }

    /**
     * 视觉分析规格
     */
    record AnalyzeVisionSpec(

            @JsonPropertyDescription("问题或指令")
            @JsonProperty(value = "prompt", required = true)
            String prompt,

            @JsonPropertyDescription("图片路径列表")
            @JsonProperty("images")
            List<String> images,

            @JsonPropertyDescription("视频路径列表")
            @JsonProperty("videos")
            List<String> videos

    ) {

        List<URI> toImageUris() {
            return images != null ? images.stream()
                                    .map(DashscopeToolLoader::parseUri)
                                    .toList() : List.of();
        }

        List<URI> toVideoUris() {
            return videos != null ? videos.stream()
                                    .map(DashscopeToolLoader::parseUri)
                                    .toList() : List.of();
        }

        Message toUserMessage() {
            final var contents = new ArrayList<Content>();
            toImageUris().stream()
                    .map(Content::image)
                    .forEach(contents::add);
            toVideoUris().stream()
                    .map(Content::video)
                    .forEach(contents::add);
            contents.add(Content.text(prompt));
            return Message.user(contents);
        }

    }

    /**
     * 图像编辑工具
     *
     * @return FunctionTool 实例
     */
    public static FunctionTool imageEdit() {
        final ChatModel IMAGE_EDIT_MODEL = new ChatModel("qwen-image-edit-max", "/api/v1/services/aigc/multimodal-generation/generation");

        return FunctionTool.newBuilder()
                .name("dashscope$edit_image")
                .description("""
                        编辑和修改图片（通义千问图像编辑模型）。
                        
                        【使用场景】
                        - 修改图片风格、色调
                        - 添加/移除图片元素
                        - 调整图片构图
                        - 图片修复和增强
                        
                        【参数说明】
                        - prompt: 编辑指令（必需），例如：
                          * "把背景换成蓝天"
                          * "让图片更明亮一些"
                          * "添加一只猫在桌子上"
                        - negative: 负面提示词（可选），描述不想要的内容
                        - images: 原图路径列表（必需），支持：
                          * 本地文件路径（绝对路径或相对路径）：
                            - Windows: C:/Users/Name/Pictures/image.jpg 或 ./image.jpg
                            - Linux/Mac: /home/user/pictures/image.jpg 或 ./image.jpg
                          * 网络图片 URL: https://example.com/image.jpg
                        - size: 输出图片尺寸（可选）
                        - number: 生成数量（可选）
                        
                        【返回结果】
                        - 编辑后的图片 URI 列表
                        
                        【注意事项】
                        - 需要提供至少一张原图
                        - prompt 应该清晰描述期望的修改
                        - 支持批量处理多张图片
                        """)
                .parameterType(ImageEditSpec.class)
                .<ImageEditSpec>function((caller, spec) -> {

                    final var request = AigcRequest.newBuilder(IMAGE_EDIT_MODEL)
                            .input(ChatModel.Input.newBuilder()
                                    .building(builder -> {

                                        final var contents = new ArrayList<Content>();
                                        contents.add(Content.text(spec.prompt()));
                                        spec.toImageUris().stream()
                                                .map(Content::image)
                                                .forEach(contents::add);
                                        builder.addMessage(Message.user(contents));

                                    })
                                    .build())
                            .parameters(parameters -> {
                                parameters.put("prompt_extend", true);
                                if (spec.number() != null) {
                                    parameters.put("n", spec.number());
                                }
                                if (spec.size() != null) {
                                    parameters.put("size", String.format("%d*%d", spec.size().width, spec.size().height));
                                }
                                if (spec.negative() != null) {
                                    parameters.put("negative_prompt", spec.negative());
                                }
                                return parameters;
                            })
                            .build();

                    return caller.client().async(request)
                            .thenApply(response -> response.output().best().message().contents());
                })
                .build();
    }

    /**
     * 图像编辑规格
     */
    record ImageEditSpec(

            @JsonPropertyDescription("编辑指令")
            @JsonProperty(value = "prompt", required = true)
            String prompt,

            @JsonPropertyDescription("负面提示词")
            @JsonProperty("negative")
            String negative,

            @JsonPropertyDescription("原图路径列表")
            @JsonProperty("images")
            List<String> images,

            @JsonPropertyDescription("输出图片尺寸")
            @JsonProperty("size")
            Size size,

            @JsonPropertyDescription("生成数量")
            @JsonProperty("number")
            Integer number

    ) {

        List<URI> toImageUris() {
            return images != null ? images.stream()
                                    .map(DashscopeToolLoader::parseUri)
                                    .toList() : List.of();
        }

        record Size(

                @JsonPropertyDescription("图片高度（像素），取值范围：[512,2048]")
                @JsonProperty("height")
                int height,

                @JsonPropertyDescription("图片宽度（像素），取值范围：[512,2048]")
                @JsonProperty("width")
                int width

        ) {

        }

    }

    /**
     * 文生图工具
     *
     * @return FunctionTool 实例
     */
    public static FunctionTool t2i() {
        return FunctionTool.newBuilder()
                .name("dashscope$t2i")
                .description("""
                        根据文字描述生成图片（通义万相模型）。
                        
                        【使用场景】
                        - 创意设计和灵感可视化
                        - 生成插画、概念图
                        - 快速原型设计
                        - 艺术创作
                        
                        【参数说明】
                        - prompt: 图片描述（必需），越详细越好，例如：
                          * "一只橘色的猫在阳光下睡觉"
                          * "未来城市的夜景，霓虹灯闪烁"
                        - negative: 负面提示词（可选），描述不想要的内容
                        
                        【返回结果】
                        - 生成图片的 URI
                        
                        【注意事项】
                        - prompt 越详细，生成效果越好
                        - 支持中文和英文描述
                        - 自动启用 prompt 优化功能
                        """)
                .parameterType(T2iSpec.class)
                .<T2iSpec>function((caller, spec) -> {
                    final var client = caller.client();
                    final var request = AigcRequest.newBuilder(ChatModel.QWEN_IMAGE_MAX)
                            .input(ChatModel.Input.newBuilder()
                                    .addMessage(Message.user(spec.prompt()))
                                    .uploadEnabled(true)
                                    .build())
                            .parameters(parameters -> {
                                parameters.put("prompt_extend", true);
                                if (spec.negative() != null) {
                                    parameters.put("negative_prompt", spec.negative());
                                }
                                return parameters;
                            })
                            .build();
                    return client.async(request)
                            .thenApply(response -> {
                                final var message = response.output().best().message();
                                return message.contents().stream()
                                        .filter(ImageContent.class::isInstance)
                                        .map(ImageContent.class::cast)
                                        .map(ImageContent::image)
                                        .findFirst()
                                        .orElseThrow();
                            });
                })
                .build();
    }

    /**
     * 文生图规格
     */
    record T2iSpec(

            @JsonPropertyDescription("图片描述")
            @JsonProperty(value = "prompt", required = true)
            String prompt,

            @JsonPropertyDescription("负面提示词")
            @JsonProperty("negative")
            String negative

    ) {

    }

    /**
     * 文生视频工具
     *
     * @return FunctionTool 实例
     */
    public static FunctionTool t2v() {
        final GeneralAigcModel VIDEO_MODEL = GeneralAigcModel.newBuilder()
                .name("wan2.6-t2v")
                .path("/api/v1/services/aigc/video-generation/video-synthesis")
                .uploadEnabled(true)
                .build();

        return FunctionTool.newBuilder()
                .name("dashscope$t2v")
                .description("""
                        根据文字描述生成视频（通义万相视频模型）。
                        
                        【使用场景】
                        - 创意视频制作
                        - 动态内容展示
                        - 短视频创作
                        - 产品演示视频
                        
                        【参数说明】
                        - prompt: 视频描述（必需），越详细越好，例如：
                          * "海浪拍打沙滩的慢动作"
                          * "城市街道的车水马龙延时摄影"
                        - audio: 音频路径（可选），用于配音视频
                          * 本地文件路径（绝对路径或相对路径）：C:/path/to/audio.mp3 或 ./audio.mp3
                          * 网络音频 URL: https://example.com/audio.mp3
                        - size: 视频尺寸（可选），默认 720P
                        - duration: 时长秒数（可选），范围 [2,15]
                        - shot_type: 镜头类型（可选）
                          * single: 单镜头
                          * multi: 多镜头
                        \n
                        【返回结果】
                        - 生成的视频任务结果（包含视频 URL）
                        \n
                        【注意事项】
                        - prompt 应该清晰描述画面内容和动作
                        - 支持 2-15 秒时长
                        - 有多种分辨率可选
                        - 生成需要一定时间，请耐心等待
                        """)
                .parameterType(T2vSpec.class)
                .<T2vSpec>function((caller, spec) -> {

                    final var input = new HashMap<String, Object>();
                    input.put("prompt", spec.prompt());
                    if (spec.audio() != null) {
                        input.put("audio_url", parseUri(spec.audio()));
                    }

                    final var request = AigcRequest.newBuilder(VIDEO_MODEL)
                            .input(input)
                            .parameters(parameters -> {
                                if (spec.duration() != null) {
                                    parameters.put("duration", spec.duration());
                                }
                                if (spec.size() != null) {
                                    parameters.put("size", spec.size());
                                }
                                if (spec.shotType() != null) {
                                    parameters.put("shot_type", spec.shotType());
                                }
                                return parameters;
                            })
                            .build();

                    return caller.client().task(request)
                            .thenCompose(half ->
                                    half.waitingFor(Task.WaitStrategies.always(Duration.ofSeconds(1))));
                })
                .build();
    }

    /**
     * 文生视频规格
     */
    record T2vSpec(

            @JsonPropertyDescription("视频描述")
            @JsonProperty(value = "prompt", required = true)
            String prompt,

            @JsonPropertyDescription("音频路径")
            @JsonProperty("audio")
            String audio,

            @JsonPropertyDescription("视频尺寸")
            @JsonProperty("size")
            VideoSize size,

            @JsonPropertyDescription("视频时长（秒），取值范围：[2,15]")
            @JsonProperty("duration")
            Integer duration,

            @JsonPropertyDescription("镜头类型")
            @JsonProperty("shot_type")
            ShotType shotType

    ) {

        enum ShotType {

            @JsonPropertyDescription("单镜头")
            @JsonProperty("single")
            SINGLE,

            @JsonPropertyDescription("多镜头")
            @JsonProperty("multi")
            MULTI

        }

        enum VideoSize {

            @JsonProperty("1280*720")
            S_720P_1280X720,

            @JsonProperty("720*1280")
            S_720P_720X1280,

            @JsonProperty("960*960")
            S_720P_960X960,

            @JsonProperty("1088*832")
            S_720P_1088X832,

            @JsonProperty("832*1088")
            S_720P_832X1088,

            @JsonProperty("1920*1080")
            S_1080P_1920X1080,

            @JsonProperty("1080*1920")
            S_1080P_1080X1920,

            @JsonProperty("1440*1440")
            S_1080P_1440X1440,

            @JsonProperty("1632*1248")
            S_1080P_1632X1248,

            @JsonProperty("1248*1632")
            S_1080P_1248X1632

        }

    }

    /**
     * 文本转语音工具
     *
     * @return FunctionTool 实例
     */
    public static FunctionTool tts() {
        return FunctionTool.newBuilder()
                .name("dashscope$tts")
                .description("""
                        将文本转换为自然流畅的语音（CosyVoice 模型）。
                        
                        【使用场景】
                        - 有声书制作
                        - 视频配音
                        - 语音助手
                        - 无障碍阅读
                        
                        【参数说明】
                        - text: 要转换的文本（必需）
                        - voice: 音色选择（可选），默认男声
                          * longanyang: 成熟男声
                          * longanhuan: 温柔女声
                          * longhuhu_v3: 可爱童声
                        - format: 音频格式（可选），默认 MP3
                          * mp3: 压缩格式，体积小
                          * wav: 无损格式，音质好
                          * pcm: 原始音频数据
                        \n
                        【返回结果】
                        - 生成音频文件的临时路径 URI
                        \n
                        【注意事项】
                        - 支持中英文混合
                        - 音频文件存储在临时目录，请及时处理
                        - 长文本可能需要更长的处理时间
                        """)
                .parameterType(TtsSpec.class)
                .<TtsSpec>function((caller, spec) -> {
                    final var session = CosyVoiceSession.newBuilder()
                            .model(CosyVoiceModel.COSYVOICE_V3_PLUS)
                            .parameters(parameters -> {

                                parameters.put("voice", null != spec.voice() ? spec.voice() : "longanyang");
                                parameters.put("format", switch (spec.format()) {
                                    case PCM -> "pcm";
                                    case WAV -> "wav";
                                    case MP3 -> "mp3";
                                });

                                return parameters;
                            })
                            .build();

                    final File targetFile;
                    try {
                        targetFile = Files.createTempFile("dashscope_tts_", ".tmp").toFile();
                        targetFile.deleteOnExit();
                    } catch (IOException ioEx) {
                        return CompletableFuture.failedStage(ioEx);
                    }

                    return caller.client()
                            .realtime(session, new BinaryFileSink<>(targetFile) {

                                @Override
                                public void onOpen(Realtime.Emitter<CosyVoiceModel.In> emitter) {
                                    super.onOpen(emitter);
                                    final var cvEmitter = (CosyVoiceEmitter) emitter;
                                    cvEmitter.text(spec.text())
                                            .close();
                                }

                            })
                            .thenCompose(Realtime.Connection::closeFuture)
                            .thenApply(unused -> targetFile.toURI());
                })
                .build();
    }

    /**
     * 文本转语音规格
     */
    record TtsSpec(

            @JsonPropertyDescription("要转换的文本")
            @JsonProperty(value = "text", required = true)
            String text,

            @JsonPropertyDescription("音色选择")
            @JsonProperty("voice")
            Voice voice,

            @JsonPropertyDescription("音频格式")
            @JsonProperty("format")
            AudioFormat format

    ) {

        enum AudioFormat {

            @JsonProperty("pcm")
            PCM,

            @JsonProperty("wav")
            WAV,

            @JsonProperty("mp3")
            MP3

        }

        enum Voice {

            @JsonPropertyDescription("成熟男声")
            @JsonProperty("longanyang")
            LONGANYANG,

            @JsonPropertyDescription("温柔女声")
            @JsonProperty("longanhuan")
            LONGANHUAN,

            @JsonPropertyDescription("可爱童声")
            @JsonProperty("longhuhu_v3")
            LONGHUHU

        }

    }

    @Override
    public CompletionStage<Void> install(Toolbox toolbox) {
        List<FunctionTool> tools = List.of(
                analyzeDocument(),
                analyzeVision(),
                imageEdit(),
                t2i(),
                t2v(),
                tts()
        );

        // 并行等待所有 upsert 操作完成
        final var stages = tools.stream()
                .map(tool -> toolbox.register(tool.meta().name(), tool))
                .toList();
        return CompletableFutureUtils.allOf(10, stages);
    }

    @Override
    public void close() {

    }

    /**
     * 解析 URI 字符串，智能识别不同类型的资源标识符
     * - 如果已经是 URI 格式（包含 scheme），则直接返回
     * - 如果是本地文件路径，则转换为 file:// URI
     *
     * @param uriOrPath URI 字符串或文件路径
     * @return 解析后的 URI
     */
    private static URI parseUri(String uriOrPath) {
        // 尝试直接解析为 URI
        try {
            final var uri = URI.create(uriOrPath);
            // 如果有 scheme（如 https://, file://, fileid://），说明已经是 URI 格式
            if (uri.getScheme() != null && !uri.getScheme().isEmpty()) {
                return uri;
            }
        } catch (IllegalArgumentException e) {
            // 不是合法的 URI 格式，按本地文件路径处理
        }
        // 否则当作本地文件路径处理
        return Paths.get(uriOrPath).toUri();
    }

}
