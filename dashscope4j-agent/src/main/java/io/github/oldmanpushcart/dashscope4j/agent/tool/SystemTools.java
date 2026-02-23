package io.github.oldmanpushcart.dashscope4j.agent.tool;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.LocalDateTime.*;

public class SystemTools {

    private static Tool datetime() {
        final String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return FunctionTool.newBuilder()
                .name("system$datetime")
                .description("获取当前时间")
                .supplier(() -> formatter.format(now()))
                .build();
    }

    public static List<Tool> tools() {
        return List.of(
                datetime()
        );
    }

}
