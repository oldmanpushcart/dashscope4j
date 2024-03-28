package io.github.oldmanpushcart.dashscope4j.embeddingx.mm;

import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoOptions;

/**
 * 多模态向量计算选项
 *
 * @since 1.3.0
 */
public interface MmEmbeddingOptions extends AlgoOptions {

    /**
     * AUTO_TRUNCATION
     * <p>是否自动截断</p>
     * <p>
     * 超过以下限制时会发生截断
     * <li>图像格式目前支持：bmp, jpg, jpeg, png 和 tiff；文件大小不超过5M</li>
     * <li>语音格式目前支持 wav, mp3 和 flac；文件大小不超过5M，最大音频时长为15s</li>
     * <li>文本长度为70字</li>
     * </p>
     */
    Option.SimpleOpt<Boolean> AUTO_TRUNCATION = new Option.SimpleOpt<>("auto_truncation", Boolean.class);

}
