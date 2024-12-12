package io.github.oldmanpushcart.internal.dashscope4j.base.tokenizer.local;

import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.base.tokenizer.Tokenizer;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class LocalTokenizer implements Tokenizer {

    private static final QwenTokenizer instance = new QwenTokenizer();

    @Override
    public CompletionStage<List<Map.Entry<Integer, String>>> encode(String text) {
        final List<Map.Entry<Integer, String>> list = instance.encodeOrdinary(text.trim()).stream()
                .<Map.Entry<Integer, String>>map(token ->
                        new AbstractMap.SimpleEntry<>(
                                token,
                                instance.mapping(token)
                        ))
                .collect(Collectors.toList());
        return CompletableFuture.completedFuture(list);
    }

    @Override
    public CompletionStage<List<Map.Entry<Integer, String>>> encode(List<Message> messages) {
        final String text = messages.stream()
                .map(Message::text)
                .reduce("", String::concat);
        return encode(text);
    }

    @Override
    public CompletionStage<String> decode(List<Integer> tokens) {
        final String text = instance.decode(tokens);
        return CompletableFuture.completedFuture(text);
    }

    @Override
    public boolean isDecodeSupported() {
        return true;
    }

}
