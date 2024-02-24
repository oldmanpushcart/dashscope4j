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

/**
 * 任务获取请求
 */
public final class TaskGetRequest extends ApiRequest<TaskGetResponse> {

    private static final ObjectMapper mapper = JacksonUtils.mapper();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String taskId;

    private TaskGetRequest(Builder builder) {
        super(builder, TaskGetResponse.class);
        this.taskId = requireNonNull(builder.taskId);
    }

    @Override
    protected HttpRequest newHttpRequest() {
        logger.debug("dashscope://task/get => {}", taskId);
        return HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/tasks/%s".formatted(taskId)))
                .GET()
                .build();
    }

    @Override
    protected Function<String, TaskGetResponse> responseDeserializer() {
        return body -> {
            logger.debug("dashscope://task/get <= {}", body);
            return JacksonUtils.toObject(mapper, body, responseType);
        };
    }

    public String taskId() {
        return taskId;
    }

    public static class Builder extends ApiRequest.Builder<TaskGetRequest, Builder> {

        private String taskId;

        /**
         * 设置任务ID
         *
         * @param taskId 任务ID
         * @return 构造器
         */
        public Builder taskId(String taskId) {
            this.taskId = requireNonNull(taskId);
            return self();
        }

        @Override
        public TaskGetRequest build() {
            return new TaskGetRequest(this);
        }

    }

}
