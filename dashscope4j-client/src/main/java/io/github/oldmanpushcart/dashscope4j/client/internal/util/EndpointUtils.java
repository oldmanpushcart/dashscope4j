package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class EndpointUtils {

    public static URI appendQueryParams(URI endpoint, Map<String, String> params) {

        if (params == null || params.isEmpty()) {
            return endpoint;
        }

        final StringBuilder queryBuilder = new StringBuilder();

        // 1. 保留原始查询字符串（如果存在）
        final String originalQuery = endpoint.getRawQuery();
        if (originalQuery != null && !originalQuery.isEmpty()) {
            queryBuilder.append(originalQuery);
        }

        // 2. 追加新参数
        params.forEach((key, value) -> {

            // 如果是第一个出现的参数，则添加 "?"
            if (!queryBuilder.isEmpty()) {
                queryBuilder.append('&');
            }

            final String encodedKey = URLEncoder.encode(key, UTF_8);
            final String encodedVal = value == null ? "" : URLEncoder.encode(value, UTF_8);
            queryBuilder
                    .append(encodedKey)
                    .append('=')
                    .append(encodedVal);

        });

        // 3. 构建新 URI（复用原 URI 的其他部分）
        try {
            return new URI(
                    endpoint.getScheme(),
                    endpoint.getAuthority(), // 包含 user-info, host, port
                    endpoint.getPath(),
                    queryBuilder.toString(),
                    endpoint.getFragment()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException("append query params error!", e);
        }
    }

    public static URI https(String host, String path) {
        final var stringBuf = new StringBuilder("https://");
        stringBuf.append(host);
        if(!path.startsWith("/")) {
            stringBuf.append("/");
        }
        stringBuf.append(path);
        return URI.create(stringBuf.toString());
    }

    public static URI wss(String host, String path) {
        final var stringBuf = new StringBuilder("wss://");
        stringBuf.append(host);
        if(!path.startsWith("/")) {
            stringBuf.append("/");
        }
        stringBuf.append(path);
        return URI.create(stringBuf.toString());
    }

}
