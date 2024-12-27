package io.github.oldmanpushcart.dashscope4j.task;

import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import lombok.Getter;
import lombok.experimental.Accessors;

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
            super(taskId, String.format("task: %s cancelled", taskId));
        }

    }

    /**
     * 任务失败异常
     */
    @Getter
    @Accessors(fluent = true)
    public static class TaskFailedException extends TaskException {

        private final String code;
        private final String desc;

        /**
         * 构造任务失败异常
         *
         * @param taskId   任务ID
         * @param response 应答
         */
        public TaskFailedException(String taskId, ApiResponse<?> response) {
            super(taskId, String.format("task: %s failed! code=%s;desc=%s;",
                    taskId,
                    response.code(),
                    response.desc()
            ));
            this.code = response.code();
            this.desc = response.desc();
        }
    }

}
