package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.audio.asr.*;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionRequest.LanguageHint;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisModel;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisOptions;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.base.exchange.ExchangeListeners;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.util.FlowPublishers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

@Disabled
public class DebugTestCase implements LoadingEnv {

    @Test
    public void test$debug$synthesis() throws IOException {

        // 文本集合
        final var strings = new String[]{
                "白日依山尽，",
                "黄河入海流。",
                "欲穷千里目，",
                "更上一层楼。"
        };

        // 语音合成请求
        final var request = SpeechSynthesisRequest.newBuilder()
                .model(SpeechSynthesisModel.COSYVOICE_LONGXIAOCHUN_V1)
                .option(SpeechSynthesisOptions.SAMPLE_RATE, 16000)
                .option(SpeechSynthesisOptions.FORMAT, SpeechSynthesisRequest.Format.WAV)
                .build();

        // 以语音合成请求为模板，对每个文本生成一个语音合成请求
        final var requests = Stream.of(strings)
                .map(string -> SpeechSynthesisRequest.newBuilder(request)
                        .text(string)
                        .build()
                )
                .toList();

        // 聚合成请求发布器
        final var requestPublisher = FlowPublishers.fromIterator(requests);

        // 进行语音合成
        client.audio().synthesis(request)

                // 打开语音合成数据交互通道：全双工模式，输出到audio.wav文件
                .exchange(Exchange.Mode.DUPLEX, ExchangeListeners.ofPath(Path.of("./audio.wav")))

                // 发送语音合成请求序列
                .thenCompose(exchange -> exchange.writeDataPublisher(requestPublisher))

                // 语音合成结束
                .thenCompose(Exchange::finishing)

                // 等待通道关闭
                .thenCompose(Exchange::closeFuture)
                .toCompletableFuture()
                .join();

    }

    @Test
    public void test$debug$recognition() throws IOException {

        // 构建音频文件的ByteBuffer发布器
        final var byteBufPublisher = FlowPublishers.fromURI(Path.of("./audio.wav").toUri());

        /*
         * 构建语音识别请求
         * 采样率：16000
         * 音频格式：WAV(PCM)
         */
        final var request = RecognitionRequest.newBuilder()
                .model(RecognitionModel.PARAFORMER_REALTIME_V2)
                .option(RecognitionOptions.SAMPLE_RATE, 16000)
                .option(RecognitionOptions.FORMAT, RecognitionRequest.Format.WAV)
                .build();

        // 识别文本缓存
        final var stringBuf = new StringBuilder();

        // 进行语音识别
        client.audio().recognition(request)

                // 打开语音识别数据交互通道：全双工模式，输出到文本缓存
                .exchange(Exchange.Mode.DUPLEX, ExchangeListeners.ofConsume(response -> {
                    if (response.output().sentence().isEnd()) {
                        stringBuf.append(response.output().sentence().text()).append("\n");
                    }
                }))

                // 发送音频文件字节流数据
                .thenCompose(exchange -> exchange.writeByteBufferPublisher(byteBufPublisher))

                // 语音识别结束
                .thenCompose(Exchange::finishing)

                // 等待通道关闭
                .thenCompose(Exchange::closeFuture)
                .toCompletableFuture()
                .join();

        // 输出识别文本
        System.out.println(stringBuf);

    }

    @Test
    public void test$debug$transcription() {

        /*
         * 构建音视频转录请求
         * 语言：日文
         * 选项：过滤语气词（日片中很多以库以库的语气词，各位懂的都懂）
         */
        final var request = TranscriptionRequest.newBuilder()
                .model(TranscriptionModel.PARAFORMER_V2)
                .resources(List.of(URI.create("https://ompc-storage.oss-cn-hangzhou.aliyuncs.com/dashscope4j/video/%5Bktxp%5D%5BFullmetal%20Alchemist%5D%5Bjap_chn%5D01.rmvb")))
                .option(TranscriptionOptions.ENABLE_DISFLUENCY_REMOVAL, true)
                .option(TranscriptionOptions.LANGUAGE_HINTS, new LanguageHint[]{LanguageHint.JA})
                .build();

        // 进行音视频转录
        final var response = client.audio().transcription(request)

                // 等待任务完成，每隔30s进行检查任务状态
                .task(Task.WaitStrategies.perpetual(Duration.ofMillis(1000L * 30)))
                .toCompletableFuture()
                .join();

        // 合并音视频转录文本（当前只有一个视频）
        final var text = response.output().results().stream()
                .map(result-> {

                    // 下载转录结果
                    final var transcription = result.lazyFetchTranscription()
                            .toCompletableFuture()
                            .join();

                    // 合并转录句子（每行一个句子）
                    return transcription.transcripts().stream()
                            .flatMap(transcript->transcript.sentences().stream())
                            .map(sentence-> "%s - %s: %s".formatted(
                                    sentence.begin(),
                                    sentence.end(),
                                    sentence.text()
                            ))
                            .reduce((a, b) -> a + "\n" + b)
                            .orElse("");

                })

                // 合并多个音视频转录文本，当前只有一个视频
                .reduce((a, b) -> a + b)
                .orElse("");

        // 输出音视频转录文本
        System.out.println(text);

    }

    @Test
    public void test$debug() {

        final var messages = List.of(
                Message.ofUser("北京有哪些好玩地方？"),
                Message.ofAi("故宫、颐和园、天坛等都是可以去游玩的景点哦。"),
                Message.ofUser("帮我安排一些行程")
        );

        final var list = client.base().tokenize().local()
                .encode(messages)
                .toCompletableFuture()
                .join();

        System.out.println("total tokens: " + list.size());


    }

}
