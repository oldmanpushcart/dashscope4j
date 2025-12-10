package io.github.oldmanpushcart.dashscope4j.client.util;

import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.http.HttpClient;

public interface OpBuildable<T, B extends Buildable<T, B>> extends Buildable<T, B> {

    B ak(String ak);

    B http(HttpClient http);

}
