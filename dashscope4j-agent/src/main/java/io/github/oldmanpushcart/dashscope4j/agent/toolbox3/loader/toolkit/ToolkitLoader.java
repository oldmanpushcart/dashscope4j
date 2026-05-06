package io.github.oldmanpushcart.dashscope4j.agent.toolbox3.loader.toolkit;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox3.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox3.loader.AbstractToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具集加载器
 * <p>
 * 手动管理的工具加载器，支持动态添加/移除 Toolkit 或 Tool。
 * 适用于需要主动控制工具生命周期的场景。
 * </p>
 */
public class ToolkitLoader extends AbstractToolLoader {

    private static final Logger logger = LoggerFactory.getLogger(ToolkitLoader.class);

    // 当前生效的 ToolUse 集合（按名称索引）
    private final Map<String, ToolUse> currentUses = new ConcurrentHashMap<>();

    public ToolkitLoader() {

    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/toolbox/loader/toolkit";
    }

    /**
     * 添加多个工具包
     *
     * @param mode      使用模式（FIXED/DYNAMIC）
     * @param toolkits  工具包列表
     * @return 当前实例，支持链式调用
     */
    public ToolkitLoader append(ToolUse.Mode mode, Toolkit... toolkits) {
        if (toolkits == null || toolkits.length == 0) {
            return this;
        }

        final var tools = new ArrayList<Tool>();
        for (var toolkit : toolkits) {
            if (toolkit != null) {
                tools.addAll(toolkit.tools());
            }
        }

        return addTools(mode, tools);
    }

    /**
     * 添加多个工具
     *
     * @param mode   使用模式（FIXED/DYNAMIC）
     * @param tools  工具列表
     * @return 当前实例，支持链式调用
     */
    public ToolkitLoader append(ToolUse.Mode mode, Tool... tools) {
        if (tools == null || tools.length == 0) {
            return this;
        }

        return addTools(mode, List.of(tools));
    }

    /**
     * 添加工具的通用实现
     *
     * @param mode  使用模式
     * @param tools 工具列表
     * @return 当前实例
     */
    private ToolkitLoader addTools(ToolUse.Mode mode, List<Tool> tools) {
        final var upserts = new ArrayList<ToolUse>();

        tools.forEach(tool -> {
            final var use = new ToolUse(mode, tool, this);
            final var oldUse = currentUses.put(tool.meta().name(), use);
            upserts.add(use);

            if (oldUse != null) {
                logger.debug("{} updated tool: {}", this, tool.meta().name());
            } else {
                logger.debug("{} added tool: {}", this, tool.meta().name());
            }
        });

        // 通知变更
        if (!upserts.isEmpty()) {
            notifyChanged(upserts, List.of());
        }

        return this;
    }

    /**
     * 移除指定名称的工具
     *
     * @param name 工具名称
     * @return 是否成功移除
     */
    public boolean remove(String name) {
        final var removed = currentUses.remove(name);
        if (removed != null) {
            logger.debug("{} removed tool: {}", this, name);
            notifyChanged(List.of(), List.of(name));
            return true;
        }
        return false;
    }

    @Override
    public List<ToolUse> loaded() {
        return List.copyOf(currentUses.values());
    }

    @Override
    public void close() {
        // 调用父类 close()，内部会检查 closeF 防重复执行
        super.close();

        // 清空缓存
        currentUses.clear();
    }

}
