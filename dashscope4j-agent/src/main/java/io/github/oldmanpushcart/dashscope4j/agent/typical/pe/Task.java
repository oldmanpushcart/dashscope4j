package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
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
    private final AtomicReference<TaskStatus> status;
    private volatile String result;
    private volatile String error;
    private volatile LocalDateTime startTime;
    private volatile LocalDateTime endTime;
    
    /**
     * Jackson 反序列化构造函数
     */
    @JsonCreator
    public Task(
            @JsonProperty("taskId") String taskId,
            @JsonProperty("description") String description) {
        this.taskId = taskId != null ? taskId : "unknown";
        this.description = description != null ? description : "";
        this.status = new AtomicReference<>(TaskStatus.PENDING);
    }
    
    /**
     * 标记任务开始执行
     */
    public void start() {
        this.startTime = LocalDateTime.now();
        this.status.set(TaskStatus.RUNNING);
    }
    
    /**
     * 标记任务执行成功
     *
     * @param result 执行结果
     */
    public void complete(String result) {
        this.result = result;
        this.endTime = LocalDateTime.now();
        this.status.set(TaskStatus.SUCCESS);
    }
    
    /**
     * 标记任务执行失败
     *
     * @param error 错误信息
     */
    public void fail(String error) {
        this.error = error;
        this.endTime = LocalDateTime.now();
        this.status.set(TaskStatus.FAILED);
    }
    
    /**
     * 标记任务被跳过
     */
    public void skip() {
        this.status.set(TaskStatus.SKIPPED);
    }
    
    // ==================== Getters ====================
    
    public String getTaskId() {
        return taskId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public TaskStatus getStatus() {
        return status.get();
    }
    
    public String getResult() {
        return result;
    }
    
    public String getError() {
        return error;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    /**
     * 判断任务是否已完成（成功或失败）
     *
     * @return true 如果任务已结束
     */
    public boolean isFinished() {
        TaskStatus s = status.get();
        return s == TaskStatus.SUCCESS || s == TaskStatus.FAILED || s == TaskStatus.SKIPPED;
    }
    
    /**
     * 判断任务是否成功
     *
     * @return true 如果任务成功
     */
    public boolean isSuccess() {
        return status.get() == TaskStatus.SUCCESS;
    }
    
    @Override
    public String toString() {
        return String.format("Task{taskId='%s', status=%s, description='%s'}", 
                taskId, status.get(), description);
    }
}
