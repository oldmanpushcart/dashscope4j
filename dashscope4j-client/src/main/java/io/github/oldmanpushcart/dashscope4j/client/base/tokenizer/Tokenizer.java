package io.github.oldmanpushcart.dashscope4j.client.base.tokenizer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface Tokenizer {

    CompletionStage<List<Map.Entry<Integer, String>>> encode(String text);

}
