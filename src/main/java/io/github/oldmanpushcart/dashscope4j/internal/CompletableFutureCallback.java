package io.github.oldmanpushcart.dashscope4j.internal;

import jakarta.validation.constraints.NotNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class CompletableFutureCallback<T> extends CompletableFuture<T> implements Callback {

    private final Action<T> action;

    public CompletableFutureCallback(Action<T> action) {
        this.action = action;
    }

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        completeExceptionally(e);
    }

    @Override
    public void onResponse(@NotNull Call httpCall, @NotNull Response httpResponse) {
        try {
            final T result = action.action(httpCall, httpResponse);
            complete(result);
        } catch (Throwable ex) {
            completeExceptionally(ex);
        }
    }

    public static CompletableFutureCallback<String> newStringFutureCallback() {
        return new CompletableFutureCallback<>((call, response) ->
                requireNonNull(response.body()).string());
    }

    @FunctionalInterface
    public interface Action<T> {

        T action(Call call, Response response) throws Throwable;

    }

}
