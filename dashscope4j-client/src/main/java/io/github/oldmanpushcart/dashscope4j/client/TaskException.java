package io.github.oldmanpushcart.dashscope4j.client;

/**
 * 任务异常
 */
public class TaskException extends RuntimeException {

    private final String taskId;

    protected TaskException(String taskId, String message) {
        super(message);
        this.taskId = taskId;
    }

    /**
     * @return 任务ID
     */
    public String taskId() {
        return taskId;
    }

    /**
     * 任务取消异常
     */
    public static class TaskCancelledException extends TaskException {

        /**
         * 构造任务取消异常
         *
         * @param taskId 任务ID
         */
        public TaskCancelledException(String taskId) {
            super(taskId, "Task: %s cancelled".formatted(taskId));
        }

    }

    /**
     * 任务失败异常
     */
    public static class TaskFailedException extends TaskException {

        private final String code;
        private final String desc;

        /**
         * 构造任务失败异常
         *
         * @param task   任务
         */
        public TaskFailedException(Task task) {
            super(task.taskId(), "Task: %s failed! code=%s;desc=%s;".formatted(task.taskId(), task.code(), task.desc()));
            this.code = task.code();
            this.desc = task.desc();
        }

        public String code() {
            return code;
        }

        public String desc() {
            return desc;
        }

    }

}
