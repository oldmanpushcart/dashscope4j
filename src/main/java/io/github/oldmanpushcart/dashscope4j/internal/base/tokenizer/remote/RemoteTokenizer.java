package io.github.oldmanpushcart.dashscope4j.internal.base.tokenizer.remote;

import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.base.tokenizer.Tokenizer;
import lombok.AllArgsConstructor;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;

@AllArgsConstructor
public class RemoteTokenizer implements Tokenizer {

    private final ApiOp apiOp;
    private final ChatModel model;

    @Override
    public CompletionStage<List<Map.Entry<Integer, String>>> encode(String text) {
        return encode(singletonList(Message.ofUser(text)));
    }

    @Override
    public CompletionStage<List<Map.Entry<Integer, String>>> encode(List<Message> messages) {
        final TokenizeRequest request = TokenizeRequest.newBuilder()
                .model(model)
                .addMessages(messages)
                .build();
        return apiOp.executeAsync(request)
                .thenApply(response -> {

                    final List<Integer> tokenIds = response.output().tokenIds();
                    final List<String> tokens = response.output().tokens();
                    if (tokenIds.size() != tokens.size()) {
                        throw new IllegalArgumentException("token-ids and tokens size not match!");
                    }

                    return IntStream.range(0, tokenIds.size())
                            .mapToObj(index ->
                                    new AbstractMap.SimpleEntry<>(
                                            tokenIds.get(index),
                                            tokens.get(index)
                                    ))
                            .collect(Collectors.toList());
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
