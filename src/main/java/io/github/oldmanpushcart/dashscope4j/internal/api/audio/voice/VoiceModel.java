package io.github.oldmanpushcart.dashscope4j.internal.api.audio.voice;

import io.github.oldmanpushcart.dashscope4j.Model;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

@Value
@Accessors(fluent = true)
public class VoiceModel implements Model {

    String name;
    URI remote;

    public static final VoiceModel VOICE_ENROLLMENT = new VoiceModel(
            "voice-enrollment",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/audio/tts/customization"
            ));

}
