package io.github.oldmanpushcart.dashscope4j.base.task;

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

    /**
     * 任务是否已经完成
     *
     * @return TRUE | FALSE
     */
    public boolean isCompleted() {
        return status == Status.SUCCEEDED || status == Status.FAILED || status == Status.CANCELED;
    }

    /**
     * 任务是否可以取消
     * <p>只能取消非运行状态，一旦任务开始运行将无法被取消</p>
     *
     * @return TRUE | FALSE
     */
    public boolean isCancelable() {
        return status == Status.PENDING;
    }

    /**
     * 任务状态
     */
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
    @FunctionalInterface
    public interface WaitStrategy {

        /**
         * 进行等待
         *
         * @param task 任务
         * @return 任务等待应答
         */
        CompletableFuture<?> performWait(Task task);

    }

    /**
     * 任务等待策略实例
     */
    public interface WaitStrategies {

        /**
         * 永久等待策略
         *
         * @param interval 间隔
         * @return 等待策略
         */
        static WaitStrategy perpetual(Duration interval) {
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

        /**
         * 超时等待策略
         *
         * @param interval 间隔
         * @param timeout  超时
         * @return 等待策略
         */
        static WaitStrategy timeout(Duration interval, Duration timeout) {
            return task -> {
                final var future = new CompletableFuture<>();
                final var lock = new ReentrantLock();
                final var condition = lock.newCondition();
                lock.lock();
                try {

                    // 检查任务是否已经过了超时时间限制
                    if (System.currentTimeMillis() - task.timing().submit().getTime() > timeout.toMillis()) {
                        future.cancel(true);
                    }

                    // 等待间隔时间后，完成本轮等待策略
                    else if (!condition.await(interval.toMillis(), TimeUnit.MILLISECONDS)) {
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
