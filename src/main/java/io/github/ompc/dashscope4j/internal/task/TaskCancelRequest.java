package io.github.ompc.dashscope4j.internal.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.internal.api.ApiRequest;
import io.github.ompc.dashscope4j.internal.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * 任务取消请求
 */
public record TaskCancelRequest(
        String taskId,
        Duration timeout

) implements ApiRequest<TaskCancelResponse> {

    private static final ObjectMapper mapper = JacksonUtils.mapper();
    private static final Logger logger = LoggerFactory.getLogger(TaskCancelRequest.class);


    private TaskCancelRequest(Builder builder) {
        this(
                requireNonNull(builder.taskId),
                requireNonNull(builder.timeout)
        );
    }

    @Override
    public HttpRequest newHttpRequest() {
        logger.debug("dashscope://task/cancel => {}", taskId);
        return HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/tasks/%s/cancel".formatted(taskId)))
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();
    }

    @Override
    public Function<String, TaskCancelResponse> responseDeserializer() {
        return body -> {
            logger.debug("dashscope://task/cancel <= {}", body);
            return JacksonUtils.toObject(mapper, body, TaskCancelResponse.class);
        };
    }

    public static class Builder implements ApiRequest.Builder<TaskCancelRequest, Builder> {

        private String taskId;
        private Duration timeout;

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
        public Builder timeout(Duration timeout) {
            this.timeout = Objects.requireNonNull(timeout);
            return this;
        }

        @Override
        public TaskCancelRequest build() {
            return new TaskCancelRequest(this);
        }

    }

}
