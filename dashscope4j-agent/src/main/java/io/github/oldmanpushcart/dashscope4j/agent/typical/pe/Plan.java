package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 执行计划
 * <p>
 * 管理整个任务的执行计划，包括任务列表、当前执行进度和执行历史。
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Plan {

    @JsonProperty("thought")
    private final String thought;
    
    @JsonProperty("tasks")
    private final List<Task> tasks;
    
    @JsonProperty("index")
    private volatile int index;

    /**
     * Jackson 反序列化构造函数
     */
    @JsonCreator
    public Plan(

            @JsonProperty("thought")
            String thought,

            @JsonProperty("tasks")
            List<Task> tasks,

            @JsonProperty("index")
            Integer index

    ) {
        this.thought = thought;
        this.tasks = tasks;
        this.index = index != null ? index : 0;
    }

    /**
     * 获取当前待执行的任务
     *
     * @return 当前任务，如果没有则返回 null
     */
    public synchronized Task current() {
        if (index >= tasks.size()) {
            return null;
        }
        return tasks.get(index);
    }

    /**
     * 推进到下一个任务
     */
    public synchronized void advance() {
        index++;
    }

    /**
     * 获取当前任务索引
     *
     * @return 当前任务索引（从0开始）
     */
    public int index() {
        return index;
    }

    /**
     * 获取总任务数
     *
     * @return 任务总数
     */
    public int size() {
        return tasks.size();
    }

    /**
     * 计划是否已完成
     *
     * @return TRUE | FALSE
     */
    @JsonIgnore
    public synchronized boolean isFinished() {
        return tasks.stream().allMatch(Task::isFinished);
    }

    /**
     * 计划是否为空
     *
     * @return TRUE | FALSE
     */
    @JsonIgnore
    public synchronized boolean isEmpty() {
        return tasks.isEmpty();
    }

    /**
     * @return 思考过程
     */
    public String thought() {
        return thought;
    }

    /**
     * @return 任务列表
     */
    public List<Task> tasks() {
        return tasks;
    }

}
