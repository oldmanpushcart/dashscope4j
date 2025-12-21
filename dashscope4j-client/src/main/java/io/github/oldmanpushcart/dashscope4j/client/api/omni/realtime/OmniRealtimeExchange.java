package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * OMNI-REALTIME 数据交换接口
 */
public interface OmniRealtimeExchange extends Exchange<OmniRealtimeClientEvent> {

    /**
     * 手动 VAD
     */
    interface ManualVad extends OmniRealtimeExchange {

        /**
         * 创建一个新地输入操作
         * <p>
         * 你可以进行图片和音频的输入。需要注意的是，图像输入必需要在音频输入之后进行
         * </p>
         *
         * @return 输入操作
         */
        CompletionStage<InputOp> newInput();

        /**
         * 输入操作
         */
        interface InputOp {

            /**
             * 输入图片
             *
             * @param image 图片
             * @return 输入操作
             */
            CompletionStage<InputOp> image(BufferedImage image);

            /**
             * 输入音频
             *
             * @param buffer 音频数据
             * @return 输入操作
             */
            CompletionStage<InputOp> audio(ByteBuffer buffer);

            /**
             * 输入音频
             *
             * @param bytes  音频数据
             * @param offset 偏移量
             * @param length 长度
             * @return 输入操作
             */
            CompletionStage<InputOp> audio(byte[] bytes, int offset, int length);

            /**
             * 清空输入
             *
             * @return 输入操作
             */
            CompletionStage<InputOp> clear();

            /**
             * 提交输入
             * <p>
             * 输入被提交之后将无法继续输入，开始转入到响应操作。在响应输入阶段进行响应提交、取消的操作。
             * </p>
             *
             * @return 响应操作
             */
            CompletionStage<ResponseOp> commit();

            /**
             * 取消输入
             * <p>
             * 输入被取消之后将无法继续输入，响应输入将无法继续进行。
             * </p>
             *
             * @return 取消操作
             */
            CompletionStage<Void> cancel();

        }

        /**
         * 响应操作
         */
        interface ResponseOp {

            /**
             * 创建一个响应
             *
             * @return 创建结果
             */
            CompletableFuture<Void> create();

        }

    }

    /**
     * 服务器 VAD
     */
    interface ServerVad extends OmniRealtimeExchange {

        /**
         * 输入图片
         *
         * @param image 图片
         * @return 输入结果
         */
        CompletionStage<Void> image(BufferedImage image);

        /**
         * 输入音频
         *
         * @param buffer 音频数据
         * @return 输入结果
         */
        CompletionStage<Void> audio(ByteBuffer buffer);

        /**
         * 输入音频
         *
         * @param bytes  音频数据
         * @param offset 偏移量
         * @param length 长度
         * @return 输入结果
         */
        CompletionStage<Void> audio(byte[] bytes, int offset, int length);

    }

    /**
     * 交换处理器
     */
    interface Handler extends Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    }

}
