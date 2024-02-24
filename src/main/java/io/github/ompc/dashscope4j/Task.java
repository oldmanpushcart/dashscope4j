package io.github.ompc.dashscope4j;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public record Task(String id, Status status, Metrics metrics, Timing timing) {

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

    public record Timing(Date submit, Date scheduled, Date end) {

    }

    @JsonCreator
    static Task of(

            @JsonProperty("task_id")
            String id,

            @JsonProperty("task_status")
            Task.Status status,

            @JsonProperty("task_metrics")
            Task.Metrics metrics,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
            @JsonProperty("submit_time")
            Date submitTime,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
            @JsonProperty("scheduled_time")
            Date scheduledTime,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
            @JsonProperty("end_time")
            Date endTime

    ) {
        return new Task(id, status, metrics, new Timing(submitTime, scheduledTime, endTime));
    }

    public interface WaitStrategy {

        CompletableFuture<?> until(Task task);

    }

    public interface Half<V> {

        CompletableFuture<V> waitingFor(WaitStrategy strategy);

    }
}
