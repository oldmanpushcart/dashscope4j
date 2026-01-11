package io.github.oldmanpushcart.dashscope4j.client.api.chat.function;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class QueryScoreFunction {

    private static final Map<Subject, Float> scores = Map.of(
            Subject.MATH, 100f,
            Subject.ENGLISH, 50f,
            Subject.CHINESE, 95f,
            Subject.PHYSICS, 85f
    );

    public Result query(Query query) {
        final var map = new HashMap<Subject, Float>();
        for (final var subject : query.subjects()) {
            map.put(subject, scores.getOrDefault(subject, 0f));
        }
        return new Result(map);
    }

    public Tool toTool() {
        return FunctionTool.newBuilder()
                .name("query_score")
                .description("查询科目成绩")
                .function(this::query)
                .parameterType(Query.class)
                .build();
    }

    public record Query(

            @JsonPropertyDescription("要查询的科目列表")
            @JsonProperty("subjects")
            Set<Subject> subjects

    ) {

    }

    public record Result(

            @JsonPropertyDescription("科目对应的分数")
            @JsonProperty("scores")
            Map<Subject, Float> scores

    ) {

    }

    @JsonClassDescription("科目")
    public enum Subject {

        @JsonPropertyDescription("数学")
        MATH,

        @JsonPropertyDescription("英语")
        ENGLISH,

        @JsonPropertyDescription("语文")
        CHINESE,

        @JsonPropertyDescription("物理")
        PHYSICS,

    }

}
