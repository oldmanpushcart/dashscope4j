package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader;

import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolKit;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;

/**
 * 工具集加载器
 * <p>
 * 通用的 ToolLoader 适配器，用于将 ToolKit 加载到 Toolbox 中。
 * </p>
 */
public class ToolKitLoader implements ToolLoader {

    private final ToolKit kit;

    private ToolKitLoader(ToolKit kit) {
        this.kit = kit;
    }

    @Override
    public CompletionStage<Void> install(Toolbox toolbox) {

        // 并行等待所有注册操作完成
        final var stages = kit.tools().stream()
                .filter(FunctionTool.class::isInstance)
                .map(FunctionTool.class::cast)
                .map(tool -> toolbox.register(tool.meta().name(), tool))
                .toList();

        return CompletableFutureUtils.allOf(stages);
    }

    @Override
    public void close() {
        // 无资源需要关闭
    }

    /**
     * 创建 ToolKitLoader
     *
     * @param kit 工具集
     * @return ToolKitLoader 实例
     */
    public static ToolKitLoader of(ToolKit kit) {
        return new ToolKitLoader(kit);
    }

    /**
     * 创建 ToolKitLoader
     *
     * @param tools 工具列表
     * @return ToolKitLoader 实例
     */
    public static ToolKitLoader of(Tool... tools) {
        return new ToolKitLoader(() -> Arrays.asList(tools));
    }

}
