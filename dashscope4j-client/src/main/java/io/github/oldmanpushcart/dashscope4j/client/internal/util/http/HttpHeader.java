package io.github.oldmanpushcart.dashscope4j.client.internal.util.http;


import io.github.oldmanpushcart.dashscope4j.client.internal.util.codec.FeatureCodec;

import java.net.http.HttpHeaders;
import java.nio.charset.Charset;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * HTTP头
 */
public class HttpHeader {

    /**
     * HTTP-HEADER: Content-Type
     */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * HTTP Content-Type
     *
     * @param mime       MIME
     * @param parameters 参数
     */
    public record ContentType(String mime, Map<String, String> parameters) {
        public static final String MIME_APPLICATION_JSON = "application/json";
        public static final String MIME_TEXT_EVENT_STREAM = "text/event-stream";
        private static final FeatureCodec codec = new FeatureCodec(';', '=');

        /**
         * 获取字符集
         *
         * @return 字符集
         */
        public Charset charset() {
            return Charset.forName(parameters.getOrDefault("charset", "UTF-8"));
        }

        /**
         * 解析Content-Type
         *
         * @param headers HTTP头
         * @return Content-Type
         */
        public static ContentType parse(HttpHeaders headers) {
            return headers.firstValue("content-type")
                    .map(ct -> {

                        // mime only
                        if (!ct.contains(";")) {
                            return new ContentType(ct, emptyMap());
                        }

                        // mime with parameters
                        return new ContentType(
                                parseMime(ct),
                                parseParameterMap(ct)
                        );

                    })
                    .orElseGet(() -> new ContentType(MIME_APPLICATION_JSON, emptyMap()));
        }

        // 解析 MIME
        private static String parseMime(String ct) {
            return ct.substring(0, ct.indexOf(";")).trim();
        }

        // 解析参数
        private static Map<String, String> parseParameterMap(String ct) {
            return codec.toMap(ct.substring(ct.indexOf(";") + 1).trim());
        }

    }

}
