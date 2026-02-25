package io.github.oldmanpushcart.dashscope4j.agent.typical.react.interceptor;

import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

record ReAct(
        String observation,
        String finalAnswer,
        String thought,
        String action,
        String actionInput
) {

    public static final String KEY_QUESTION = "Question";
    public static final String KEY_OBSERVATION = "Observation";
    public static final String KEY_FINAL_ANSWER = "Final Answer";
    public static final String KEY_THOUGHT = "Thought";
    public static final String KEY_ACTION = "Action";
    public static final String KEY_ACTION_INPUT = "Action Input";

    private static final Set<String> keys = Set.of(
            KEY_QUESTION,
            KEY_OBSERVATION,
            KEY_FINAL_ANSWER,
            KEY_THOUGHT,
            KEY_ACTION,
            KEY_ACTION_INPUT
    );

    private static final Pattern pattern;

    static {
        final var keyRegexSegment = keys.stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        final var regex = "(?s)\\b(" + keyRegexSegment + ")\\s*:\\s*(.*?)(?=\\b(" + keyRegexSegment + ")\\s*:|$)";
        pattern = Pattern.compile(regex);
    }

    public boolean hasFinalAnswer() {
        return finalAnswer != null && !finalAnswer.isEmpty();
    }

    public boolean hasAction() {
        return action != null && !action.isEmpty();
    }

    public static ReAct of(String text) {
        final var pojoMap = new HashMap<String, String>();
        final var matcher = pattern.matcher(text);
        while (matcher.find()) {
            final var key = matcher.group(1).trim();
            final var value = matcher.group(2).trim();
            pojoMap.put(key, value);
        }

        return new ReAct(
                pojoMap.get(KEY_OBSERVATION),
                pojoMap.get(KEY_FINAL_ANSWER),
                pojoMap.get(KEY_THOUGHT),
                pojoMap.get(KEY_ACTION),
                pojoMap.get(KEY_ACTION_INPUT)
        );
    }

}
