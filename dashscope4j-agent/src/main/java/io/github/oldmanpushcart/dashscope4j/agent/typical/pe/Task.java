package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 任务运行时状态
 * <p>
 * 管理单个任务的执行状态、结果和元数据。
 * </p>
 */
public class Task {
    
    private final String taskId;
    private final String description;
    private final AtomicReference<Status> status;
    private volatile String result;
    private volatile String error;
    
    /**
     * Jackson 反序列化构造函数
     */
    @JsonCreator
    public Task(

            @JsonProperty("taskId")
            String taskId,

            @JsonProperty("description")
            String description

    ) {
        this.taskId = taskId;
        this.description = description;
        this.status = new AtomicReference<>(Status.PENDING);
    }
    
    /**
     * 标记任务开始执行
     */
    public void start() {
        this.status.set(Status.RUNNING);
    }
    
    /**
     * 标记任务执行成功
     *
     * @param result 执行结果
     */
    public void complete(String result) {
        this.result = result;
        this.status.set(Status.SUCCESS);
    }
    
    /**
     * 标记任务执行失败
     *
     * @param error 错误信息
     */
    public void fail(String error) {
        this.error = error;
        this.status.set(Status.FAILED);
    }
    
    /**
     * 标记任务被跳过
     */
    public void skip() {
        this.status.set(Status.SKIPPED);
    }
    
    // ==================== Getters ====================
    
    public String taskId() {
        return taskId;
    }
    
    public String description() {
        return description;
    }
    
    public Status status() {
        return status.get();
    }
    
    public String result() {
        return result;
    }
    
    public String error() {
        return error;
    }
    
    /**
     * 判断任务是否已完成（成功或失败）
     *
     * @return true 如果任务已结束
     */
    public boolean isFinished() {
        Status s = status.get();
        return s == Status.SUCCESS || s == Status.FAILED || s == Status.SKIPPED;
    }
    
    /**
     * 判断任务是否成功
     *
     * @return true 如果任务成功
     */
    public boolean isSuccess() {
        return status.get() == Status.SUCCESS;
    }

    /**
     * 任务状态枚举
     */
    public enum Status {

        /**
         * 待执行
         */
        PENDING,

        /**
         * 执行中
         */
        RUNNING,

        /**
         * 执行成功
         */
        SUCCESS,

        /**
         * 执行失败
         */
        FAILED,

        /**
         * 已跳过（Replan 时被移除或合并）
         */
        SKIPPED
    }
}
