package io.github.oldmanpushcart.dashscope4j.agent.prompt;

import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;

/**
 * 提示语模板
 */
public class PromptTemplate {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private final String template;
    private final Map<String, Object> parameterMap;

    public PromptTemplate(String template) {
        this.template = template;
        this.parameterMap = new HashMap<>();
    }

    private PromptTemplate(Builder builder) {
        this.template = builder.template;
        this.parameterMap = Collections.unmodifiableMap(builder.parameterMap);
    }

    /**
     * 渲染模板
     *
     * @param parameterMap 参数集合
     * @return 提示词字符串
     */
    public String render(Map<String, Object> parameterMap) {
        final Map<String, Object> merged = new HashMap<>();
        merged.putAll(this.parameterMap);
        merged.putAll(parameterMap);
        return resolve(template, merged);
    }

    /**
     * 渲染模板
     *
     * @return 提示词字符串
     */
    public String render() {
        return render(emptyMap());
    }

    /**
     * 渲染模板并转换为指定类型
     *
     * @param parameterMap 参数集合
     * @param mapper       转换器
     * @param <T>          目标类型
     * @return 转换目标对象
     */
    public <T> T renderTo(Map<String, Object> parameterMap, Function<String, T> mapper) {
        return mapper.apply(render(parameterMap));
    }

    /**
     * 渲染模板并转换为指定类型
     *
     * @param mapper 转换器
     * @param <T>    目标类型
     * @return 转换目标对象
     */
    public <T> T renderTo(Function<String, T> mapper) {
        return mapper.apply(render());
    }

    @Override
    public String toString() {
        return render();
    }

    /**
     * 替换字符串中的占位符，支持转义字符（\${name}）。
     *
     * @param template     模板字符串
     * @param parameterMap 占位符替换值的映射
     * @return 替换后的字符串
     */
    private static String resolve(String template, Map<String, Object> parameterMap) {
        if (null == parameterMap || parameterMap.isEmpty()) {
            return template;
        }
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        final StringBuffer stringBuf = new StringBuffer();
        while (matcher.find()) {
            final boolean isEscaped = matcher.start() > 0 && template.charAt(matcher.start() - 1) == '\\';
            final String placeholder = matcher.group(1);
            final String replacement = isEscaped || !parameterMap.containsKey(placeholder)
                    ? String.format("${%s}", placeholder)
                    : String.valueOf(parameterMap.get(placeholder));
            matcher.appendReplacement(stringBuf, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(stringBuf);
        return stringBuf.toString();
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(PromptTemplate template) {
        return new Builder(template);
    }

    public static class Builder implements Buildable<PromptTemplate, Builder> {

        private String template;
        private final Map<String, Object> parameterMap = new HashMap<>();

        public Builder() {

        }

        public Builder(PromptTemplate template) {
            this.template = template.template;
            this.parameterMap.putAll(template.parameterMap);
        }

        public Builder template(String template) {
            this.template = template;
            return this;
        }

        /**
         * 添加参数
         *
         * @param name  参数名
         * @param value 参数值
         * @return this
         */
        public Builder parameter(String name, Object value) {
            parameterMap.put(name, value);
            return this;
        }

        /**
         * 延迟添加参数
         *
         * @param name   参数名
         * @param getter 延迟获取参数值函数
         * @return this
         */
        public Builder parameter(String name, Supplier<Object> getter) {
            parameterMap.put(name, new LazyGet(getter));
            return this;
        }

        @Override
        public PromptTemplate build() {
            return new PromptTemplate(this);
        }

    }

    /**
     * 延迟获取参数值
     */
    @Value
    @Accessors(fluent = true)
    private static class LazyGet {

        Supplier<Object> getter;

        @Override
        public String toString() {
            return String.valueOf(getter.get());
        }

    }

}
