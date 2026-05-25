package io.github.oldmanpushcart.dashscope4j.client.internal.base.tokenizer.local;

import io.github.oldmanpushcart.dashscope4j.client.base.tokenizer.Tokenizer;

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

}
