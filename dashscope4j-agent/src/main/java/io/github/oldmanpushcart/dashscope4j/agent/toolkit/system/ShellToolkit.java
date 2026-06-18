package io.github.oldmanpushcart.dashscope4j.agent.toolkit.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolExecutionException;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Shell 命令执行工具包
 * <p>
 * 提供在本地环境中执行 Shell 命令的能力（⚠️ 需谨慎使用）
 */
public class ShellToolkit implements Toolkit {

    /**
     * 默认命令执行超时时间
     */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 默认安全等级：STRICT（严格）
     */
    private static final SecurityLevel DEFAULT_SECURITY_LEVEL = SecurityLevel.STRICT;

    /**
     * 命令执行超时时间
     */
    private final Duration timeout;

    /**
     * 安全等级
     */
    private final SecurityLevel securityLevel;

    private ShellToolkit(Builder builder) {
        Objects.requireNonNull(builder.timeout, "timeout must not be null!");
        CheckUtils.require(builder.timeout, t -> !t.isNegative() && !t.isZero(), "timeout must be positive!");
        Objects.requireNonNull(builder.securityLevel, "securityLevel must not be null!");
        this.timeout = builder.timeout;
        this.securityLevel = builder.securityLevel;
    }

    @Override
    public List<Tool> tools() {
        return List.of(shell());
    }

    // ==================== Builder ====================

    public static ShellToolkit create() {
        return newBuilder().build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<ShellToolkit, Builder> {
        private Duration timeout = DEFAULT_TIMEOUT;
        private SecurityLevel securityLevel = DEFAULT_SECURITY_LEVEL;

        /**
         * 设置命令执行超时时间
         *
         * @param timeout 超时时间（建议 1-300 秒）
         * @return this
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * 设置安全等级
         *
         * @param securityLevel 安全等级
         * @return this
         */
        public Builder securityLevel(SecurityLevel securityLevel) {
            this.securityLevel = securityLevel;
            return this;
        }

        @Override
        public ShellToolkit build() {
            return new ShellToolkit(this);
        }
    }

    /**
     * 创建 shell$exec 工具
     */
    private FunctionTool shell() {
        return FunctionTool.newBuilder()
                .name("shell$exec")
                .description("""
                        在本地环境执行 Shell 命令或脚本。调用前，模型需自行根据当前操作系统类型，将命令转换为目标系统支持的语法。
                        - 参数: command (字符串数组, 必需), timeout (整数, 可选)。
                        - 返回: output, exit_code, is_success, prompt。
                        - 限制: 严禁执行破坏性或危害系统安全的命令。 建议优先使用只读查询命令，并确保具备相应权限。
                        """)
                .parameterType(CmdSpec.class)
                .<CmdSpec>function((caller, spec) -> {
                    try {
                        // 安全检查：验证命令是否在黑名单中
                        validateCommand(spec.command());

                        final var charset = detectTerminalCharset();

                        final var process = new ProcessBuilder()
                                .redirectErrorStream(true)
                                .command(spec.command())
                                .start();

                        // 等待进程完成（带超时）
                        final var completed = process.waitFor(timeout.toMillis(), MILLISECONDS);

                        if (!completed) {
                            // 超时，销毁进程
                            process.destroyForcibly();
                            throw ToolExecutionException.callFailed(
                                    "shell$execute",
                                    String.format("Command execution timed out (exceeded %s), forcibly terminated", timeout),
                                    "Try simplifying the command or increasing the timeout."
                            );
                        }

                        // 同步读取输出
                        final var outputBuf = new StringBuilder();
                        try (final var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                outputBuf.append(line).append('\n');
                            }
                        }

                        final int exitCode = process.exitValue();
                        final String output = outputBuf.toString();

                        return Map.of(
                                "output", output,
                                "exit_code", exitCode,
                                "is_success", exitCode == 0,
                                "prompt", generatePrompt(exitCode, timeout)
                        );

                    } catch (IOException | InterruptedException ex) {
                        if (ex instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw ToolExecutionException.callFailed(
                                "shell$execute",
                                "Command execution failed: " + ex.getMessage(),
                                "Check the command syntax and ensure the program exists.",
                                ex
                        );
                    }
                })
                .build();
    }

    /**
     * 验证命令安全性
     *
     * @param command 命令列表
     * @throws SecurityException 如果命令在黑名单中
     */
    private void validateCommand(List<String> command) {
        if (command == null || command.isEmpty()) {
            throw new SecurityException("命令不能为空");
        }

        // 根据安全等级决定是否进行验证
        if (securityLevel == SecurityLevel.NONE) {
            return;
        }

        String cmdStr = String.join(" ", command).toLowerCase();

        // 检查 Windows 危险命令
        if (isWindows()) {
            for (String dangerous : WinDangerousCommands.ALL) {
                if (cmdStr.contains(dangerous.toLowerCase())) {
                    throw new SecurityException("检测到危险命令：" + dangerous);
                }
            }
        }
        // 检查 Unix/Linux 危险命令
        else {
            for (String dangerous : UnixDangerousCommands.ALL) {
                if (cmdStr.contains(dangerous.toLowerCase())) {
                    throw new SecurityException("检测到危险命令：" + dangerous);
                }
            }
        }
    }

    /**
     * 判断是否为 Windows 系统
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * 检测终端字符集
     */
    private Charset detectTerminalCharset() {
        final Pattern activeCodePagePattern = Pattern.compile(".*?(\\d+).*?");

        // windows
        if (isWindows()) {
            try {
                // 执行 chcp 获取活动代码页
                final var process = new ProcessBuilder()
                        .redirectErrorStream(true)
                        .command("cmd.exe", "/c", "chcp")
                        .start();

                // 等待进程完成（带超时）
                final var completed = process.waitFor(timeout.toMillis(), MILLISECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    return Charset.defaultCharset();
                }

                // 读取输出
                final var outputBuf = new StringBuilder();
                try (final var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputBuf.append(line).append('\n');
                    }
                }

                final String output = outputBuf.toString();
                final int exitCode = process.exitValue();

                if (exitCode != 0) {
                    return Charset.defaultCharset();
                }

                // 根据活动代码页约定值推测当前终端字符集
                final var matcher = activeCodePagePattern.matcher(output);
                if (matcher.find()) {
                    final var cp = Integer.parseInt(matcher.group(1));
                    return switch (cp) {
                        case 936 -> Charset.forName("GBK");      // 简体中文
                        case 950 -> Charset.forName("Big5");     // 繁体中文
                        case 932 -> Charset.forName("MS932");    // 日文
                        case 949 -> Charset.forName("MS949");    // 韩文
                        case 437 -> Charset.forName("IBM437");   // 英文
                        case 850 -> Charset.forName("Cp850");    // 西欧
                        case 1252 -> Charset.forName("Cp1252");  // Windows 西欧
                        default -> Charset.defaultCharset();
                    };
                } else {
                    return Charset.defaultCharset();
                }

            } catch (InterruptedException | IOException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return Charset.defaultCharset();
            }
        }

        // unix / linux / mac
        else {
            return Optional.ofNullable(System.getenv("LANG"))
                    .or(() -> Optional.ofNullable(System.getenv("LC_ALL")))

                    // 处理可能的修饰符，如 zh_CN.UTF-8
                    .map(encoding -> {
                        if (encoding.contains(".")) {
                            return encoding.substring(encoding.indexOf('.') + 1).trim();
                        } else {
                            return encoding;
                        }
                    })

                    // 处理可能的修饰符，如 UTF-8@euro
                    .map(encoding -> {
                        if (encoding.contains("@")) {
                            return encoding.substring(0, encoding.indexOf('@'));
                        } else {
                            return encoding;
                        }
                    })

                    // 转换为 charset
                    .map(encoding -> {
                        try {
                            return Charset.forName(encoding);
                        } catch (Throwable ex) {
                            return Charset.defaultCharset();
                        }
                    })

                    // 其他情况直接返回默认字符
                    .orElse(Charset.defaultCharset());
        }
    }

