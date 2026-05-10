package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool;

import java.util.List;
import java.util.Optional;

public interface ToolLookup {

    List<Tool> lookupAll();

    Optional<Tool> lookupByName(String name);

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
