package io.github.oldmanpushcart.dashscope4j.agent.prompt;

import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 * 提示语模板
 */
public class PromptTemplate {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private final String template;
    private final Map<String, Object> variables;

    protected PromptTemplate(Builder builder) {
        this.template = builder.template;
        this.variables = unmodifiableMap(builder.variables);
    }

    /**
     * 渲染模板
     *
     * @param variables 变量表
     * @return 提示词字符串
     */
    public String render(Map<String, Object> variables) {
        final Map<String, Object> merged = new HashMap<>();
        merged.putAll(this.variables);
        merged.putAll(variables);
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
     * @param variables 变量表
     * @param mapper    转换器
     * @param <T>       目标类型
     * @return 转换目标对象
     */
    public <T> T renderTo(Map<String, Object> variables, Function<String, T> mapper) {
        return mapper.apply(render(variables));
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
     * @param template  模板字符串
     * @param variables 变量表
     * @return 替换后的字符串
     */
    private static String resolve(String template, Map<String, Object> variables) {
        if (null == variables || variables.isEmpty()) {
            return template;
        }
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        final StringBuilder stringBuf = new StringBuilder();
        while (matcher.find()) {
            final boolean isEscaped = matcher.start() > 0 && template.charAt(matcher.start() - 1) == '\\';
            final String placeholder = matcher.group(1);
            final String replacement = isEscaped || !variables.containsKey(placeholder)
                    ? "${%s}".formatted(placeholder)
                    : String.valueOf(variables.get(placeholder));
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
        private final Map<String, Object> variables = new HashMap<>();

        public Builder() {

        }

        public Builder(PromptTemplate pTemplate) {
            this.template = pTemplate.template;
            this.variables.putAll(pTemplate.variables);
        }

        /**
         * 设置模板
         *
         * @param template 模板
         * @return this
         */
        public Builder template(String template) {
            this.template = template;
            return this;
        }

        /**
         * 添加变量
         *
         * @param name  变量名
         * @param value 变量值
         * @return this
         */
        public Builder variable(String name, Object value) {
            variables.put(name, value);
            return this;
        }

        /**
         * 延迟添加变量
         *
         * @param name   变量名
         * @param getter 延迟获取变量值函数
         * @return this
         */
        public Builder variable(String name, Supplier<Object> getter) {
            variables.put(name, getter.get());
            return this;
        }

        @Override
        public PromptTemplate build() {
            return new PromptTemplate(this);
        }

    }

}
