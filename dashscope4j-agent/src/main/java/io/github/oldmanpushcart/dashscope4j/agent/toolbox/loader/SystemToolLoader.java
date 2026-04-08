package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import static java.time.LocalDateTime.now;

/**
 * 系统工具加载器
 * <p>
 * 提供系统级工具给 LLM 使用：
 * - datetime: 获取当前日期时间
 * - os: 获取操作系统信息
 * - env: 获取环境变量
 * - cmd: 执行系统命令（需谨慎使用）
 */
public class SystemToolLoader implements ToolLoader {

    public static final ToolLoader INSTANCE = new SystemToolLoader();

    /**
     * 命令执行超时时间（秒）
     */
    private static final int CMD_TIMEOUT_SECONDS = 30;

    /**
     * Windows 危险命令黑名单（部分）
     */
    private static final String[] WIN_DANGEROUS_COMMANDS = {
            "format", "diskpart", "del /s", "del /q", "rmdir /s",
            "shutdown", "taskkill /f", "net stop", "sc delete"
    };

    /**
     * Unix/Linux 危险命令黑名单（部分）
     */
    private static final String[] UNIX_DANGEROUS_COMMANDS = {
            "rm -rf /", "mkfs", "dd if=/dev/zero",
            "> /dev/sda", ":(){ :|:& };:", "chmod -R 777 /"
    };

