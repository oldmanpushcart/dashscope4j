package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(ExecuteScriptTool.class);

    public static final String TOOL_NAME = "global$skill$execute_script";

    private final Map<String, Skill> skillsMap;
    private final Path tempDir;
    private final Duration timeout;

    public ExecuteScriptTool(Map<String, Skill> skillsMap, Path tempDir, Duration timeout) {
        this.skillsMap = skillsMap;
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
                        - script_path: 脚本相对路径，必须位于 scripts/ 目录下，如 "scripts/process.py"
                        - args: 脚本参数列表，如 ["--input", "data.csv", "--output", "result.json"]
                        - interpreter: 解释器，如 "python", "bash", "node" (可选，默认根据扩展名自动判断：.py->python, .sh->bash, .js->node)
                        
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
                    final var skill = skillsMap.get(spec.name());
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
        return skill.readScript(path)
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
