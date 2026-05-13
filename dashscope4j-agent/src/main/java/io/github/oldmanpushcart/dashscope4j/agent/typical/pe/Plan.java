package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 执行计划
 * <p>
 * 管理整个任务的执行计划，包括任务列表、当前执行进度和执行历史。
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Plan {
    
    private final String thought;
    private final List<Task> tasks;
    private volatile int currentTaskIndex;
    private final ReentrantLock lock;
    
    /**
     * Jackson 反序列化构造函数
     */
    @JsonCreator
    public Plan(
            @JsonProperty("thought") String thought,
            @JsonProperty("tasks") List<Task> tasks
    ) {
        this.thought = thought != null ? thought : "";
        this.tasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        this.currentTaskIndex = 0;
        this.lock = new ReentrantLock();
    }
    
    /**
     * 获取下一个待执行的任务
     *
     * @return 下一个任务，如果没有则返回 null
     */
    public Task getNextTask() {
        lock.lock();
        try {
            if (currentTaskIndex >= tasks.size()) {
                return null;
            }
            return tasks.get(currentTaskIndex);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 推进到下一个任务
     */
    public void advanceToNextTask() {
        lock.lock();
        try {
            currentTaskIndex++;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 获取当前任务索引
     *
     * @return 当前任务索引（从0开始）
     */
    public int getCurrentTaskIndex() {
        return currentTaskIndex;
    }
    
    /**
     * 获取总任务数
     *
     * @return 任务总数
     */
    public int getTotalTasks() {
        return tasks.size();
    }
    
    /**
     * 判断所有任务是否已完成
     *
     * @return true 如果所有任务都已结束
     */
    public boolean isAllTasksFinished() {
        lock.lock();
        try {
            return tasks.stream().allMatch(Task::isFinished);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 获取思考过程
     *
     * @return 思考过程
     */
    public String getThought() {
        return thought;
    }
    
    /**
     * 获取所有任务（不可变视图）
     *
     * @return 任务列表
     */
    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }
    
    /**
     * 获取已成功完成的任务数量
     *
     * @return 成功任务数
     */
    public long getSuccessCount() {
        lock.lock();
        try {
            return tasks.stream().filter(Task::isSuccess).count();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 获取失败的任务数量
     *
     * @return 失败任务数
     */
    public long getFailedCount() {
        lock.lock();
        try {
            return tasks.stream().filter(t -> t.getStatus() == TaskStatus.FAILED).count();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 创建计划快照（用于日志或传递给 LLM）
     *
     * @return 计划快照字符串
     */
    public String createSnapshot() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Plan Snapshot ===\n");
            sb.append("Thought: ").append(thought).append("\n\n");
            sb.append("Progress: ").append(currentTaskIndex).append("/").append(tasks.size()).append("\n\n");
            sb.append("Tasks:\n");
            
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                
                if (i == currentTaskIndex) {
                    // Current task - this agent's responsibility
                    sb.append(" >> [CURRENT TASK - YOUR RESPONSIBILITY]\n");
                    sb.append("    ")
                      .append(String.format("[%d] %s - %s", i + 1, task.getStatus(), task.getDescription()))
                      .append("\n");
                } else {
                    // Other tasks - not this agent's responsibility
                    sb.append("    [OTHER TASK - Not your responsibility]\n");
                    sb.append("    ")
                      .append(String.format("[%d] %s - %s", i + 1, task.getStatus(), task.getDescription()))
                      .append("\n");
                }
                
                if (task.getResult() != null) {
                    sb.append("        Result: ").append(truncate(task.getResult(), 500)).append("\n");
                }
                if (task.getError() != null) {
                    sb.append("        Error: ").append(truncate(task.getError(), 500)).append("\n");
                }
            }
            
            sb.append("\n=== End Snapshot ===");
            return sb.toString();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 截断字符串
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
    
    @Override
    public String toString() {
        return String.format("Plan{thought='%s', tasks=%d, current=%d}", 
                thought, tasks.size(), currentTaskIndex);
    }
}