    @Override
    public CompletionStage<Void> install(Toolbox toolbox) {
        List<FunctionTool> tools = List.of(
                os(),
                env(),
                cmd(),
                datetime()
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

    public static FunctionTool datetime() {
        final String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return FunctionTool.newBuilder()
                .name("system$datetime")
                .description("""
                        获取当前系统的日期和时间。
                        
                        【使用场景】
                        - 回答关于当前时间的问题
                        - 为日志添加时间戳
                        - 计算时间相关的推理
                        
                        【返回结果】
                        - ISO 8601 格式的日期时间字符串
                        - 示例：2024-03-24T20:30:15.123
                        
                        【注意事项】
                        - 无需参数
                        - 返回服务器本地时间
                        """)
                .supplier(() -> formatter.format(now()))
                .build();
    }

    public static FunctionTool os() {
        return FunctionTool.newBuilder()
                .name("system$os")
                .description("""
                        获取当前操作系统的详细信息。
                        
                        【使用场景】
                        - 判断运行平台（Windows/Linux/Mac）
                        - 获取系统架构（x86_64/aarch64）
                        - 了解 Java 运行时环境
                        
                        【返回结果】
                        - 包含多个系统属性的 Map：
                          * os.name: 操作系统名称
                          * os.version: 操作系统版本
                          * os.arch: 系统架构
                          * java.version: Java 版本
                          * user.dir: 工作目录
                          * 等等...
                        
                        【注意事项】
                        - 无需参数
                        - 返回完整的系统属性列表
                        """)
                .supplier(System::getProperties)
                .build();
    }

    public static FunctionTool env() {
        return FunctionTool.newBuilder()
                .name("system$env")
                .description("""
                        获取当前进程的环境变量列表。
                        
                        【使用场景】
                        - 查看配置的环境变量
                        - 调试环境问题
                        - 获取 PATH、HOME 等关键变量
                        
                        【返回结果】
                        - 包含所有环境变量的 Map
                        - Key: 环境变量名
                        - Value: 环境变量值
                        
                        【注意事项】
                        - 无需参数
                        - 敏感变量（如密码）也会被返回，请注意安全
                        - 不同操作系统环境变量名大小写敏感性不同
                        """)
                .supplier(() -> System.getenv())
                .build();
    }

    /**
     * 命令执行规格
     */
    record CmdSpec(

            @JsonPropertyDescription("要执行的命令（字符串数组形式）")
            @JsonProperty(value = "command", required = true)
            List<String> command

    ) {

    }

    public static FunctionTool cmd() {
        return FunctionTool.newBuilder()
                .name("system$cmd")
                .description("""
                        在本地环境中执行系统命令、脚本或程序（⚠️ 需谨慎使用）。
                        
                        【使用场景】
                        - 执行系统管理任务（查看文件、进程等）
                        - 运行脚本或程序
                        - 获取系统状态信息
                        - 自动化运维任务
                        
                        【参数说明】
                        - command: 命令及其参数（必需），使用字符串数组格式
                          * Windows 示例：["cmd.exe", "/c", "dir"]
                          * Linux 示例：["bash", "-c", "ls -la"]
                          * 直接执行程序：["python", "--version"]
                        
                        【返回结果】
                        - output: 命令的标准输出和错误输出
                        - exit_code: 退出码（0 表示成功）
                        - is_success: 是否执行成功
                        - prompt: 失败时的处理建议
                        
                        【⚠️ 安全注意事项】
                        - ⛔ 禁止执行危险命令（如格式化磁盘、删除系统文件等）
                        - ⛔ 禁止执行可能危害系统安全的命令
                        - ⏱️ 命令执行有 30 秒超时限制，防止长时间挂起
                        - 🔒 建议优先使用只读命令（查询类）
                        - 📝 生产环境使用时请确保有足够的权限控制
                        
                        【常见用法示例】
                        - Windows 查看目录：["cmd.exe", "/c", "dir C:\\Users"]
                        - Linux 查看进程：["ps", "aux"]
                        - 查看 Python 版本：["python", "--version"]
                        - Git 状态检查：["git", "status"]
                        """)
                .parameterType(CmdSpec.class)
                .<CmdSpec>function((caller, spec) -> {
                    try {
                        // 安全检查：验证命令是否在黑名单中
                        validateCommand(spec.command());

                        final var charset = detectTerminalCharset();
                        final var result = executeCmdWithTimeout(spec.command(), charset);
                        return CompletableFuture.completedStage(result);
                    } catch (SecurityException ex) {
                        // 安全拦截的危险命令
                        final var result = new Result(
                                "⛔ 命令被拒绝：" + ex.getMessage(),
                                -1
                        );
                        return CompletableFuture.completedStage(result);
                    } catch (IOException | InterruptedException ex) {
                        if (ex instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        final var result = new Result(
                                "命令执行失败：" + ex.getMessage(),
                                -1
                        );
                        return CompletableFuture.completedStage(result);
                    }
                })
                .build();
    }

    /**
     * 命令执行结果
     */
    record Result(

            @JsonProperty("output")
            String output,

            @JsonProperty("exit_code")
            int code

    ) {

        @JsonProperty("is_success")
        public boolean isSuccess() {
            return code == 0;
        }

        @JsonProperty("prompt")
        public String prompt() {

            if (isSuccess()) {
                return "执行成功";
            }

            return """
                    ⚠️ 命令执行失败，请检查以下可能的问题：
                    
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
                    """;
        }

    }

    /**
     * 验证命令安全性
     *
     * @param command 命令列表
     * @throws SecurityException 如果命令在黑名单中
     */
    private static void validateCommand(List<String> command) {
        if (command == null || command.isEmpty()) {
            throw new SecurityException("命令不能为空");
        }

        String cmdStr = String.join(" ", command).toLowerCase();

        // 检查 Windows 危险命令
        if (isWindows()) {
            for (String dangerous : WIN_DANGEROUS_COMMANDS) {
                if (cmdStr.contains(dangerous.toLowerCase())) {
                    throw new SecurityException("检测到危险命令：" + dangerous);
                }
            }
        }
        // 检查 Unix/Linux 危险命令
        else {
            for (String dangerous : UNIX_DANGEROUS_COMMANDS) {
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
    private static Charset detectTerminalCharset() {
        final Pattern activeCodePagePattern = Pattern.compile(".*?(\\d+).*?");

        // windows
        if (isWindows()) {
            try {

                // 执行 chcp 获取活动代码页
                final var result = executeCmdWithTimeout(List.of("cmd.exe", "/c", "chcp"), Charset.defaultCharset());
                if (!result.isSuccess()) {
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
    private static Result executeCmdWithTimeout(List<String> command, Charset charset) throws IOException, InterruptedException {
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
        boolean completed = process.waitFor(CMD_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

        if (!completed) {
            // 超时，销毁进程
            process.destroyForcibly();
            return new Result(
                    "⏱️ 命令执行超时（超过 " + CMD_TIMEOUT_SECONDS + " 秒），已被强制终止",
                    -2
            );
        }

        // 获取输出（已考虑超时）
        String output;
        try {
            output = outputFuture.get(CMD_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            output = "获取输出失败：" + e.getCause().getMessage();
        } catch (TimeoutException e) {
            output = "读取输出超时";
        }
        return new Result(output, process.exitValue());
    }

}
