package io.github.oldmanpushcart.internal.dashscope4j.base.task;

import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiRequestBuilderImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;
import static java.util.Objects.requireNonNull;

/**
 * 任务取消请求
 */
public record TaskCancelRequest(String taskId, Duration timeout) implements HttpApiRequest<TaskCancelResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    @Override
    public String suite() {
        return "dashscope://base/task";
    }

    @Override
    public String type() {
        return "cancel";
    }

    @Override
    public HttpRequest newHttpRequest() {
        logger.debug("{} => {}", protocol(), taskId);
        return HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/tasks/%s/cancel".formatted(taskId)))
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();
    }

    @Override
    public Function<String, TaskCancelResponse> newResponseDecoder() {
        return body -> {
            logger.debug("{} <= {}", protocol(), body);
            return JacksonUtils.toObject(body, TaskCancelResponse.class);
        };
    }

    public static class Builder extends ApiRequestBuilderImpl<TaskCancelRequest, Builder> {

        private String taskId;

        /**
         * 设置任务ID
         *
         * @param taskId 任务ID
         * @return 构造器
         */
        public Builder taskId(String taskId) {
            this.taskId = requireNonNull(taskId);
            return this;
        }

        @Override
        public TaskCancelRequest build() {
            return new TaskCancelRequest(requireNonNull(taskId), timeout());
        }

    }

}
