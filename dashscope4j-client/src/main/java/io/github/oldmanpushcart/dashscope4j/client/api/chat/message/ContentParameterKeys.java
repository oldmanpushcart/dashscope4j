package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters.SimpleParameterKey;

public interface ContentParameterKeys {

    /**
     * FPS
     * <p>每秒抽帧数。取值范围为 [0.1, 10]，默认值为2.0</p>
     * <p>
     * 有两个功能：
     *     <ul>
     *         <li>输入视频文件时，控制抽帧频率，每{@code 1/fps}秒抽取一帧。</li>
     *         <li>告知模型相邻帧之间的时间间隔，帮助其更好地理解视频的时间动态。同时适用于输入视频文件与图像列表时。该功能同时支持视频文件和图像列表输入，适用于事件时间定位或分段内容摘要等场景。</li>
     *     </ul>
     * </p>
     * <p>较大的fps适合高速运动的场景（如体育赛事、动作电影等），较小的fps适合长视频或内容偏静态的场景。</p>
     */
    SimpleParameterKey<Float> FPS = new SimpleParameterKey<>("fps", Float.class);

    /**
     * MIN_PIXELS
     * <p>设定输入图像或视频帧的最小像素阈值。</p>
     * <p>当输入图像或视频帧的像素小于{@code min_pixels}时，会将其进行放大，直到总像素高于{@code min_pixels}</p>
     */
    SimpleParameterKey<Integer> MIN_PIXELS = new SimpleParameterKey<>("min_pixels", Integer.class);


    /**
     * MAX_PIXELS
     * <p>设定输入图像或视频帧的最大像素阈值。</p>
     * <p>
     * 当输入图像或视频的像素在{@code [min_pixels, max_pixels]}区间内时，模型会按原图进行识别。
     * 当输入图像像素大于{@code max_pixels}时，会将图像进行缩小，直到总像素低于{@code max_pixels}。
     * </p>
     */
    SimpleParameterKey<Integer> MAX_PIXELS = new SimpleParameterKey<>("max_pixels", Integer.class);


    /**
     * TOTAL_PIXELS
     * <p>限制从视频中抽取的所有帧的总像素（单帧图像像素 × 总帧数）</p>
     * <p>
     * 如果视频总像素超过此限制，系统将对视频帧进行缩放，但仍会确保单帧图像的像素值在{@code [min_pixels, max_pixels]}范围内。
     * </p>
     */
    SimpleParameterKey<Integer> TOTAL_PIXELS = new SimpleParameterKey<>("total_pixels", Integer.class);

    /**
     * CACHE_CONTROL
     * <p>支持显式缓存的模型支持，用于开启显式缓存。</p>
     * <p>
     * 缓存控制用于控制模型对输入数据的缓存行为。
     * </p>
     */
    SimpleParameterKey<CacheControl> CACHE_CONTROL = new SimpleParameterKey<>("cache_control", CacheControl.class);

    /**
     * CACHE_CONTROL
     * <p>支持显式缓存的模型支持，用于开启显式缓存。</p>
     * <p>
     * 缓存控制用于控制模型对输入数据的缓存行为。
     * </p>
     */
    record CacheControl(@JsonProperty("type") Type type) {

        enum Type {
            @JsonProperty("ephemeral")
            EPHEMERAL
        }

    }

}
