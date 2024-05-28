package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class CollectionUtils {

    /**
     * 列表元素映射
     *
     * @param source 源列表
     * @param mapper 映射器
     * @param <T>    源类型
     * @param <U>    目标类型
     * @return 映射后的列表
     */
    public static <T, U> List<U> mapTo(List<T> source, Function<T, U> mapper) {
        return source.stream()
                .map(mapper)
                .toList();
    }

    /**
     * 是否为非空集合
     *
     * @param collection 集合
     * @return TRUE | FALSE
     */
    public static boolean isNotEmptyCollection(Collection<?> collection) {
        return Objects.nonNull(collection) && !collection.isEmpty();
    }

    /**
     * 更新列表
     *
     * @param isAppend 是否追加
     * @param target   目标列表
     * @param source   源列表
     * @param <T>      对象类型
     */
    public static <T> void updateList(boolean isAppend, List<? super T> target, Collection<? extends T> source) {
        if (!isAppend) {
            target.clear();
        }
        if (isNotEmptyCollection(source)) {
            target.addAll(source);
        }
    }
}
