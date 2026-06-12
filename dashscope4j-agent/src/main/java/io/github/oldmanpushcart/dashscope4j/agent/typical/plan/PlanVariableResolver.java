package io.github.oldmanpushcart.dashscope4j.agent.typical.plan;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlanVariableResolver {

    // 匹配 ${...} 的正则表达式
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * 解析 Step 的 arguments 中的变量
     * @param arguments 原始的参数 Map
     * @param plan      当前的 Plan 对象（作为全局上下文）
     * @return 解析后的新 Map
     */
    public static Map<String, Object> resolve(Map<String, Object> arguments, Plan plan) {
        if (arguments == null || plan == null) return arguments;

        Map<String, Object> resolvedArgs = new HashMap<>();
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            resolvedArgs.put(entry.getKey(), resolveValue(entry.getValue(), plan));
        }
        return resolvedArgs;
    }

    /**
     * 递归解析单个值
     */
    private static Object resolveValue(Object value, Plan plan) {
        if (value instanceof String) {
            return resolveString((String) value, plan);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapValue = (Map<String, Object>) value;
            return resolve(mapValue, plan);
        } else if (value instanceof List<?> listValue) {
            List<Object> resolvedList = new ArrayList<>();
            for (Object item : listValue) {
                resolvedList.add(resolveValue(item, plan));
            }
            return resolvedList;
        }
        return value;
    }

    /**
     * 解析字符串中的 ${...} 变量
     */
    private static Object resolveString(String str, Plan plan) {
        Matcher matcher = VARIABLE_PATTERN.matcher(str);
        StringBuilder sb = new StringBuilder();
        boolean hasMatch = false;

        while (matcher.find()) {
            hasMatch = true;
            String expression = matcher.group(1).trim();
            Object resolvedObj = evaluateExpression(expression, plan);
            String replacement = (resolvedObj != null) ? resolvedObj.toString() : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        if (!hasMatch) return str;
        matcher.appendTail(sb);

        // 优化：如果整个字符串就是一个变量，且解析出的结果不是字符串，则直接返回原始对象
        if (str.trim().matches("^\\$\\{[^}]+}$")) {
            String expression = str.trim().substring(2, str.length() - 1).trim();
            Object originalObj = evaluateExpression(expression, plan);
            if (originalObj != null && !(originalObj instanceof String)) {
                return originalObj;
            }
        }
        return sb.toString();
    }

    /**
     * 核心路径解析器：支持 plan.goal, step_1.result.keywords, step_1.result.list[0]
     */
    private static Object evaluateExpression(String expression, Plan plan) {
        // 1. 处理顶层 Plan 属性，如 ${plan.goal}
        if (expression.startsWith("plan.")) {
            String planField = expression.substring(5); // 截取 "plan." 之后的内容
            return getPlanField(plan, planField);
        }

        // 2. 处理 Step 属性，如 ${step_1.result.keywords}
        String[] paths = expression.split("\\.", 2); // 最多切分为2段：[step_id, 剩余路径]
        if (paths.length < 2) return null;

        String stepId = paths[0];
        String remainingPath = paths[1];

        // 在 Plan 的 steps 中找到对应的 Step
        Plan.Step targetStep = findStepById(plan, stepId);
        if (targetStep == null) return null;

        // 对 Step 的剩余路径进行解析
        return evaluateStepPath(targetStep, remainingPath);
    }

    /**
     * 解析 Step 内部的路径（支持 result, arguments, message 等）
     */
    private static Object evaluateStepPath(Plan.Step step, String path) {
        if (path == null || path.isEmpty()) return step;

        // 处理数组索引，例如: result.papers[0]
        if (path.contains("[")) {
            int bracketIndex = path.indexOf('[');
            String fieldName = path.substring(0, bracketIndex);
            int index = Integer.parseInt(path.substring(bracketIndex + 1, path.indexOf(']')));

            Object fieldValue = getStepField(step, fieldName);
            if (fieldValue instanceof List<?> list) {
                return (index >= 0 && index < list.size()) ? list.get(index) : null;
            }
            return null;
        }

        // 普通属性访问，例如: result.keywords
        String[] parts = path.split("\\.");
        Object current = step;
        for (String part : parts) {
            if (current == null) return null;
            // 注意：这里假设 Step 内部的字段（如 result）本身是 Map
            // 如果 result 是自定义 POJO，可以在此处再加一层反射
            if (current instanceof Plan.Step) {
                current = getStepField((Plan.Step) current, part);
            } else if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    // ==================== 纯反射提取方法（针对 Plan 和 Step 专属定制） ====================

    /**
     * 获取 Plan 的字段值
     */
    private static Object getPlanField(Plan plan, String fieldName) {
        try {
            Field field = Plan.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(plan);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 获取 Step 的字段值
     */
    private static Object getStepField(Plan.Step step, String fieldName) {
        try {
            Field field = Plan.Step.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(step);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 根据 ID 查找 Step
     */
    private static Plan.Step findStepById(Plan plan, String stepId) {
        if (plan.getSteps() == null) return null;
        for (Plan.Step step : plan.getSteps()) {
            if (step.getId() != null && step.getId().equals(stepId)) {
                return step;
            }
        }
        return null;
    }
}