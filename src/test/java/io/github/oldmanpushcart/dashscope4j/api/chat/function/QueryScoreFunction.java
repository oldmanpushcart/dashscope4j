package io.github.oldmanpushcart.dashscope4j.api.chat.function;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

@ChatFnName("query-score-function")
@ChatFnDescription("查询科目成绩")
public class QueryScoreFunction implements ChatFunction<QueryScoreFunction.Parameter, QueryScoreFunction.Result> {

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final Subject subject = parameter.getSubject();
        switch (subject) {
            case MATCH:
                return completedFuture(new Result(subject, 88));
            case ENGLISH:
                return completedFuture(new Result(subject, 90));
            case CHINESE:
                return completedFuture(new Result(subject, 100));
            case PHYSICS:
                return completedFuture(new Result(subject, 65.5f));
        }
        throw new IllegalArgumentException(String.format("illegal subject: %s", subject));
    }

    @Builder(access = AccessLevel.PRIVATE)
    @Jacksonized
    @Data
    public static class Parameter {

        @JsonPropertyDescription("科目")
        @JsonProperty(required = true)
        private final Subject subject;

    }

    @Data
    public static class Result {

        @JsonPropertyDescription("科目")
        private final Subject subject;

        @JsonPropertyDescription("成绩")
        private final float score;
    }

    @JsonClassDescription("科目")
    public enum Subject {

        @JsonPropertyDescription("数学")
        @JsonProperty("math")
        MATCH,

        @JsonPropertyDescription("英语")
        @JsonProperty("english")
        ENGLISH,

        @JsonPropertyDescription("语文")
        @JsonProperty("chinese")
        CHINESE,

        @JsonPropertyDescription("物理")
        @JsonProperty("physics")
        PHYSICS

    }

}
