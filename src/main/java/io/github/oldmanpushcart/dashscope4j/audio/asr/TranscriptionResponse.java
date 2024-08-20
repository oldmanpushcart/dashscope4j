package io.github.oldmanpushcart.dashscope4j.audio.asr;

import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.audio.asr.timespan.SentenceTimeSpan;
import io.github.oldmanpushcart.dashscope4j.base.algo.HttpAlgoResponse;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * 语音转录应答
 *
 * @since 2.2.0
 */
public interface TranscriptionResponse extends HttpAlgoResponse<TranscriptionResponse.Output> {

    /**
     * 输出结果
     */
    interface Output {

        /**
         * @return 转录记录集合
         */
        List<Item> results();

    }

    /**
     * 转录记录
     */
    interface Item {

        /**
         * @return 转录结果
         */
        Ret ret();

        /**
         * @return 原始音频文件地址
         */
        URI originURI();

        /**
         * @return 转录结果文件地址
         */
        URI transcriptionURI();

        /**
         * 懒加载获取转录结果
         *
         * @param executor       线程池
         * @param connectTimeout 连接超时时间
         * @param timeout        读取超时时间
         * @return 转录结果
         */
        CompletionStage<Transcription> lazyFetchTranscription(Executor executor, Duration connectTimeout, Duration timeout);

        /**
         * @return 懒加载获取转录结果
         */
        default CompletionStage<Transcription> lazyFetchTranscription() {
            return lazyFetchTranscription(null, null, null);
        }

    }

    /**
     * 转录结果
     */
    interface Transcription {

        /**
         * @return 原始音视频文件地址
         */
        URI originURI();

        /**
         * @return 原始音视频元数据
         */
        Meta meta();

        /**
         * @return 转录片段集合
         */
        List<Transcript> transcripts();

        /**
         * 音视频元数据
         */
        interface Meta {

            /**
             * @return 采样率
             */
            int sampleRate();

            /**
             * @return 音视频编码格式
             */
            String format();

            /**
             * @return 音视频时长
             */
            Duration duration();

            /**
             * @return 音视频通道集合
             */
            List<Integer> channels();

        }

    }

    /**
     * 转录片段
     */
    interface Transcript {

        /**
         * @return 音视频通道
         */
        int channel();

        /**
         * @return 片段时长
         */
        Duration duration();

        /**
         * @return 片段文本
         */
        String text();

        /**
         * @return 整句时间片集合
         */
        List<SentenceTimeSpan> sentences();

    }


}
