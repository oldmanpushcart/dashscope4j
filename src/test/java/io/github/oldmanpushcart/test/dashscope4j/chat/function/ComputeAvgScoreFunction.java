package io.github.oldmanpushcart.test.dashscope4j.chat.function;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFn;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunction;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@ChatFn(name = "compute_avg_score", description = "计算平均成绩")
public class ComputeAvgScoreFunction implements ChatFunction<ComputeAvgScoreFunction.Request, ComputeAvgScoreFunction.Response> {

    @Override
    public CompletableFuture<Response> call(Request request) {
        return CompletableFuture.completedFuture(new Response(
                (float) Stream.of(request.scores()).mapToDouble(Float::doubleValue).average().orElse(0)
        ));
    }

    public record Request(
            @JsonPropertyDescription("分数集合")
            Float[] scores
    ) {

    }

    public record Response(
            @JsonPropertyDescription("平均分")
            Float avgScore
    ) {

    }

}