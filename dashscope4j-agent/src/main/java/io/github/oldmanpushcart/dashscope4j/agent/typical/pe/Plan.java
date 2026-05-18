package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class Plan {

    @JsonProperty("tasks")
    private final List<Task> tasks;

    @JsonCreator
    private Plan(

            @JsonProperty("tasks")
            List<Task> tasks

    ) {
        this.tasks = Collections.unmodifiableList(tasks);
    }

    public synchronized boolean hasNext() {
        return next() != null;
    }

    public synchronized Task next() {
        if (isEmpty()) {
            return null;
        }
        for (final var task : tasks) {
            if (task.status() == Task.Status.SUCCESS) {
                continue;
            }
            if (task.status() == Task.Status.FAILURE) {
                return null;
            }
            if (task.status() == Task.Status.PENDING) {
                return task;
            }
        }
        return null;
    }

    public synchronized boolean taskSuccess(Task task, String result) {
        if (task.status() != Task.Status.PENDING) {
            return false;
        }
        task.status = Task.Status.SUCCESS;
        task.result = result;
        return true;
    }

    public synchronized boolean taskFailure(Task task, String error) {
        if (task.status() != Task.Status.PENDING) {
            return false;
        }
        task.status = Task.Status.FAILURE;
        task.result = error;
        return true;
    }

    public synchronized boolean isFailure() {
        return tasks.stream()
                .anyMatch(task -> task.status() == Task.Status.FAILURE);
    }

    public synchronized boolean isSuccess() {
        return tasks.stream()
                .allMatch(task -> task.status() == Task.Status.SUCCESS);
    }

    public synchronized boolean isEmpty() {
        return tasks.isEmpty();
    }

    public synchronized List<Task> tasks() {
        return tasks;
    }

    public static class Task {

        private final String taskId;
        private final String goal;

        private String result;
        private Status status;

        @JsonCreator
        private Task(

                @JsonProperty("task_id")
                String taskId,

                @JsonProperty("goal")
                String goal,

                @JsonProperty("result")
                String result,

                @JsonProperty("status")
                Status status
        ) {
            this.taskId = taskId;
            this.goal = goal;
            this.result = result;
            this.status = status;
        }

        @JsonProperty("task_id")
        public String taskId() {
            return taskId;
        }

        @JsonProperty("goal")
        public String goal() {
            return goal;
        }

        @JsonProperty("result")
        public String result() {
            return result;
        }

        @JsonProperty("status")
        public Status status() {
            return status;
        }

        public enum Status {
            @JsonProperty("pending") PENDING,
            @JsonProperty("success") SUCCESS,
            @JsonProperty("failure") FAILURE
        }

    }

}
