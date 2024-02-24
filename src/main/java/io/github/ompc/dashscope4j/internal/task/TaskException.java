package io.github.ompc.dashscope4j.internal.task;


import io.github.ompc.dashscope4j.Ret;

public class TaskException extends RuntimeException {

    private final String taskId;

    public TaskException(String taskId, String message) {
        super(message);
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    public static class TaskCancelledException extends TaskException {

        public TaskCancelledException(String taskId) {
            super(taskId, "task: %s cancelled".formatted(taskId));
        }

    }

    public static class TaskFailedException extends TaskException {

        private final Ret ret;

        public TaskFailedException(String taskId, Ret ret) {
            super(taskId, "task: %s failed! code=%s;message=%s;".formatted(taskId, ret.code(), ret.message()));
            this.ret = ret;
        }

        public Ret getRet() {
            return ret;
        }

    }

}
