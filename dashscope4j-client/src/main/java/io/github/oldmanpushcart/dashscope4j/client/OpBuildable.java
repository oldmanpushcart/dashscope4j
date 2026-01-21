package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

public interface OpBuildable<T, B extends OpBuildable<T, B>> extends Buildable<T, B> {

    B client(DashscopeClient client);

}
