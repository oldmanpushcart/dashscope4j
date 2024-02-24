package io.github.ompc.dashscope4j;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 任务
 *
 * @param id      任务ID
 * @param status  任务状态
 * @param metrics 任务进度
 * @param timing  任务时间
 */
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

    /**
     * 任务进度
     *
     * @param total     总数
     * @param succeeded 成功数
     * @param failed    失败数
     */
    public record Metrics(
            @JsonProperty("TOTAL")
            int total,
            @JsonProperty("SUCCEEDED")
            int succeeded,
            @JsonProperty("FAILED")
            int failed
    ) {

    }

    /**
     * 任务时间表
     *
     * @param submit    任务提交时间
     * @param scheduled 任务最后被调度时间
     * @param end       任务结束时间
     */
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

    /**
     * 任务等待策略
     */
    public interface WaitStrategy {

        /**
         * 等待直至任务结束
         *
         * @param task 任务
         * @return 任务等待应答
         */
        CompletableFuture<?> until(Task task);

    }

    /**
     * 任务等待策略实例
     */
    public interface WaitStrategies {

        /**
         * 间隔等待策略
         *
         * @param interval 间隔
         * @return 等待策略
         */
        static WaitStrategy interval(Duration interval) {
            return task -> {
                final var future = new CompletableFuture<>();
                final var lock = new ReentrantLock();
                final var condition = lock.newCondition();
                lock.lock();
                try {
                    if (!condition.await(interval.toMillis(), TimeUnit.MILLISECONDS)) {
                        future.complete(null);
                    }
                } catch (InterruptedException e) {
                    future.cancel(true);
                } finally {
                    lock.unlock();
                }
                return future;
            };
        }

    }

    /**
     * 半任务
     *
     * @param <V>
     */
    public interface Half<V> {

        /**
         * 等待任务结束
         *
         * @param strategy 等待策略
         * @return 任务应答
         */
        CompletableFuture<V> waitingFor(WaitStrategy strategy);

    }

}
