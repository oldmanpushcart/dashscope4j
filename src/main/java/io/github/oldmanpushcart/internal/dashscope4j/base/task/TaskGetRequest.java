package io.github.oldmanpushcart.internal.dashscope4j.base.task;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
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
 * 任务获取请求
 */
public record TaskGetRequest(String taskId, Duration timeout) implements ApiRequest<TaskGetResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    @Override
    public String suite() {
        return "dashscope://base/task";
    }

    @Override
    public String type() {
        return "get";
    }

    @Override
    public HttpRequest newHttpRequest() {
        logger.debug("{} => {}", protocol(), taskId);
        return HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/tasks/%s".formatted(taskId)))
                .GET()
                .build();
    }

    @Override
    public Function<String, TaskGetResponse> responseDeserializer() {
        return body -> {
            logger.debug("{} <= {}", protocol(), body);
            return JacksonUtils.toObject(body, TaskGetResponse.class);
        };
    }

    public static class Builder extends ApiRequestBuilderImpl<TaskGetRequest, Builder> {

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
        public TaskGetRequest build() {
            return new TaskGetRequest(
                    requireNonNull(taskId, "taskId is required!"),
                    timeout()
            );
        }

    }

}
