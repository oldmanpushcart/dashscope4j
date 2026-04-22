package io.github.oldmanpushcart.dashscope4j.agent.tool.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolKit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.*;

/**
 * Shell 命令执行工具包
 * <p>
 * 提供在本地环境中执行 Shell 命令的能力（⚠️ 需谨慎使用）
 */
public class ShellToolKit implements ToolKit {

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

    private ShellToolKit(Builder builder) {
        this.timeout = builder.timeout;
        this.securityLevel = builder.securityLevel;
    }

    @Override
    public List<Tool> tools() {
        return List.of(createShellTool());
    }

    /**
     * 创建 shell$exec 工具
     */
    private FunctionTool createShellTool() {
        return FunctionTool.newBuilder()
                .name("shell$exec")
                .description("""
                        在本地环境中执行 Shell 命令或脚本（⚠️ 需谨慎使用）。
                        
                        【使用场景】
                        - 执行系统管理任务（查看文件、进程等）
                        - 运行脚本或程序
                        - 获取系统状态信息
                        - 自动化运维任务
                        
                        【参数说明】
                        - command: 要执行的命令（必需），使用字符串数组格式
                          * Windows 示例：["cmd.exe", "/c", "dir"]
                          * Linux/Mac 示例：["bash", "-c", "ls -la"]
                          * 直接执行程序：["python", "--version"]
                        
                        【返回结果】
                        - output: 命令的标准输出和错误输出
                        - exit_code: 退出码（0 表示成功）
                        - is_success: 是否执行成功
                        - prompt: 失败时的处理建议
                        
                        【⚠️ 安全注意事项】
                        - ⛔ 禁止执行危险命令（如格式化磁盘、删除系统文件等）
                        - ⛔ 禁止执行可能危害系统安全的命令
                        - ⏱️ 命令执行有 %s 超时限制，防止长时间挂起
                        - 🔒 建议优先使用只读命令（查询类）
                        - 📝 生产环境使用时请确保有足够的权限控制
                        
                        【常见用法示例】
                        - Windows 查看目录：["cmd.exe", "/c", "dir C:\\\\Users"]
                        - Linux 查看进程：["ps", "aux"]
                        - 查看 Python 版本：["python", "--version"]
                        - Git 状态检查：["git", "status"]
                        """.formatted(timeout))
                .parameterType(CmdSpec.class)
                .<CmdSpec>function((caller, spec) -> {
                    try {
                        // 安全检查：验证命令是否在黑名单中
                        validateCommand(spec.command());

                        final var charset = detectTerminalCharset();
                        final var result = executeWithTimeout(spec.command(), charset);
                        return CompletableFuture.completedStage(result);
                    } catch (SecurityException ex) {
                        // 安全拦截的危险命令
                        final var result = new Result(
                                "⛔ 命令被拒绝：" + ex.getMessage(),
                                -1,
                                false  // Shell 执行失败
                        );
                        return CompletableFuture.completedStage(result);
                    } catch (IOException | InterruptedException ex) {
                        if (ex instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        final var result = new Result(
                                "命令执行失败：" + ex.getMessage(),
                                -1,
                                false  // Shell 执行失败
                        );
                        return CompletableFuture.completedStage(result);
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
                final var result = executeWithTimeout(List.of("cmd.exe", "/c", "chcp"), Charset.defaultCharset());
                if (!result.shellExecutionSuccess()) {
                    throw new IOException(result.toString());
                }

                // 根据活动代码页约定值推测当前终端字符集
                final var matcher = activeCodePagePattern.matcher(result.output);
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
     * 带超时控制的命令执行
     *
     * @param command 命令列表
     * @param charset 字符集
     * @return 执行结果
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    private Result executeWithTimeout(List<String> command, Charset charset) throws IOException, InterruptedException {
        final var process = new ProcessBuilder()
                .redirectErrorStream(true)
                .command(command)
                .start();

        // 启动输出读取线程
        final var outputFuture = CompletableFuture.supplyAsync(() -> {
            final var outputBuf = new StringBuilder();
            try (final var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuf.append(line).append('\n');
                }
            } catch (IOException e) {
                return "读取输出时出错：" + e.getMessage();
            }
            return outputBuf.toString();
        });

        // 等待进程完成（带超时）
        final var completed = process.waitFor(timeout.toMillis(), MILLISECONDS);

        if (!completed) {
            // 超时，销毁进程
            process.destroyForcibly();
            return new Result(
                    "⏱️ 命令执行超时（超过 " + timeout + "），已被强制终止",
                    -2,
                    false  // Shell 执行失败
            );
        }

        // 获取输出（已考虑超时）
        String output;
        try {
            output = outputFuture.get(timeout.toMillis(), MILLISECONDS);
        } catch (ExecutionException e) {
            output = "获取输出失败：" + e.getCause().getMessage();
        } catch (TimeoutException e) {
            output = "读取输出超时";
        }
        return new Result(output, process.exitValue(), true);  // Shell 执行成功
    }

    // ==================== 内部类 ====================

    /**
     * 命令执行规格
     */
    record CmdSpec(

            @JsonPropertyDescription("要执行的命令（字符串数组形式）")
            @JsonProperty(value = "command", required = true)
            List<String> command

    ) {
    }

    /**
     * 命令执行结果
     */
    record Result(

            @JsonProperty("output")
            String output,

            @JsonProperty("exit_code")
            int exitCode,

            @JsonProperty("shell_execution_success")
            boolean shellExecutionSuccess

    ) {
        /**
         * 命令是否执行成功(退出码为0)
         *
         * @return 命令是否成功
         */
        @JsonProperty("is_command_success")
        public boolean isCommandSuccess() {
            return exitCode == 0;
        }

        /**
         * 获取提示信息
         *
         * @param timeout 超时时间
         * @return 提示信息
         */
        public String prompt(Duration timeout) {
            // Shell 执行失败(超时、异常等)
            if (!shellExecutionSuccess) {
                return """
                        ⚠️ Shell 执行失败，命令未能正常运行：
                        
                        【可能原因】
                        - 命令执行超时(超过 %s)
                        - 命令被安全策略拦截
                        - 系统资源不足或 IO 错误
                        
                        【建议】
                        - 检查命令是否过于耗时
                        - 确认命令不在危险命令黑名单中
                        - 简化命令后重试
                        """.formatted(timeout);
            }

            // Shell 执行成功，但命令返回非零退出码
            if (exitCode != 0) {
                return """
                        ⚠️ 命令执行失败(退出码: %d)，请检查以下可能的问题：
                        
                        【常见问题排查】
                        1. 命令语法是否正确
                           - Windows: 使用 cmd.exe /c 或 PowerShell -Command
                           - Linux/Mac: 使用 bash -c 或直接执行
                        
                        2. 命令是否存在
                           - 检查命令是否已安装
                           - 检查 PATH 环境变量配置
                        
                        3. 权限是否足够
                           - 是否需要管理员/root 权限
                           - 文件是否有执行权限
                        
                        4. 参数是否正确
                           - 路径是否存在
                           - 参数格式是否匹配操作系统
                        
                        【调试建议】
                        - 尝试简化命令，逐步排查
                        - 使用 echo 测试环境变量
                        - 查看完整错误信息定位问题
                        """.formatted(exitCode);
            }

            // 完全成功
            return "执行成功";
        }

        /**
         * 获取提示信息(使用默认超时时间)
         *
         * @return 提示信息
         */
        @JsonProperty("prompt")
        public String prompt() {
            return prompt(DEFAULT_TIMEOUT);
        }
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

    // ==================== Builder ====================

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<ShellToolKit, Builder> {
        private Duration timeout = DEFAULT_TIMEOUT;
        private SecurityLevel securityLevel = DEFAULT_SECURITY_LEVEL;

        /**
         * 设置命令执行超时时间
         *
         * @param timeout 超时时间（建议 1-300 秒）
         * @return this
         */
        public Builder timeout(Duration timeout) {
            if (timeout == null || timeout.isNegative() || timeout.isZero()) {
                throw new IllegalArgumentException("timeout must be positive");
            }
            if (timeout.getSeconds() > 300) {
                throw new IllegalArgumentException(
                        "timeout must not exceed 300 seconds, got: " + timeout.getSeconds() + "s"
                );
            }
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
        public ShellToolKit build() {
            return new ShellToolKit(this);
        }
    }

}
