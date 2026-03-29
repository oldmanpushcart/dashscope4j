package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * 全局工具：获取静态资源
 */
class GetAssertTool {

    public static final String TOOL_NAME = "global$skill$get_assert";

    private final Map<String, Skill> skillsMap;
    private final Path tempDir;

    public GetAssertTool(Map<String, Skill> skillsMap, Path tempDir) {
        this.skillsMap = skillsMap;
        this.tempDir = tempDir;
    }

    public FunctionTool toTool() {
        return FunctionTool.newBuilder()
                .name(TOOL_NAME)
                .description("""
                        获取 Skill 中的静态资源 URI。
                        
                        【使用场景】
                        需要使用 Skill 提供的模板文件、图片、数据文件等二进制资源时使用此工具。
                        例如：获取 Excel 模板用于生成报表、获取图片用于图像处理、获取配置文件用于程序运行等。
                        
                        【参数说明】
                        - skill_name: Skill 名称，如 "data-processor"
                        - resource_path: 资源文件相对路径，必须位于 assets/ 目录下，如 "assets/template.xlsx"
                        
                        【返回结果】
                        临时文件的 URI (file:///...)，可以直接用于文件读取或其他需要文件路径的场景。
                        临时文件会在 SkillToolLoader 关闭时自动清理。
                        
                        【示例】
                        ```json
                        {
                          "skill_name": "report-generator",
                          "resource_path": "assets/excel-template.xlsx"
                        }
                        ```
                        """)
                .parameterType(Spec.class)
                .<Spec>function((caller, spec) -> {
                    final var skill = skillsMap.get(spec.name());
                    if (null == skill) {
                        throw new IllegalArgumentException("Unknow skill: %s".formatted(spec.name()));
                    }
                    return saveAssertToTempFile(skill, spec.path(), tempDir);
                })
                .build();
    }

    /**
     * 保存静态资源到临时文件
     */
    private static CompletionStage<File> saveAssertToTempFile(Skill skill, String resourcePath, Path tempDir) {

        final var saveF = new CompletableFuture<File>();
        final var filename = Paths.get(resourcePath).getFileName().toString();
        final var uniqueId = UUID.randomUUID().toString().substring(0, 8);
        final var tempFile = tempDir.resolve(uniqueId + "-" + filename);

        try {

            //noinspection resource
            final var outputC = FileChannel.open(tempFile, CREATE, TRUNCATE_EXISTING);
            skill.readAssert(resourcePath, new Skill.ReadHandler() {

                @Override
                public void onRead(ByteBuffer buffer) {
                    try {
                        while (buffer.hasRemaining()) {
                            final var written = outputC.write(buffer);
                            if (written == 0) {
                                break;
                            }
                        }
                    } catch (IOException ex) {
                        saveF.completeExceptionally(ex);
                    }
                }

                @Override
                public void onFailure(IOException ex) {
                    saveF.completeExceptionally(ex);
                    IOUtils.closeQuietly(outputC);
                }

                @Override
                public void onCompleted() {
                    IOUtils.closeQuietly(outputC);
                    saveF.complete(tempFile.toFile());
                }

            });

        } catch (Throwable t) {
            saveF.completeExceptionally(t);
        }

        return saveF;
    }

    /**
     * 参数类
     */
    record Spec(

            @JsonProperty("skill_name")
            @JsonPropertyDescription("Skill 名称")
            String name,

            @JsonProperty("resource_path")
            @JsonPropertyDescription("资源文件相对路径，如 'assets/template.xlsx'")
            String path

    ) {
    }

}
