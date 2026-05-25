package io.github.oldmanpushcart.dashscope4j.client.api.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.Ret;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Task extends Ret {

    private final String taskId;
    private final Status status;
    private final Metrics metrics;
    private final Instant submitAt;
    private final Instant scheduledAt;
    private final Instant endAt;

    @JsonCreator
    private Task(

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String desc,

            @JsonProperty("task_id")
            String taskId,

            @JsonProperty("task_status")
            Status status,

            @JsonProperty("task_metrics")
            Metrics metrics,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
            @JsonProperty("submit_time")
            Date submitAt,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
            @JsonProperty("scheduled_time")
            Date scheduledAt,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
            @JsonProperty("end_time")
            Date endAt

    ) {
        super(code, desc);
        this.taskId = taskId;
        this.status = status;
        this.metrics = metrics;
        this.submitAt = null != submitAt ? submitAt.toInstant() : null;
        this.scheduledAt = null != scheduledAt ? scheduledAt.toInstant() : null;
        this.endAt = null != endAt ? endAt.toInstant() : null;
    }

    /**
     * 任务是否已经完成
     * <p>
     * 任务处于终结状态，即任务已经完成，无法继续运行。
     * 终结状态包括：
     * <ul>
     *     <li>成功；{@link Status#SUCCEEDED}</li>
     *     <li>失败；{@link Status#FAILED}</li>
     *     <li>取消；{@link Status#CANCELED}</li>
     * </ul>
     * </p>
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

    public String taskId() {
        return taskId;
    }

    public Status status() {
        return status;
    }

    public Metrics metrics() {
        return metrics;
    }

    public Instant submitAt() {
        return submitAt;
    }

    public Instant scheduledAt() {
        return scheduledAt;
    }

    public Instant endAt() {
        return endAt;
    }

    /**
     * 任务状态
     */
    public enum Status {

        /**
         * 等待
         */
        PENDING,

        /**
         * 运行中
         */
        RUNNING,

        /**
         * 成功
         */
        SUCCEEDED,

        /**
         * 失败
         */
        FAILED,

        /**
         * 取消
         */
        CANCELED,

        /**
         * 未知
         */
        UNKNOWN
    }

    /**
     * 任务指标
     */
    public static class Metrics {

        private final int total;
        private final int succeeded;
        private final int failed;

        @JsonCreator
        private Metrics(

                @JsonProperty("TOTAL")
                int total,

                @JsonProperty("SUCCEEDED")
                int succeeded,

                @JsonProperty("FAILED")
                int failed

        ) {
            this.total = total;
            this.succeeded = succeeded;
            this.failed = failed;
        }

        public int total() {
            return total;
        }

        public int succeeded() {
            return succeeded;
        }

        public int failed() {
            return failed;
        }

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
        CompletionStage<?> performWait(Task task);

    }

    /**
     * 半提交任务
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
        CompletionStage<V> waitingFor(WaitStrategy strategy);

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
        static WaitStrategy always(Duration interval) {
            return task -> {
                final CompletableFuture<?> future = new CompletableFuture<>();
                final ReentrantLock lock = new ReentrantLock();
                final Condition condition = lock.newCondition();
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
        static WaitStrategy until(Duration interval, Duration timeout) {
            return task -> {
                final CompletableFuture<?> future = new CompletableFuture<>();
                final ReentrantLock lock = new ReentrantLock();
                final Condition condition = lock.newCondition();
                lock.lock();
                try {

                    // 检查任务是否已经过了超时时间限制
                    if (Instant.now().isAfter(task.submitAt().plus(timeout))) {
                        future.cancel(true);
                    }

                    // 等待间隔时间后，完成本轮等待策略
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

}
