package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class Plan {

    private final List<Task> tasks;

    @JsonCreator
    private Plan(

            @JsonProperty("tasks")
            List<Task> tasks

    ) {
        this.tasks = Collections.unmodifiableList(tasks);
    }

    public boolean hasNext() {
        return next() != null;
    }

    public Task next() {
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

    @JsonIgnore
    public boolean isFailure() {
        return tasks.stream()
                .anyMatch(task -> task.status() == Task.Status.FAILURE);
    }

    @JsonIgnore
    public boolean isSuccess() {
        return tasks.stream()
                .allMatch(task -> task.status() == Task.Status.SUCCESS);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    @JsonProperty("tasks")
    public List<Task> tasks() {
        return tasks;
    }

    public static class Task {

        private final String name;
        private final String goal;

        private String result;
        private Status status;

        @JsonCreator
        private Task(

                @JsonProperty("name")
                String name,

                @JsonProperty("goal")
                String goal,

                @JsonProperty("result")
                String result,

                @JsonProperty("status")
                Status status
        ) {
            this.name = name;
            this.goal = goal;
            this.result = result;
            this.status = status;
        }

        @JsonProperty("name")
        public String name() {
            return name;
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

        public boolean success(String result) {
            if (status != Status.PENDING) {
                return false;
            }
            status = Status.SUCCESS;
            this.result = result;
            return true;
        }

        public boolean failure(String error) {
            if (status != Status.PENDING) {
                return false;
            }
            status = Status.FAILURE;
            this.result = error;
            return true;
        }

        public enum Status {
            @JsonProperty("pending") PENDING,
            @JsonProperty("success") SUCCESS,
            @JsonProperty("failure") FAILURE
        }

    }

}
