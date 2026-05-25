package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 执行 Skill 中的脚本
 */
class ExecuteScriptFunction implements Function<ExecuteScriptFunction.Spec, String> {

    private final Map<String, Skill> skills;
    private final Duration timeout;

    public ExecuteScriptFunction(Map<String, Skill> skills, Duration timeout) {
        this.skills = skills;
        this.timeout = timeout;
    }

    @Override
    public String apply(Spec spec) {

        final var skill = skills.get(spec.name());
        if (null == skill) {
            throw new IllegalArgumentException("Skill %s not found!".formatted(spec.name()));
        }

        try {

            // 确定解释器：优先使用指定的，否则根据文件扩展名自动检测
            final var interpreter = CommonUtils.isNotBlankString(spec.interpreter())
                    ? spec.interpreter()
                    : detectInterpreter(spec.path());

            // 获取脚本的绝对路径
            final var scriptPath = skill.getAsset(spec.path()).toAbsolutePath().toString();

            // 构建完整的命令列表
            final var commands = new ArrayList<String>();

            // 如果有解释器，添加到命令开头
            if (interpreter != null) {
                commands.add(interpreter);
            }

            commands.add(scriptPath);

            if (spec.args() != null) {
                commands.addAll(spec.args());
            }

            return skill.executeScript(spec.path(), commands, timeout);
        } catch (Exception ex) {
            final var message = "Execute script error! skill=%s;script=%s;".formatted(
                    spec.name(),
                    spec.path()
            );
            throw new IllegalStateException(message, ex);
        }

    }

    /**
     * 根据脚本文件扩展名检测解释器
     *
     * @param scriptPath 脚本路径
     * @return 解释器名称，如果无法识别则返回 null
     */
    private static String detectInterpreter(String scriptPath) {
        final var lcFilename = Paths.get(scriptPath).getFileName().toString().toLowerCase();

        if (lcFilename.endsWith(".py")) return "python";
        if (lcFilename.endsWith(".sh")) return "bash";
        if (lcFilename.endsWith(".js")) return "node";
        if (lcFilename.endsWith(".pl")) return "perl";
        if (lcFilename.endsWith(".rb")) return "ruby";

        // 无法识别的文件类型，返回 null
        return null;
    }

    public Tool asTool() {
        return FunctionTool.newBuilder()
                .name("global$skill$execute_script")
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
                .function(this)
                .build();
    }

    /**
     * 参数类
     */
    public record Spec(

            @JsonProperty(value = "skill_name", required = true)
            @JsonPropertyDescription("Skill 名称")
            String name,

            @JsonProperty(value = "script_path", required = true)
            @JsonPropertyDescription("脚本相对路径，如 'scripts/extract.py'")
            String path,

            @JsonProperty(value = "args", required = true)
            @JsonPropertyDescription("脚本参数列表")
            List<String> args,

            @JsonProperty("interpreter")
            @JsonPropertyDescription("解释器，如 'python', 'bash' (可选，默认根据扩展名自动判断)")
            String interpreter

    ) {
    }

}
