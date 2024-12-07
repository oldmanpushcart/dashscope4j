package io.github.oldmanpushcart.dashscope4j;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Value
@Accessors(fluent = true)
public class Usage {

    List<Item> items;

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

    @JsonCreator
    static Usage of(Map<String, Object> map) {
        final List<Item> items = new ArrayList<>();
        map.forEach((k, v) -> {
            if (v instanceof Number) {
                final Number num = (Number)v;
                items.add(new Item(k, num.intValue()));
            }
        });
        return new Usage(items);
    }

    @Value
    public static class Item {
        String name;
        int cost;
    }

}
