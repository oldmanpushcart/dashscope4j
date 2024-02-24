package io.github.ompc.dashscope4j.internal.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.internal.api.ApiRequest;
import io.github.ompc.dashscope4j.internal.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class TaskCancelRequest extends ApiRequest<TaskCancelResponse> {

    private static final ObjectMapper mapper = JacksonUtils.mapper();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String taskId;

    protected TaskCancelRequest(Builder builder) {
        super(builder, TaskCancelResponse.class);
        this.taskId = requireNonNull(builder.taskId);
    }

    @Override
    protected HttpRequest newHttpRequest() {
        logger.debug("dashscope://task/cancel => {}", taskId);
        return HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/tasks/%s/cancel".formatted(taskId)))
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();
    }

    @Override
    protected Function<String, TaskCancelResponse> responseDeserializer() {
        return body -> {
            logger.debug("dashscope://task/cancel <= {}", body);
            return JacksonUtils.toObject(mapper, body, responseType);
        };
    }

    public static class Builder extends ApiRequest.Builder<TaskCancelRequest, Builder> {

        private String taskId;

        public Builder taskId(String taskId) {
            this.taskId = requireNonNull(taskId);
            return this;
        }

        @Override
        public TaskCancelRequest build() {
            return new TaskCancelRequest(this);
        }

    }

}
