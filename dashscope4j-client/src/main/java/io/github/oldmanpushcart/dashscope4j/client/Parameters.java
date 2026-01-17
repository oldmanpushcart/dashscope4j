package io.github.oldmanpushcart.dashscope4j.client;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 参数集
 */
@SuppressWarnings("ClassCanBeRecord")
public class Parameters {

    @JsonValue
    private final Map<String, Object> map;

    public Parameters() {
        this(new HashMap<>());
    }

    private Parameters(Map<String, Object> map) {
        this.map = map;
    }

    /**
     * 添加参数
     *
     * @param parameterKey 参数项
     * @param value        值
     * @param <T>          值类型
     * @return this
     */
    public <T> Parameters append(ParameterKey<T, ?> parameterKey, T value) {
        map.put(parameterKey.name(), parameterKey.convert(value));
        return this;
    }

    /**
     * 添加参数
     *
     * @param name  名称
     * @param value 值
     * @return this
     */
    public Parameters append(String name, Object value) {
        map.put(name, value);
        return this;
    }

    /**
     * 合并参数集
     *
     * @param parameters 合并目标
     * @return this
     */
    public Parameters merge(Parameters parameters) {
        map.putAll(parameters.map);
        return this;
    }

    /**
     * 判断参数项是否存在
     *
     * @param parameterKey 参数项
     * @param value        值
     * @param <T>          类型
     * @return this
     */
    public <T> boolean has(ParameterKey<T, ?> parameterKey, T value) {
        return map.containsKey(parameterKey.name()) && map.get(parameterKey.name()).equals(parameterKey.convert(value));
    }

    /**
     * 判断参数项是否存在
     *
     * @param name  名称
     * @param value 值
     * @return thi
     */
    public boolean has(String name, Object value) {
        return map.containsKey(name) && map.get(name).equals(value);
    }

    /**
     * @return 是否为空
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * 遍历参数项
     *
     * @param consumer 遍历器
     */
    public void forEach(BiConsumer<String, Object> consumer) {
        map.forEach(consumer);
    }

    public Map<String,Object> dump() {
        return new HashMap<>(map);
    }

    /**
     * @return 不可修改参数集
     */
    public Parameters unmodifiable() {
        return new Parameters(Collections.unmodifiableMap(map));
    }

    @SuppressWarnings("unchecked")
    public <R> R get(ParameterKey<?, R> key) {
        return (R)map.get(key.name());
    }

    @SuppressWarnings("unchecked")
    public <R> R get(String name) {
        return (R)map.get(name);
    }

    /**
     * 参数键
     *
     * @param <T> 值类型
     * @param <R> 转换后的值类型
     */
    public interface ParameterKey<T, R> {

        /**
         * @return 名称
         */
        String name();

        /**
         * @return 类型
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
     * 标准参参数键
     *
     * @param <T> 值类型
     * @param <R> 转换后的值类型
     */
    public static class StdParameterKey<T, R> implements ParameterKey<T, R> {

        private final String name;
        private final Class<R> type;
        private final Function<T, R> convert;

        /**
         * 构建标准参参数键
         *
         * @param name    名称
         * @param type    类型
         * @param convert 转换器
         */
        public StdParameterKey(String name, Class<R> type, Function<T, R> convert) {
            this.name = name;
            this.type = type;
            this.convert = convert;
        }

        @Override
        public R convert(T value) {
            return convert.apply(value);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Class<R> type() {
            return type;
        }

    }

    /**
     * 简单参参数键
     *
     * @param <T> 值类型
     */
    public static class SimpleParameterKey<T> extends StdParameterKey<T, T> {

        /**
         * 构建简单参数键
         *
         * @param name    名称
         * @param type    类型
         * @param convert 值转换器
         */
        public SimpleParameterKey(String name, Class<T> type, Function<T, T> convert) {
            super(name, type, convert);
        }

        /**
         * 构建简单参数键
         *
         * @param name 名称
         * @param type 类型
         */
        public SimpleParameterKey(String name, Class<T> type) {
            this(name, type, Function.identity());
        }

    }

}
