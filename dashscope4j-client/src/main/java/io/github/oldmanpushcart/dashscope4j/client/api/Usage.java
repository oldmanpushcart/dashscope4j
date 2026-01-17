package io.github.oldmanpushcart.dashscope4j.client.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
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
            JsonNode node = p.getCodec().readTree(p);
            return buildUsage(node);
        }

        private Usage buildUsage(JsonNode node) {
            if (!node.isObject()) {
                throw new IllegalArgumentException("Root must be a JSON object");
            }

            List<Item> items = new ArrayList<>();
            Map<String, Usage> children = new LinkedHashMap<>(); // 保持顺序（可选）

            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode value = entry.getValue();

                if (value.isNumber() && value.isIntegralNumber()) {
                    // 叶子节点：整数 → Item
                    items.add(new Item(fieldName, value.intValue()));
                } else if (value.isObject()) {
                    // 嵌套对象 → child Usage
                    children.put(fieldName, buildUsage(value));
                } else if (value.isNumber()) {
                    // 非整数数字（如 double）→ 取整或报错
                    items.add(new Item(fieldName, value.intValue())); // 或抛异常
                } else if (value.isTextual()) {
                    // 字符串数字？按需处理
                    try {
                        int cost = Integer.parseInt(value.textValue());
                        items.add(new Item(fieldName, cost));
                    } catch (NumberFormatException e) {
                        // 忽略非数字字符串，或按业务处理
                    }
                }
                // 其他类型（boolean, array, null）默认忽略
            });

            return new Usage(items, children);

        }
    }

}
