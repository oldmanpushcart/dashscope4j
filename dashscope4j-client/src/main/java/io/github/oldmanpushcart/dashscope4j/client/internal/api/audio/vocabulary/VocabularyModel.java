package io.github.oldmanpushcart.dashscope4j.client.internal.api.audio.vocabulary;

import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.net.URI;

public interface VocabularyModel extends Model {

    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    class BaseVocabularyModel extends BaseModel implements VocabularyModel {

        public BaseVocabularyModel(String name, URI remote, Option option) {
            super(name, remote, option);
        }

        public BaseVocabularyModel(String name, URI remote) {
            super(name, remote);
        }

    }

    VocabularyModel SPEECH_BIASING = new BaseVocabularyModel(
            "speech-biasing",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/audio/asr/customization")
    );

}
