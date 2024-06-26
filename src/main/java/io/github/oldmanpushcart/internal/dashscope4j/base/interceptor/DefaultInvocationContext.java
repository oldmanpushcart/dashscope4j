package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;

import java.util.concurrent.Executor;

record DefaultInvocationContext(DashScopeClient client, Executor executor) implements InvocationContext {

}
