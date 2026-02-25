package io.github.oldmanpushcart.dashscope4j.agent.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.ImageContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.GeneralAigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.BinaryFileSink;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class DashscopeTools {

    /**
     * @return 文档理解工具
     */
    public static Tool document() {
        return new Supplier<Tool>() {

            @Override
            public Tool get() {
                return FunctionTool.newBuilder()
                        .name("dashscope$document")
                        .description("文档理解")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {

                            final var model = GeneralAigcModel.newBuilder()
                                    .name("qwen-doc-turbo")
                                    .path("/api/v1/services/aigc/text-generation/generation")
                                    .uploadEnabled(true)
                                    .build();

                            final var input = new HashMap<String,Object>(){{
                                put("messages", new ArrayList<>(){{
                                    add(new HashMap<>(){{
                                        put("role", "user");
                                        put("content", new ArrayList<>(){{
                                            add(new HashMap<>(){{
                                                put("type", "text");
                                                put("text", spec.prompt());
                                            }});
                                            add(new HashMap<>(){{
                                                put("type", "doc_url");
                                                put("file_parsing_strategy","auto");
                                                put("doc_url", new ArrayList<>(){{
                                                    addAll(spec.files());
                                                }});
                                            }});
                                        }});
                                    }});
                                }});
                            }};

                            final var request = AigcRequest.newBuilder(model)
                                    .input(input)
                                    .build();

                            return caller.client().async(request)
                                    .thenApply(AigcResponse::output);
                        })
                        .build();
            }

            record Spec(

                    @JsonPropertyDescription("提示词")
                    @JsonProperty(value = "prompt", required = true)
                    String prompt,

                    @JsonPropertyDescription("文件URI列表")
                    @JsonProperty(value = "files", required = true)
                    List<URI> files

            ) {

            }

        }.get();
    }

    /**
     * @return 理解图片和视频工具
     */
    public static Tool vision() {
        return new Supplier<>() {

            @Override
            public Tool get() {
                return FunctionTool.newBuilder()
                        .name("dashscope$vision")
                        .description("图片和视频理解")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
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

            record Spec(

                    @JsonPropertyDescription("提示词")
                    @JsonProperty(value = "prompt", required = true)
                    String prompt,

                    @JsonPropertyDescription("图片URI列表")
                    @JsonProperty("images")
                    List<URI> images,

                    @JsonPropertyDescription("视频URI列表")
                    @JsonProperty("videos")
                    List<URI> videos

            ) {

                Message toUserMessage() {
                    final var contents = new ArrayList<Content>();
                    if (images != null) {
                        images.stream()
                                .map(Content::image)
                                .forEach(contents::add);
                    }
                    if (videos != null) {
                        videos.stream()
                                .map(Content::video)
                                .forEach(contents::add);
                    }
                    contents.add(Content.text(prompt));
                    return Message.user(contents);
                }

            }

        }.get();
    }

    /**
     * @return 图像编辑工具
     */
    public static Tool imageEdit() {
        return new Supplier<>() {

            @Override
            public Tool get() {
                return FunctionTool.newBuilder()
                        .name("dashscope$image_edit")
                        .description("图片编辑")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {

                            final var model = new ChatModel("qwen-image-edit-max", "/api/v1/services/aigc/multimodal-generation/generation");
                            final var request = AigcRequest.newBuilder(model)
                                    .input(ChatModel.Input.newBuilder()
                                            .building(builder -> {

                                                final var contents = new ArrayList<Content>();
                                                contents.add(Content.text(spec.prompt()));
                                                if (spec.images() != null) {
                                                    spec.images().stream()
                                                            .map(Content::image)
                                                            .forEach(contents::add);
                                                }
                                                builder.addMessage(Message.user(contents));

                                            })
                                            .build())
                                    .addParameter("prompt_extend", true)
                                    .building(builder -> {

                                        if (spec.number() != null) {
                                            builder.addParameter("n", spec.number());
                                        }
                                        if (spec.size() != null) {
                                            builder.addParameter("size", String.format("%d*%d", spec.size().width, spec.size().height));
                                        }
                                        if (spec.negative() != null) {
                                            builder.addParameter("negative_prompt", spec.negative());
                                        }

                                    })
                                    .build();

                            return caller.client().async(request)
                                    .thenApply(response -> response.output().best().message().contents());
                        })
                        .build();
            }

            record Spec(

                    @JsonPropertyDescription("提示词")
                    @JsonProperty(value = "prompt", required = true)
                    String prompt,

                    @JsonPropertyDescription("负面提示词")
                    @JsonProperty("negative")
                    String negative,

                    @JsonPropertyDescription("图片URI列表")
                    @JsonProperty("images")
                    List<URI> images,

                    @JsonPropertyDescription("生成图片大小")
                    @JsonProperty("size")
                    Size size,

                    @JsonPropertyDescription("生成图片数量")
                    @JsonProperty("number")
                    Integer number

            ) {

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

        }.get();
    }

    /**
     * @return 文生图工具
     */
    public static Tool t2i() {
        return new Supplier<>() {

            @Override
            public Tool get() {
                return FunctionTool.newBuilder()
                        .name("dashscope$text_to_image")
                        .description("文生图")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
                            final var client = caller.client();
                            final var request = AigcRequest.newBuilder(ChatModel.QWEN_IMAGE_MAX)
                                    .input(ChatModel.Input.newBuilder()
                                            .addMessage(Message.user(spec.prompt()))
                                            .uploadEnabled(true)
                                            .build())
                                    .addParameter("prompt_extend", true)
                                    .building(builder -> {
                                        if (spec.negative() != null) {
                                            builder.addParameter("negative_prompt", spec.negative());
                                        }
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

            record Spec(

                    @JsonPropertyDescription("提示词")
                    @JsonProperty(value = "prompt", required = true)
                    String prompt,

                    @JsonPropertyDescription("负面提示词")
                    @JsonProperty("negative")
                    String negative

            ) {

            }

        }.get();
    }

    /**
     * @return 文生视频工具
     */
    public static Tool t2v() {
        return new Supplier<>() {

            @Override
            public Tool get() {
                return FunctionTool.newBuilder()
                        .name("dashscope$text_to_video")
                        .description("文生视频")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {

                            final var model = GeneralAigcModel.newBuilder()
                                    .name("wan2.6-t2v")
                                    .path("/api/v1/services/aigc/video-generation/video-synthesis")
                                    .uploadEnabled(true)
                                    .build();

                            final var input = new HashMap<String, Object>();
                            input.put("prompt", spec.prompt());
                            if (spec.audio() != null) {
                                input.put("audio_url", spec.audio());
                            }

                            final var request = AigcRequest.newBuilder(model)
                                    .input(input)
                                    .building(builder -> {

                                        final var parameters = new Parameters();
                                        if (spec.duration() != null) {
                                            parameters.append("duration", spec.duration());
                                        }
                                        if (spec.size() != null) {
                                            parameters.append("size", spec.size());
                                        }
                                        if (spec.shotType() != null) {
                                            parameters.append("shot_type", spec.shotType());
                                        }

                                        builder.parameters(parameters);
                                    })
                                    .build();

                            return caller.client().task(request)
                                    .thenCompose(half ->
                                            half.waitingFor(Task.WaitStrategies.always(Duration.ofSeconds(1))));
                        })
                        .build();
            }

            record Spec(

                    @JsonProperty(value = "prompt", required = true)
                    String prompt,

                    @JsonPropertyDescription("音频URI")
                    @JsonProperty("audio")
                    URI audio,

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

        }.get();
    }

    /**
     * @return 文本转语音工具
     */
    public static Tool tts() {
        return new Supplier<>() {

            @Override
            public Tool get() {
                return FunctionTool.newBuilder()
                        .name("dashscope$text_to_speech")
                        .description("文本转音频")
                        .parameterType(Spec.class)
                        .<Spec>function((caller, spec) -> {
                            final var session = CosyVoiceSession.newBuilder()
                                    .model(CosyVoiceModel.COSYVOICE_V3_PLUS)
                                    .building(builder -> {

                                        if (spec.voice() != null) {
                                            builder.addParameter("voice", spec.voice());
                                        } else {
                                            builder.addParameter("voice", "longanyang");
                                        }

                                        if (spec.format() != null) {
                                            builder.addParameter(CosyVoiceParameterKeys.FORMAT, switch (spec.format()) {
                                                case PCM -> CosyVoiceParameterKeys.Format.PCM;
                                                case WAV -> CosyVoiceParameterKeys.Format.WAV;
                                                case MP3 -> CosyVoiceParameterKeys.Format.MP3;
                                            });
                                        }

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
                                            CompletableFuture.completedStage(null)
                                                    .thenCompose(unused -> cvEmitter.text(spec.text()))
                                                    .thenCompose(unused -> cvEmitter.closing());
                                        }

                                    })
                                    .thenCompose(Realtime.Connection::closeFuture)
                                    .thenApply(unused -> targetFile.toURI());
                        })
                        .build();
            }

            record Spec(

                    @JsonPropertyDescription("文本")
                    @JsonProperty(value = "text", required = true)
                    String text,

                    @JsonPropertyDescription("音色")
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

                    @JsonPropertyDescription("男声")
                    @JsonProperty("longanyang")
                    LONGANYANG,

                    @JsonPropertyDescription("女声")
                    @JsonProperty("longanhuan")
                    LONGANHUAN,

                    @JsonPropertyDescription("童声")
                    @JsonProperty("longhuhu_v3")
                    LONGHUHU

                }

            }

        }.get();
    }

    public static List<Tool> tools() {
        return List.of(
                document(),
                vision(),
                imageEdit(),
                t2i(),
                t2v(),
                tts()
        );
    }

}
