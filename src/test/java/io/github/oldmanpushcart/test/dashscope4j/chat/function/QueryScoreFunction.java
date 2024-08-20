package io.github.oldmanpushcart.test.dashscope4j.chat.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFn;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@ChatFn(name = "query_score", description = "query student's scores")
public class QueryScoreFunction implements ChatFunction<QueryScoreFunction.Request, Result<List<QueryScoreFunction.Score>>> {

    private final Map<String, List<Score>> studentScoreMap = new HashMap<>() {{
        put("张三", List.of(
                new Score("张三", Subject.CHINESE, 90),
                new Score("张三", Subject.MATH, 80),
                new Score("张三", Subject.ENGLISH, 70)
        ));
        put("李四", List.of(
                new Score("李四", Subject.CHINESE, 80),
                new Score("李四", Subject.MATH, 70),
                new Score("李四", Subject.ENGLISH, 60)
        ));
        put("王五", List.of(
                new Score("王五", Subject.CHINESE, 70),
                new Score("王五", Subject.MATH, 60),
                new Score("王五", Subject.ENGLISH, 50)
        ));
    }};

    @Override
    public CompletionStage<Result<List<Score>>> call(Request request) {

        if (!studentScoreMap.containsKey(request.name())) {
            return CompletableFuture.completedFuture(new Result<>(
                    false,
                    "学生不存在",
                    null
            ));
        }

        return CompletableFuture.completedFuture(new Result<>(
                true,
                "查询成功",
                studentScoreMap.get(request.name()).stream().filter(score -> {
                    if (null == request.subjects()) {
                        return true;
                    }
                    for (final var subject : request.subjects()) {
                        if (subject == score.subject()) {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList())
        ));
    }

    public record Request(

            @JsonProperty(required = true)
            @JsonPropertyDescription("the student name to query")
            String name,

            @JsonProperty(required = true)
            @JsonPropertyDescription("the subjects to query")
            Subject... subjects

    ) {

    }

    public record Score(

            @JsonPropertyDescription("student name")
            String name,

            @JsonPropertyDescription("subject items")
            Subject subject,

            @JsonPropertyDescription("score value")
            float value

    ) {

    }


    public enum Subject {

        @JsonPropertyDescription("语文")
        CHINESE,

        @JsonPropertyDescription("数学")
        MATH,

        @JsonPropertyDescription("英语")
        ENGLISH

    }

}