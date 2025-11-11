package io.github.oldmanpushcart.dashscope4j.client.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 用量
 *
 * @param items 使用项集合
 */
@JsonDeserialize(using = Usage.UsageJsonDeserializer.class)
public record Usage(List<Item> items) {

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
        return new Usage(List.of());
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
        public Usage deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
            final var map = parser.getCodec().readValue(parser, new TypeReference<Map<String, Object>>() {
            });
            final var items = new ArrayList<Item>();
            map.forEach((k, v) -> {
                if (v instanceof Number num) {
                    items.add(new Item(k, num.intValue()));
                }
            });
            return new Usage(items);
        }

    }

}
