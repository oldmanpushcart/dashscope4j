package io.github.ompc.dashscope4j.chat;

import java.util.Map;

/**
 * 聊天插件
 *
 * @param name      插件名称
 * @param arguments 插件参数
 */
public record ChatPlugin(String name, Map<String, Object> arguments) {

    /**
     * OCR 插件
     */
    public static final ChatPlugin OCR = new ChatPlugin("ocr", Map.of());

    /**
     * 计算器插件
     */
    public static final ChatPlugin CALCULATOR = new ChatPlugin("calculator", Map.of());

    /**
     * 文生图插件
     */
    public static final ChatPlugin TEXT_TO_IMAGE = new ChatPlugin("text_to_image", Map.of());

    /**
     * PDF 解析插件
     */
    public static final ChatPlugin PDF_EXTRACTER = new ChatPlugin("pdf_extracter", Map.of());

}
