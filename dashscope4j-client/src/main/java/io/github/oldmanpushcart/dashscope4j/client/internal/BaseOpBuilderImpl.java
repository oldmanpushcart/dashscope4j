package io.github.oldmanpushcart.dashscope4j.client.internal;

import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.http.HttpClient;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

public abstract class BaseOpBuilderImpl<T, B extends Buildable<T, B>> implements Buildable<T, B> {

    private String ak;
    private HttpClient http;

    public B ak(String ak) {
        this.ak = requireNonBlankString(ak, "ak must not be blank!");
        return self();
    }

    public B http(HttpClient http) {
        this.http = requireNonNull(http, "http must not be null!");
        return self();
    }

    protected String ak() {
        return requireNonBlankString(ak, "ak is missing!");
    }

    protected HttpClient http() {
        return requireNonNull(http, "http is missing!");
    }

}
