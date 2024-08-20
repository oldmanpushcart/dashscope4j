package io.github.oldmanpushcart.internal.dashscope4j.base.tokenize.remote;

import io.github.oldmanpushcart.dashscope4j.base.tokenizer.Tokenizer;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiExecutor;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.check;

public class RemoteTokenizer implements Tokenizer {

    private final ApiExecutor executor;
    private final ChatModel model;

    public RemoteTokenizer(ApiExecutor executor, ChatModel model) {
        this.executor = executor;
        this.model = model;
    }

    @Override
    public CompletionStage<List<Map.Entry<Integer, String>>> encode(String text) {
        return encode(List.of(Message.ofUser(text)));
    }

    @Override
    public CompletionStage<List<Map.Entry<Integer, String>>> encode(List<Message> messages) {
        final var request = TokenizeRequest.newBuilder()
                .model(model)
                .messages(messages)
                .build();
        return executor.async(request)
                .thenApply(response -> {
                    final var tokenIds = response.output().tokenIds();
                    final var tokens = response.output().tokens();
                    final var total = check(tokenIds.size(), v -> v == tokens.size(), "illegal response format: token-ids and tokens size not match!");
                    return IntStream.range(0, total)
                            .<Map.Entry<Integer, String>>mapToObj(index ->
                                    new AbstractMap.SimpleEntry<>(
                                            tokenIds.get(index),
                                            tokens.get(index)
                                    ))
                            .toList();
                });
    }

    @Override
    public CompletionStage<String> decode(List<Integer> tokens) {
        throw new UnsupportedOperationException("remote tokenizer does not support decode!");
    }

    @Override
    public boolean isDecodeSupported() {
        return false;
    }

}
