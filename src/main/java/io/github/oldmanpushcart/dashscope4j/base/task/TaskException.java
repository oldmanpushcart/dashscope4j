package io.github.oldmanpushcart.dashscope4j.base.task;


import io.github.oldmanpushcart.dashscope4j.Ret;

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
     * 获取任务ID
     *
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
            super(taskId, "task: %s cancelled".formatted(taskId));
        }

    }

    /**
     * 任务失败异常
     */
    public static class TaskFailedException extends TaskException {

        private final Ret ret;

        /**
         * 构造任务失败异常
         *
         * @param taskId 任务ID
         * @param ret    应答结果
         */
        public TaskFailedException(String taskId, Ret ret) {
            super(taskId, "task: %s failed! code=%s;message=%s;".formatted(taskId, ret.code(), ret.message()));
            this.ret = ret;
        }

        /**
         * 获取应答结果
         *
         * @return 应答结果
         */
        public Ret ret() {
            return ret;
        }

    }

}
