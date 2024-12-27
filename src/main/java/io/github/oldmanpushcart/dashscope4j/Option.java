package io.github.oldmanpushcart.dashscope4j;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 选项
 */
@EqualsAndHashCode
@ToString
public final class Option {

    @JsonValue
    private final Map<String, Object> map;

    /**
     * 构造选项
     *
     * @param map 选项KV集合
     */
    public Option(Map<String, Object> map) {
        this.map = map;
    }

    /**
     * 构造选项
     */
    public Option() {
        this.map = new HashMap<>();
    }

    /**
     * 设置选项
     *
     * @param opt   选项
     * @param value 值
     * @param <T>   值类型
     * @param <R>   转换后的值类型
     * @return this
     */
    public <T, R> Option option(Opt<T, R> opt, T value) {
        map.put(opt.name(), opt.convert(value));
        return this;
    }

    /**
     * 设置选项
     *
     * @param name  选项名称
     * @param value 选项值
     * @return this
     */
    public Option option(String name, Object value) {
        map.put(name, value);
        return this;
    }

    public Option merge(Option option) {
        map.putAll(option.map);
        return this;
    }

    /**
     * @return 不可变选项
     */
    public Option unmodifiable() {
        return new Option(Collections.unmodifiableMap(map));
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Option clone() {
        return new Option(map);
    }

    /**
     * 选项项目
     *
     * @param <T> 值类型
     * @param <R> 转换后的值类型
     */
    public interface Opt<T, R> {

        /**
         * 选项名称
         *
         * @return 选项名称
         */
        String name();

        /**
         * 选项类型
         *
         * @return 选项类型
         */
        Class<R> type();

        /**
         * 转换
         *
         * @param value 值
         * @return 转换后的值
         */
        R convert(T value);

    }

    /**
     * 标准选项
     *
     * @param <T> 类型
     * @param <R> 转换后的类型
     */
    @lombok.Value
    @Accessors(fluent = true)
    public static class StdOpt<T, R> implements Opt<T, R> {

        String name;
        Class<R> type;
        Function<T, R> convert;

        @Override
        public R convert(T value) {
            return convert.apply(value);
        }

    }

    /**
     * 简单选项
     *
     * @param <T> 选项类型
     */
    @lombok.Value
    @Accessors(fluent = true)
    public static class SimpleOpt<T> implements Opt<T, T> {

        String name;
        Class<T> type;
        Function<T, T> convert;

        /**
         * 简单项目
         *
         * @param name    选项名称
         * @param type    选项类型
         * @param convert 转换函数
         */
        public SimpleOpt(String name, Class<T> type, Function<T, T> convert) {
            this.name = name;
            this.type = type;
            this.convert = convert;
        }

        /**
         * 简单项目
         *
         * @param name 选项名称
         * @param type 选项类型
         */
        public SimpleOpt(String name, Class<T> type) {
            this(name, type, Function.identity());
        }

        @Override
        public T convert(T value) {
            return convert.apply(value);
        }

    }

}
