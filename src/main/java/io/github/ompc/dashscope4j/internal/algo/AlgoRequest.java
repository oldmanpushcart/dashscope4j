package io.github.ompc.dashscope4j.internal.algo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.Model;
import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.internal.api.ApiRequest;
import io.github.ompc.dashscope4j.internal.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.util.function.Function;

import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.ContentType.MIME_APPLICATION_JSON;
import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_CONTENT_TYPE;
import static java.util.Objects.requireNonNull;

/**
 * 算法请求
 */
public interface AlgoRequest<R extends AlgoResponse<?>> extends ApiRequest<R> {

    @JsonProperty("model")
    Model model();

    @JsonProperty("input")
    Object input();

    @JsonProperty("parameters")
    Option option();

    /**
     * 算法请求构建器
     *
     * @param <M> 模型
     * @param <T> 请求
     * @param <B> 构建器
     */
    interface Builder<M extends Model, T extends AlgoRequest<?>, B extends Builder<M, T, B>> extends ApiRequest.Builder<T, B> {

        /**
         * 设置模型
         *
         * @param model 模型
         * @return this
         */
        B model(M model);

        /**
         * 设置选项
         *
         * @param opt   选项
         * @param value 选项值
         * @param <OT>  选项类型
         * @param <OR>  选项值类型
         * @return this
         */
        <OT, OR> B option(Option.Opt<OT, OR> opt, OT value);

        /**
         * 设置选项
         *
         * @param name  选项名
         * @param value 选项值
         * @return this
         */
        B option(String name, Object value);

    }

}
