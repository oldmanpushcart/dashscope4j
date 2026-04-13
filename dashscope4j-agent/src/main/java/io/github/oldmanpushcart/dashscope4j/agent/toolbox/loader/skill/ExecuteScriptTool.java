package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 全局工具：执行脚本
 */
class ExecuteScriptTool {

    public static final String TOOL_NAME = "global$skill$execute_script";

    private final Map<String, Skill> skills;
    private final Path tempDir;
    private final Duration timeout;

    public ExecuteScriptTool(Map<String, Skill> skills, Path tempDir, Duration timeout) {
        this.skills = skills;
        this.tempDir = tempDir;
        this.timeout = timeout;
    }

    public FunctionTool toTool() {
        return FunctionTool.newBuilder()
                .name(TOOL_NAME)
                .description("""
                        执行 Skill 中的脚本。
                        
                        【使用场景】
                        需要运行 Skill 提供的自动化脚本、批处理程序、数据处理工具等时使用此工具。
                        例如：运行 Python 脚本进行数据分析、执行 Bash 脚本进行文件处理、调用 Node.js 程序进行代码转换等。
                        
                        【参数说明】
                        - skill_name: Skill 名称，如 "image-processor"
                        - script_path: 脚本相对路径
                        - args: 脚本参数列表，如 ["--input", "data.csv", "--output", "result.json"]
                        - interpreter: 解释器，如 "python", "bash", "node" (可选，默认根据扩展名自动判断：.py->python, .sh->bash, .js->node)
                        
                        【路径使用严格规范】
                        ⚠️ 重要：路径参数是**资源的唯一标识符**,必须原样复制：
                        - 从 SKILL.md、README.md 等文档中看到的路径，必须**逐字复制**到 script_path 参数
                        - 不要修改、优化或"修正"路径格式（例如：不要去掉前缀，不要转换为绝对路径）
                        - 不要对路径进行 URL 编码/解码
                        - 即使路径看起来"奇怪"或"不正确",也要原样保留
                        
                        ❌ 错误示例：
                        - 修改路径的前缀部分
                        - 将相对路径改为绝对路径
                        
                        ✅ 正确示例：
                        - 文档中怎么写，参数就怎么填，保持完全一致
                        
                        【返回结果】
                        脚本执行的标准输出内容。如果脚本执行失败，将抛出异常。
                        脚本执行超时时间为 30 秒，超时将被强制终止。
                        
                        【示例】
                        ```json
                        {
                          "skill_name": "data-analyzer",
                          "script_path": "scripts/analyze.py",
                          "args": ["--format", "json"],
                          "interpreter": "python"
                        }
                        ```
                        """)
                .parameterType(Spec.class)
                .<Spec>function((caller, spec) -> {
                    final var skill = skills.get(spec.name());
                    if (null == skill) {
                        throw new IllegalArgumentException("Unknow skill: %s".formatted(spec.name()));
                    }
                    return executeScript(skill, spec.path(), spec.args(), spec.interpreter());
                })
                .build();
    }

    /**
     * 执行脚本
     */
    private CompletionStage<String> executeScript(Skill skill, String path, List<String> args, String interpreter) {
        return skill.script(path)
                .thenCompose(scriptContent -> {
                    try {

                        // 确定解释器
                        final var finalInterpreter = (interpreter == null || interpreter.isBlank())
                                ? detectInterpreter(path)
                                : interpreter;

                        // 创建临时脚本文件
                        final var scriptName = Paths.get(path).getFileName().toString();
                        final var tempScript = tempDir.resolve(UUID.randomUUID() + "-" + scriptName);
                        Files.writeString(tempScript, scriptContent);

                        // 构建命令
                        final var command = new ArrayList<String>();
                        command.add(finalInterpreter);
                        command.add(tempScript.toString());
                        command.addAll(args != null ? args : List.of());

                        return CompletableFuture.supplyAsync(() -> {

                            Process process = null;

                            try {

                                // 执行脚本
                                process = new ProcessBuilder(command)
                                        .directory(tempDir.toFile())
                                        .redirectErrorStream(true)
                                        .start();

                                // 等待进程执行完成（带超时）
                                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                                    process.destroyForcibly();
                                    process = null;
                                    throw new TimeoutException("Script execution timeout (%s ms)".formatted(timeout.toMillis()));
                                }

                                // 读取输出
                                return new String(process.getInputStream().readAllBytes());

                            } catch (Exception e) {
                                throw new RuntimeException("Failed to execute script", e);
                            } finally {
                                if (process != null) {
                                    process.destroyForcibly();
                                }
                                try {
                                    Files.deleteIfExists(tempScript);
                                } catch (IOException e) {
                                    // ignored.
                                }
                            }
                        });
                    } catch (IOException e) {
                        return CompletableFuture.failedFuture(e);
                    }

                });
    }

    /**
     * 检测脚本解释器
     */
    private static String detectInterpreter(String scriptPath) {
        final var lcFilename = Paths.get(scriptPath).getFileName().toString().toLowerCase();

        if (lcFilename.endsWith(".py")) return "python";
        if (lcFilename.endsWith(".sh")) return "bash";
        if (lcFilename.endsWith(".js")) return "node";

        throw new IllegalArgumentException("Cannot determine interpreter for: " + lcFilename +
                ". Please specify interpreter explicitly.");
    }

    /**
     * 参数类
     */
    public record Spec(

            @JsonProperty("skill_name")
            @JsonPropertyDescription("Skill 名称")
            String name,

            @JsonProperty("script_path")
            @JsonPropertyDescription("脚本相对路径，如 'scripts/extract.py'")
            String path,

            @JsonProperty("args")
            @JsonPropertyDescription("脚本参数列表")
            List<String> args,

            @JsonProperty("interpreter")
            @JsonPropertyDescription("解释器，如 'python', 'bash' (可选，默认根据扩展名自动判断)")
            String interpreter

    ) {
    }

}
