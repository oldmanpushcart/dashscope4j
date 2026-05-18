package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import java.util.List;

public class Plan {

    private final List<Task> tasks;
    private volatile int index;

    public Task current() {
        return tasks.get(index);
    }

    public int size() {
        return tasks.size();
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    public boolean hasNext() {
        return index < tasks.size();
    }

    public boolean isSuccess() {
        return tasks.stream().allMatch(Task::isSuccess);
    }

    public boolean isFailure() {
        return tasks.stream().anyMatch(Task::isFailure);
    }

    public Plan(List<Task> tasks) {
        this.tasks = tasks;
    }

    public static class Task {

        private final String taskId;
        private final String goal;

        private volatile String result;
        private volatile Status status;

        public Task(String taskId, String goal) {
            this.taskId = taskId;
            this.goal = goal;
        }

        public String taskId() {
            return taskId;
        }

        public String goal() {
            return goal;
        }

        public String result() {
            return result;
        }

        public Status status() {
            return status;
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        public boolean isFailure() {
            return status == Status.FAILURE;
        }

        public boolean success(String result) {
            synchronized (this) {
                if (status != Status.PENDING) {
                    return false;
                }
                this.result = result;
                this.status = Status.SUCCESS;
            }
            return true;
        }

        public boolean failure(String result) {
            synchronized (this) {
                if (status != Status.PENDING) {
                    return false;
                }
                this.result = result;
                this.status = Status.FAILURE;
            }
            return true;
        }

        public enum Status {
            PENDING,
            EXECUTING,
            SUCCESS,
            FAILURE
        }

    }

}
