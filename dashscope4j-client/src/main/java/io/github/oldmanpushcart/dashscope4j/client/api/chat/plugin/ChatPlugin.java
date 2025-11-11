package io.github.oldmanpushcart.dashscope4j.client.api.chat.plugin;

import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * 对话插件
 */
public record ChatPlugin(String name, Map<String, Object> meta) implements Plugin {

    /**
     * OCR 插件
     */
    public static final ChatPlugin OCR = new ChatPlugin("ocr", emptyMap());

    /**
     * 计算器插件
     */
    public static final ChatPlugin CALCULATOR = new ChatPlugin("calculator", emptyMap());

    /**
     * 文生图插件
     */
    public static final ChatPlugin TEXT_TO_IMAGE = new ChatPlugin("text_to_image", emptyMap());

    /**
     * PDF 解析插件
     */
    public static final ChatPlugin PDF_EXTRACTER = new ChatPlugin("pdf_extracter", emptyMap());

}
