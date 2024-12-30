package io.github.oldmanpushcart.dashscope4j.internal.api.audio.vocabulary;

import io.github.oldmanpushcart.dashscope4j.Model;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

@Value
@Accessors(fluent = true)
public class VocabularyModel implements Model {

    String name;
    URI remote;

    public static final VocabularyModel SPEECH_BIASING = new VocabularyModel(
            "speech-biasing",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/audio/asr/customization")
    );

}
