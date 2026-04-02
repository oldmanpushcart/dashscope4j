package io.github.oldmanpushcart.dashscope4j.agent.tool.loader.skill;

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

import static java.nio.file.StandardOpenOption.*;

/**
 * 全局工具：获取静态资源
 */
class GetAssertTool {

    public static final String TOOL_NAME = "global$skill$get_assert";

    private final Map<String, Skill> skills;
    private final Path tempDir;

    public GetAssertTool(Map<String, Skill> skills, Path tempDir) {
        this.skills = skills;
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
                        - assert_path: 资源文件相对路径
                        
                        【路径使用严格规范】
                        ⚠️ 重要：路径参数是**资源的唯一标识符**,必须原样复制：
                        - 从 SKILL.md、README.md 等文档中看到的路径，必须**逐字复制**到 assert_path 参数
                        - 不要修改、优化或"修正"路径格式（例如：不要去掉前缀，不要转换为绝对路径）
                        - 不要对路径进行 URL 编码/解码
                        - 即使路径看起来"奇怪"或"不正确",也要原样保留
                        
                        ❌ 错误示例：
                        - 修改路径的前缀部分
                        - 将相对路径改为绝对路径
                        
                        ✅ 正确示例：
                        - 文档中怎么写，参数就怎么填，保持完全一致
                        
                        【返回结果】
                        临时文件的 URI (file:///...)，可以直接用于文件读取或其他需要文件路径的场景。
                        临时文件会在 SkillToolLoader 关闭时自动清理。
                        
                        【示例】
                        ```json
                        {
                          "skill_name": "report-generator",
                          "assert_path": "assets/excel-template.xlsx"
                        }
                        ```
                        """)
                .parameterType(Spec.class)
                .<Spec>function((caller, spec) -> {
                    final var skill = skills.get(spec.name());
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
            final var outputC = FileChannel.open(tempFile, CREATE, TRUNCATE_EXISTING, WRITE);
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

            @JsonProperty("assert_path")
            @JsonPropertyDescription("资源文件相对路径，如 'assets/template.xlsx'")
            String path

    ) {
    }

}
