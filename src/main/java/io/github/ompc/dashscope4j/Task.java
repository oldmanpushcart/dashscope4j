package io.github.ompc.dashscope4j;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.concurrent.CompletableFuture;

public record Task(String id, Status status, Metrics metrics) {

    public boolean isCompleted() {
        return status == Status.SUCCEEDED || status == Status.FAILED || status == Status.CANCELED;
    }

    public enum Status {
        PENDING,
        RUNNING,
        SUCCEEDED,
        FAILED,
        CANCELED,
        UNKNOWN
    }

    public record Metrics(
            @JsonProperty("TOTAL")
            int total,
            @JsonProperty("SUCCEEDED")
            int succeeded,
            @JsonProperty("FAILED")
            int failed
    ) {

    }

    @JsonCreator
    static Task of(
            @JsonProperty("task_id")
            String id,
            @JsonProperty("task_status")
            Task.Status status,
            @JsonProperty("task_metrics")
            Task.Metrics metrics
    ) {
        return new Task(id, status, metrics);
    }

    public interface WaitStrategy {

        CompletableFuture<?> until(String taskId);

    }

    public interface Half<V> {

        CompletableFuture<V> waitingFor(WaitStrategy strategy);

    }
}
