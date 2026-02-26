package io.github.oldmanpushcart.dashscope4j.client.api;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 用量
 *
 * @param items 使用项集合
 */
@JsonDeserialize(using = Usage.UsageJsonDeserializer.class)
public record Usage(List<Item> items, Map<String, Usage> children) {

    /**
     * 计算总用量
     *
     * @return 总用量
     */
    public int total() {
        return total(v -> true);
    }

    /**
     * 计算总用量
     *
     * @param filter 过滤器；过滤调不需要参与计算的项目
     * @return 总用量
     */
    public int total(Predicate<Item> filter) {
        return items.stream()
                .filter(filter)
                .mapToInt(Item::cost)
                .sum();
    }

    /**
     * 空用量
     *
     * @return 空用量
     */
    public static Usage empty() {
        return new Usage(List.of(), Map.of());
    }

    /**
     * 项
     *
     * @param name 名称
     * @param cost 用量
     */
    public record Item(String name, int cost) {

    }


    /**
     * {@code JSON -> Usage}
     */
    static class UsageJsonDeserializer extends JsonDeserializer<Usage> {

        @Override
        public Usage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final var root = p.getCodec().readTree(p);

            // 必须是一个对象
            if (!root.isObject()) {
                throw new JsonParseException(p, "Expected an object");
            }

            return deserializeUsage((ObjectNode) root);
        }

        private Usage deserializeUsage(ObjectNode root) {

            final var items = new ArrayList<Item>();
            final var children = new LinkedHashMap<String, Usage>();

            root.fields().forEachRemaining(entry -> {

                final var name = entry.getKey();
                final var value = entry.getValue();

                // 数值
                if (value.isNumber()) {
                    items.add(new Item(name, value.asInt()));
                }

                // 嵌套 Usage
                else if (value.isObject()) {
                    children.put(name, deserializeUsage((ObjectNode) value));
                }

                // 其他类型忽略

            });

            return new Usage(items, children);

        }
    }

}
