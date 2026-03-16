package io.github.oldmanpushcart.dashscope4j.agent.tool.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.time.LocalDateTime.now;

public class SystemToolLoader implements ToolLoader {

    @Override
    public CompletionStage<Void> init(Updater updater) {
        return CompletableFuture.completedStage(null)
                .thenAccept(unused ->
                        updater.update(List.of(
                                datetime(),
                                os(),
                                env(),
                                cmd()
                        )));
    }

    public static Tool datetime() {
        final String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return FunctionTool.newBuilder()
                .name("system$datetime")
                .description("获取当前时间")
                .supplier(() -> formatter.format(now()))
                .build();
    }

    public static Tool os() {
        return FunctionTool.newBuilder()
                .name("system$os")
                .description("获取当前操作系统信息")
                .supplier(System::getProperties)
                .build();
    }

    public static Tool env() {
        return FunctionTool.newBuilder()
                .name("system$env")
                .description("获取当前环境信息")
                .supplier(() -> System.getenv())
                .build();
    }

    public static Tool cmd() {
        return new Supplier<Tool>() {

            @Override
            public Tool get() {
                return FunctionTool.newBuilder()
                        .name("system$cmd")
                        .description("执行命令")
                        .parameterType(Spec.class)
                        .<Spec>function(new BiFunction<>() {

                            private static final Pattern ACTIVE_CODE_PAGE_PATTERN = Pattern.compile(".*?(\\d+).*?");
                            private final Logger logger = LoggerFactory.getLogger(SystemToolLoader.class);

                            @Override
                            public Object apply(Tool.Caller caller, Spec spec) {

                                try {
                                    final var charset = detectTerminalCharset();
                                    final var result = executeCmd(spec.command(), charset);
                                    return CompletableFuture.completedStage(result);
                                } catch (IOException | InterruptedException ex) {
                                    if (ex instanceof InterruptedException) {
                                        Thread.currentThread().interrupt();
                                    }
                                    logger.warn("dashscope4j-agent://tool/{} execute occur error!", "system$cmd", ex);
                                    final var result = new Result(ex.getMessage(), -1);
                                    return CompletableFuture.completedStage(result);
                                }

                            }

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
                                            执行失败，你需要考虑以下情况解决。
                                            1. 命令是否匹配操作系统
                                            2. 命令是否匹配SHELL，重点要区分CMD、PowerShell、Bash、Zsh等
                                            3. 检查命令是否存在，如果不存在需要更换对等的其他命令。
                                            """;
                                }

                            }

                            private static Result executeCmd(List<String> command, Charset charset) throws IOException, InterruptedException {
                                final var process = new ProcessBuilder()
                                        .redirectErrorStream(true)
                                        .command(command)
                                        .start();
                                final var outputBuf = new StringBuilder();
                                try (final var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        outputBuf.append(line).append('\n');
                                    }
                                }
                                return new Result(outputBuf.toString(), process.waitFor());
                            }

                            private static Charset detectTerminalCharset() {

                                // windows
                                if (isWin()) {
                                    try {

                                        // 执行 chcp 获取活动代码页
                                        final var result = executeCmd(List.of("cmd.exe", "/c", "chcp"), Charset.defaultCharset());
                                        if (!result.isSuccess()) {
                                            throw new IOException(result.toString());
                                        }

                                        // 根据活动代码页约定值推测当前终端字符集
                                        final var matcher = ACTIVE_CODE_PAGE_PATTERN.matcher(result.output);
                                        if (matcher.find()) {
                                            final var cp = Integer.parseInt(matcher.group(1));
                                            return switch (cp) {
                                                case 936 -> Charset.forName("GBK");
                                                case 950 -> Charset.forName("Big5");
                                                case 932 -> Charset.forName("MS932");
                                                case 949 -> Charset.forName("MS949");
                                                case 437 -> Charset.forName("IBM437");
                                                case 850 -> Charset.forName("Cp850");
                                                case 1252 -> Charset.forName("Cp1252");
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

                            private static boolean isWin() {
                                return System.getProperty("os.name").toLowerCase().contains("win");
                            }

                        })
                        .build();
            }

            record Spec(

                    @JsonPropertyDescription("命令")
                    @JsonProperty(value = "command", required = true)
                    List<String> command

            ) {

            }

        }.get();

    }


}
