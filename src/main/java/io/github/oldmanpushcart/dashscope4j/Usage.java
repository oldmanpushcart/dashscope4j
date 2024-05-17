package io.github.oldmanpushcart.dashscope4j;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 用量
 *
 * @param items 项目列表
 */
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
     * 创建空用量
     *
     * @return 空用量
     * @since 1.4.0
     */
    public static Usage empty() {
        return new Usage(List.of());
    }

    /**
     * 用量项目
     *
     * @param name 名称
     * @param cost 消耗
     */
    public record Item(String name, int cost) {

    }

    /**
     * 用量Json反序列化
     *
     * @param map Json Object Map
     * @return 用量
     */
    @JsonCreator
    static Usage of(Map<String, Object> map) {
        final var items = new ArrayList<Item>();
        map.forEach((k, v) -> {
            if (v instanceof Number num) {
                items.add(new Item(k, num.intValue()));
            }
        });
        return new Usage(items);
    }

}