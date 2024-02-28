package io.github.oldmanpushcart.internal.dashscope4j.util;

/**
 * 特征检测
 */
public class FeatureDetection {

    private final byte[] feature;
    private boolean isDetected = false;
    private int index = 0;

    /**
     * 特征检测
     *
     * @param feature 特征
     */
    public FeatureDetection(byte[] feature) {
        this.feature = feature;
    }

    private void screeningByte(final byte b) {
        if (b != feature[index]) {
            reset();
            isDetected = false;
            return;
        }
        if (++index == feature.length) {
            reset();
            isDetected = true;
        }
    }

    private void reset() {
        index = 0;
    }

    /**
     * 检测
     *
     * @param bytes  检测目标
     * @param offset 偏移量
     * @param length 长度
     * @return 检测位置
     */
    public int screening(final byte[] bytes, int offset, int length) {
        int position = -1;
        for (int index = offset; index < offset + length; index++) {
            screeningByte(bytes[index]);
            if (isDetected) {
                position = index;
                break;
            }
        }
        return position;
    }

}
