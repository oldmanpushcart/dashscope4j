package io.github.oldmanpushcart.internal.dashscope4j.base.tokenize.local;

import io.github.oldmanpushcart.dashscope4j.base.tokenizer.Tokenizer;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LocalTokenizer implements Tokenizer {

    private static final QwenTokenizer instance = new QwenTokenizer();

    @Override
    public CompletableFuture<List<Map.Entry<Integer, String>>> encode(String text) {
        final var list = instance.encodeOrdinary(text.trim()).stream()
                .<Map.Entry<Integer, String>>map(token ->
                        new AbstractMap.SimpleEntry<>(
                                token,
                                instance.mapping(token)
                        ))
                .toList();
        return CompletableFuture.completedFuture(list);
    }

    @Override
    public CompletableFuture<List<Map.Entry<Integer, String>>> encode(List<Message> messages) {
        final var text = messages.stream()
                .map(Message::text)
                .reduce("", String::concat);
        return encode(text);
    }

    @Override
    public CompletableFuture<String> decode(List<Integer> tokens) {
        final var text = instance.decode(tokens);
        return CompletableFuture.completedFuture(text);
    }

    @Override
    public boolean isDecodeSupported() {
        return true;
    }

}
