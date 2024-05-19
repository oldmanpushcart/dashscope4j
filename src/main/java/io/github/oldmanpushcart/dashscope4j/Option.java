package io.github.oldmanpushcart.dashscope4j;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 选项
 */
public final class Option {

    @JsonValue
    private final Map<String, Object> map = new HashMap<>();

    /**
     * 构造选项
     */
    public Option() {

    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Option clone() {
        Option option = new Option();
        option.map.putAll(this.map);
        return option;
    }

    /**
     * 构造选项
     *
     * @param map 选项KV集合
     * @since 1.4.0
     */
    public Option(Map<String, Object> map) {
        this.map.putAll(map);
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

    /**
     * 删除选项
     *
     * @param opt 选项
     * @param <T> 值类型
     * @param <R> 转换后的值类型
     * @return this
     */
    public <T, R> Option remove(Opt<T, R> opt) {
        map.remove(opt.name());
        return this;
    }

    /**
     * 删除选项
     *
     * @param name 选项名称
     * @return 选项值
     */
    public Object remove(String name) {
        return map.remove(name);
    }

    /**
     * 判断是否有指定选项
     *
     * @param opt 选项
     * @return TRUE | FALSE
     */
    public <T, R> boolean has(Opt<T, R> opt) {
        return map.containsKey(opt.name());
    }

    /**
     * 判断是否有指定选项
     *
     * @param name 选项名称
     * @return TRUE | FALSE
     */
    public boolean has(String name) {
        return map.containsKey(name);
    }

    /**
     * 判断是否有指定选项
     *
     * @param opt    选项
     * @param expect 期望值
     * @param <T>    值类型
     * @param <R>    转换后的值类型
     * @return TRUE | FALSE
     */
    public <T, R> boolean has(Opt<T, R> opt, Object expect) {
        return map.containsKey(opt.name()) && Objects.equals(expect, map.get(opt.name()));
    }

    /**
     * 判断是否有指定选项
     *
     * @param name   选项名称
     * @param expect 期望值
     * @return TRUE | FALSE
     */
    public boolean has(String name, Object expect) {
        return map.containsKey(name) && Objects.equals(expect, map.get(name));
    }

    /**
     * 获取选项
     *
     * @param opt 选项
     * @return 选项值
     */
    public <T, R> Object get(Opt<T, R> opt) {
        return map.get(opt.name());
    }

    /**
     * 获取选项
     *
     * @param name 选项名称
     * @return 选项值
     */
    public Object get(String name) {
        return map.get(name);
    }

    /**
     * 导出选项为KV集合
     *
     * @return 选项KV集合
     */
    public Map<String, Object> export() {
        return Collections.unmodifiableMap(map);
    }

    /**
     * 选项是否为空
     *
     * @return TRUE | FALSE
     */
    public boolean isEmpty() {
        return map.isEmpty();
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
     * 标准项目
     *
     * @param name    选项名称
     * @param type    选项类型
     * @param convert 转换函数
     * @param <T>     值类型
     * @param <R>     转换后的值类型
     */
    public record StdOpt<T, R>(String name, Class<R> type, Function<T, R> convert) implements Opt<T, R> {

        @Override
        public R convert(T value) {
            return convert.apply(value);
        }

    }

    /**
     * 简单项目
     *
     * @param name    选项名称
     * @param type    选项类型
     * @param convert 转换函数
     * @param <T>     值类型
     */
    public record SimpleOpt<T>(String name, Class<T> type, Function<T, T> convert) implements Opt<T, T> {

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
