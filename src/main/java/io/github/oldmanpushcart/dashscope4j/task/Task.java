package io.github.oldmanpushcart.dashscope4j.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Task {

    String identity;
    Status status;
    Metrics metrics;
    Timing timing;

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

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Metrics {

        int total;
        int succeeded;
        int failed;

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

    }

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Timing {

        Instant submit, scheduled, end;

        public Timing(Date submit, Date scheduled, Date end) {
            this.submit = Optional.ofNullable(submit)
                    .map(Date::toInstant)
                    .orElse(null);
            this.scheduled = Optional.ofNullable(scheduled)
                    .map(Date::toInstant)
                    .orElse(null);
            this.end = Optional.ofNullable(end)
                    .map(Date::toInstant)
                    .orElse(null);
        }
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
                    if (Instant.now().isAfter(task.timing().submit().plus(timeout))) {
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

}
