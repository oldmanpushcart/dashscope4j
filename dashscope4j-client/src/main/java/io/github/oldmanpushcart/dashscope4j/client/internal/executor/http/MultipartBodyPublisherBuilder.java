package io.github.oldmanpushcart.dashscope4j.client.internal.executor.http;

import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * HTTP-Multipart 请求体发布器构建器
 */
public class MultipartBodyPublisherBuilder implements Buildable<HttpRequest.BodyPublisher, MultipartBodyPublisherBuilder> {

    private String boundary;
    private final List<Part> parts = new ArrayList<>();

    /**
     * 设置边界
     *
     * @param boundary 边界
     * @return this
     */
    public MultipartBodyPublisherBuilder boundary(String boundary) {
        this.boundary = boundary;
        return this;
    }

    /**
     * 添加文本部分
     *
     * @param name    名称
     * @param text    文本
     * @param charset 字符集
     * @return this
     */
    public MultipartBodyPublisherBuilder part(String name, String text, Charset charset) {
        this.parts.add(new TextPart(name, text, charset));
        return this;
    }

    /**
     * 添加文本部分
     *
     * @param name 名称
     * @param text 文本
     * @return this
     */
    public MultipartBodyPublisherBuilder part(String name, String text) {
        return part(name, text, Charset.defaultCharset());
    }

    /**
     * 添加URI部分
     *
     * @param name 名称
     * @param uri  URI
     * @return this
     */
    public MultipartBodyPublisherBuilder part(String name, URI uri) {
        this.parts.add(new AnonymousUriPart(name, uri));
        return this;
    }

    /**
     * 添加URI部分
     *
     * @param name     名称
     * @param uri      URI
     * @param filename 资源名
     * @return this
     */
    public MultipartBodyPublisherBuilder part(String name, URI uri, String filename) {
        this.parts.add(new UriPart(name, uri, filename));
        return this;
    }

    @Override
    public HttpRequest.BodyPublisher build() {

        // 添加结束部分
        parts.add(new FinishPart());

        // 构建请求体
        return HttpRequest.BodyPublishers.concat(
                parts.stream()
                        .map(part -> part.body(boundary))
                        .toArray(HttpRequest.BodyPublisher[]::new)
        );

    }


    /**
     * HTTP-Multipart's part
     */
    private interface Part {

        /**
         * 获取部分体
         *
         * @param boundary 边界
         * @return 部分体
         */
        HttpRequest.BodyPublisher body(String boundary);

    }

    /**
     * Text part
     *
     * @param name    名称
     * @param text    文本
     * @param charset 字符集
     */
    private record TextPart(String name, String text, Charset charset) implements Part {

        @Override
        public HttpRequest.BodyPublisher body(String boundary) {
            final var body = """
                    --%s
                    Content-Disposition: form-data; name="%s"
                    Content-Type: text/plain; charset=%s
                    
                    %s
                    """
                    .formatted(boundary, name, charset.name(), text)
                    .replaceAll("\n", "\r\n");
            return HttpRequest.BodyPublishers.ofString(body, UTF_8);
        }

    }

    /**
     * 匿名URI部分
     *
     * @param name 名称
     * @param uri  URI
     */
    private record AnonymousUriPart(String name, URI uri) implements Part {

        @Override
        public HttpRequest.BodyPublisher body(String boundary) {

            // header
            final var header = """
                    --%s
                    Content-Disposition: form-data; name="%s"
                    Content-Type: application/octet-stream
                    
                    """
                    .formatted(boundary, name)
                    .replaceAll("\n", "\r\n");

            // header & input
            return HttpRequest.BodyPublishers.concat(
                    HttpRequest.BodyPublishers.ofString(header, UTF_8),
                    HttpRequest.BodyPublishers.ofInputStream(() -> {
                        try {
                            return uri.toURL().openStream();
                        } catch (IOException e) {
                            throw new RuntimeException("open uri: %s failed!".formatted(uri), e);
                        }
                    })
            );

        }
    }

    /**
     * URI部分
     *
     * @param name     名称
     * @param uri      URI
     * @param filename 资源名
     */
    private record UriPart(String name, URI uri, String filename) implements Part {

        @Override
        public HttpRequest.BodyPublisher body(String boundary) {

            // header
            final var header = """
                    --%s
                    Content-Disposition: form-data; name="%s"; filename="%s"
                    Content-Type: application/octet-stream
                    
                    """
                    .formatted(boundary, name, filename)
                    .replaceAll("\n", "\r\n");

            // header & input
            return HttpRequest.BodyPublishers.concat(
                    HttpRequest.BodyPublishers.ofString(header, UTF_8),
                    HttpRequest.BodyPublishers.ofInputStream(() -> {
                        try {
                            return uri.toURL().openStream();
                        } catch (IOException e) {
                            throw new RuntimeException("open uri: %s failed!".formatted(uri), e);
                        }
                    })
            );

        }

    }

    /**
     * 结束部分
     */
    private record FinishPart() implements Part {

        @Override
        public HttpRequest.BodyPublisher body(String boundary) {
            final var body = """
                    
                    --%s--
                    """
                    .formatted(boundary)
                    .replaceAll("\n", "\r\n");
            return HttpRequest.BodyPublishers.ofString(body, UTF_8);
        }

    }

}
