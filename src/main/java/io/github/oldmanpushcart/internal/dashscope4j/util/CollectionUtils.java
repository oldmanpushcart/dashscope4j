package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 集合工具类
 */
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
     * 更新模式
     */
    public enum UpdateMode {

        /**
         * 全量替换
         */
        REPLACE_ALL,

        /**
         * 追加到尾部
         */
        APPEND_TAIL

    }

    /**
     * 更新列表
     *
     * @param mode   更新模式
     * @param target 目标列表
     * @param source 源列表
     * @param <E>    元素类型
     * @param <T>    列表类型
     * @return 目标列表
     */
    public static <E, T extends Collection<E>> T updateList(UpdateMode mode, T target, Collection<E> source) {
        switch (mode) {
            case REPLACE_ALL -> {
                target.clear();
                target.addAll(source);
            }
            case APPEND_TAIL -> target.addAll(source);
        }
        return target;
    }

}
