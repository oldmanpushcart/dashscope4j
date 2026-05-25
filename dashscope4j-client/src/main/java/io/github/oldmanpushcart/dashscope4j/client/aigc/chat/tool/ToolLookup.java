package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool;

import java.util.List;
import java.util.Optional;

/**
 * 工具查找器，用于查询和管理可用的工具
 */
public interface ToolLookup {

    /**
     * 查找所有可用工具
     *
     * @return 工具列表
     */
    List<Tool> lookupAll();

    /**
     * 根据名称查找工具
     *
     * @param name 工具名称
     * @return 找到的工具，未找到时返回空Optional
     */
    Optional<Tool> lookupByName(String name);

    /**
     * 组合多个工具查找器
     *
     * @param lookups 工具查找器列表
     * @return 组合后的工具查找器
     */
    static ToolLookup group(List<ToolLookup> lookups) {
        return new ToolLookup() {

            @Override
            public List<Tool> lookupAll() {
                return lookups.stream()
                        .flatMap(lookup -> lookup.lookupAll().stream())
                        .toList();
            }

            @Override
            public Optional<Tool> lookupByName(String name) {
                for (final var lookup : lookups) {
                    final var toolOpt = lookup.lookupByName(name);
                    if (toolOpt.isPresent()) {
                        return toolOpt;
                    }
                }
                return Optional.empty();
            }
        };
    }

    /**
     * 基于工具列表创建工具查找器
     *
     * @param tools 工具列表
     * @return 工具查找器
     */
    static ToolLookup tools(List<Tool> tools) {
        return new ToolLookup() {

            @Override
            public List<Tool> lookupAll() {
                return tools;
            }

            @Override
            public Optional<Tool> lookupByName(String name) {
                for (final var tool : tools) {
                    if (tool.meta().name().equals(name)) {
                        return Optional.of(tool);
                    }
                }
                return Optional.empty();
            }
        };
    }

    /**
     * 基于单个工具创建工具查找器
     *
     * @param tool 工具对象
     * @return 工具查找器
     */
    static ToolLookup single(Tool tool) {
        return new ToolLookup() {

            @Override
            public List<Tool> lookupAll() {
                return List.of(tool);
            }

            @Override
            public Optional<Tool> lookupByName(String name) {
                if (tool.meta().name().equals(name)) {
                    return Optional.of(tool);
                }
                return Optional.empty();
            }
        };
    }

}