    /**
     * 生成提示信息
     *
     * @param exitCode 退出码
     * @param timeout  超时时间
     * @return 提示信息
     */
    private String generatePrompt(int exitCode, Duration timeout) {
        // 命令返回非零退出码
        if (exitCode != 0) {
            return String.format(
                    """
                            ⚠️ Command execution failed (exit code: %d), please check:
                            
                            [Common Issues]
                            1. Command syntax is correct
                               - Windows: Use cmd.exe /c or PowerShell -Command
                               - Linux/Mac: Use bash -c or execute directly
                            
                            2. Command exists
                               - Check if the command is installed
                               - Check PATH environment variable
                            
                            3. Sufficient permissions
                               - Admin/root privileges required?
                               - File has execute permission?
                            
                            4. Parameters are correct
                               - Path exists?
                               - Parameter format matches OS?
                            
                            [Debugging Tips]
                            - Simplify the command and test step by step
                            - Use echo to test environment variables
                            - Check full error message for details
                            """,
                    exitCode
            );
        }

        // 完全成功
        return "Execution successful";
    }

    // ==================== 内部类 ====================

    /**
     * 命令执行规格
     */
    record CmdSpec(

            @JsonPropertyDescription("要执行的命令（字符串数组形式）")
            @JsonProperty(value = "command", required = true)
            List<String> command,

            @JsonPropertyDescription("命令执行超时时间（秒）")
            @JsonProperty(value = "timeout")
            Integer timeout

    ) {
    }


    /**
     * 安全等级枚举
     */
    public enum SecurityLevel {
        /**
         * 无安全检查（不推荐）
         */
        NONE,

        /**
         * 严格模式（默认）：阻止所有已知危险命令
         */
        STRICT
    }

    /**
     * Windows 危险命令
     */
    private static class WinDangerousCommands {
        static final String[] ALL = {
                "format", "diskpart", "del /s", "del /q", "rmdir /s",
                "shutdown", "taskkill /f", "net stop", "sc delete"
        };
    }

    /**
     * Unix/Linux 危险命令
     */
    private static class UnixDangerousCommands {
        static final String[] ALL = {
                "rm -rf /", "mkfs", "dd if=/dev/zero",
                "> /dev/sda", ":(){ :|:& };:", "chmod -R 777 /"
        };
    }

}
