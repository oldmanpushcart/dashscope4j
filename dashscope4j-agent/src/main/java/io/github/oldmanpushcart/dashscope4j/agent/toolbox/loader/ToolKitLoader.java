package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader;

import io.github.oldmanpushcart.dashscope4j.agent.tool.ToolKit;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 * 工具集加载器
 * <p>
 * 通用的 ToolLoader 适配器，用于将 ToolKit 加载到 Toolbox 中。
 * 支持通过过滤器选择性加载工具。
 * </p>
 *
 * @since 4.0.0
 */
public class ToolKitLoader implements ToolLoader {

    /**
     * 工具集
     */
    private final ToolKit kit;

    /**
     * 工具过滤器（可选）
     */
    private final Predicate<FunctionTool> filter;

    /**
     * 构造工具集加载器
     *
     * @param builder 构建器
     */
    private ToolKitLoader(Builder builder) {
        Objects.requireNonNull(builder.kit, "kit must not be null!");
        this.kit = builder.kit;
        this.filter = builder.filter;
    }

    @Override
    public CompletionStage<Void> install(Toolbox toolbox) {

        // 并行等待所有注册操作完成
        final var stages = kit.tools().stream()
                .filter(FunctionTool.class::isInstance)
                .map(FunctionTool.class::cast)
                // 应用过滤器（如果设置了）
                .filter(tool -> filter == null || filter.test(tool))
                .map(tool -> toolbox.register(tool.meta().name(), tool))
                .toList();

        return CompletableFutureUtils.allOf(stages);
    }

    @Override
    public void close() {
        // 无资源需要关闭
    }

    /**
     * 创建 ToolKitLoader（向后兼容）
     *
     * @param kit 工具集
     * @return ToolKitLoader 实例
     */
    public static ToolKitLoader of(ToolKit kit) {
        return newBuilder().kit(kit).build();
    }

    /**
     * 创建 ToolKitLoader（向后兼容）
     *
     * @param tools 工具列表
     * @return ToolKitLoader 实例
     */
    public static ToolKitLoader of(Tool... tools) {
        return newBuilder().kit(() -> Arrays.asList(tools)).build();
    }

    /**
     * 创建新的构建器
     *
     * @return Builder 实例
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * ToolKitLoader 构建器
     * <p>
     * 使用 Builder 模式配置工具集加载器，支持设置过滤器。
     * </p>
     *
     * @since 4.0.0
     */
    public static class Builder implements Buildable<ToolKitLoader, Builder> {

        /**
         * 工具集（必需）
         */
        private ToolKit kit;

        /**
         * 工具过滤器（可选）
         */
        private Predicate<FunctionTool> filter;

        /**
         * 设置工具集
         *
         * @param kit 工具集
         * @return 当前构建器
         */
        public Builder kit(ToolKit kit) {
            this.kit = kit;
            return this;
        }

        /**
         * 设置工具过滤器
         * <p>
         * 过滤器用于选择性加载工具，只有返回 true 的工具才会被注册到工具箱。
         * </p>
         *
         * @param filter 工具过滤器，基于 FunctionTool 进行过滤
         * @return 当前构建器
         */
        public Builder filter(Predicate<FunctionTool> filter) {
            this.filter = filter;
            return this;
        }

        /**
         * 构建工具集加载器
         *
         * @return 新创建的 ToolKitLoader 实例
         */
        @Override
        public ToolKitLoader build() {
            return new ToolKitLoader(this);
        }

    }

}
