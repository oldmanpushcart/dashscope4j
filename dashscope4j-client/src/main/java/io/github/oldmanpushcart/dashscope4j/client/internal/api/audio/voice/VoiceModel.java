package io.github.oldmanpushcart.dashscope4j.client.internal.api.audio.voice;

import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.net.URI;

public interface VoiceModel extends Model {

    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    class BaseVoiceModel extends BaseModel implements VoiceModel {

        public BaseVoiceModel(String name, URI remote, Option option) {
            super(name, remote, option);
        }

        public BaseVoiceModel(String name, URI remote) {
            super(name, remote);
        }

    }

    VoiceModel VOICE_ENROLLMENT = new BaseVoiceModel(
            "voice-enrollment",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/audio/tts/customization"
            ));

}
