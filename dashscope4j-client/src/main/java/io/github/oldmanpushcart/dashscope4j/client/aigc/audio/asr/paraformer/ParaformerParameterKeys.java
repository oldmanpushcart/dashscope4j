package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters.SimpleParameterKey;

public interface ParaformerParameterKeys {

    SimpleParameterKey<Integer> SAMPLE_RATE = new SimpleParameterKey<>("sample_rate", Integer.class);
    SimpleParameterKey<String> VOCABULARY_ID = new SimpleParameterKey<>("vocabulary_id", String.class);
    SimpleParameterKey<Boolean> DISFLUENCY_REMOVE_ENABLED = new SimpleParameterKey<>("disfluency_remove_enabled", Boolean.class);
    SimpleParameterKey<String[]> LANGUAGE_HINTS = new SimpleParameterKey<>("language_hints", String[].class);
    SimpleParameterKey<Boolean> SEMANTIC_PUNCTUATION_ENABLED = new SimpleParameterKey<>("semantic_punctuation_enabled", Boolean.class);
    SimpleParameterKey<Integer> MAX_SENTENCE_SILENCE = new SimpleParameterKey<>("max_sentence_silence", Integer.class);
    SimpleParameterKey<Boolean> MULTI_THRESHOLD_MODE_ENABLED = new SimpleParameterKey<>("multi_threshold_mode_enabled", Boolean.class);
    SimpleParameterKey<Boolean> PUNCTUATION_PREDICTION_ENABLED = new SimpleParameterKey<>("punctuation_prediction_enabled", Boolean.class);
    SimpleParameterKey<Boolean> HEARTBEAT = new SimpleParameterKey<>("heartbeat", Boolean.class);
    SimpleParameterKey<Boolean> INVERSE_TEXT_NORMALIZATION_ENABLED = new SimpleParameterKey<>("inverse_text_normalization_enabled", Boolean.class);

}
