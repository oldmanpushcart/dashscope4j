package io.github.oldmanpushcart.dashscope4j.client.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * 从字符串数组反序列化为 Throwable
 * <p>
 * 期望的 JSON 格式：
 * <pre>{@code
 * ["ExceptionType: message1", "CauseType: message2", ...]
 * }</pre>
 * </p>
 */
public class ThrowableAsStringDeserializer extends JsonDeserializer<Throwable> {

    @Override
    public Throwable deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        final var token = parser.currentToken();
        
        // 处理 null 值
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        
        // 必须是数组
        if (token != JsonToken.START_ARRAY) {
            context.reportWrongTokenException(
                    Throwable.class,
                    JsonToken.START_ARRAY,
                    "Expected as string array for exception chain!"
            );
        }
        
        // 读取数组中的所有字符串
        final var messages = new java.util.ArrayList<String>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            final var message = parser.getValueAsString();
            if (message != null && !message.isEmpty()) {
                messages.add(message);
            }
        }
        
        // 如果数组为空，返回 null
        if (messages.isEmpty()) {
            return null;
        }
        
        // 从后往前构建异常链（最后一个是最根本的原因）
        Throwable cause = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            final var currentMessage = messages.get(i);
            final var exception = createException(currentMessage, cause);
            cause = exception;
        }
        
        return cause;
    }
    
    /**
     * 根据消息字符串创建异常对象
     * 
     * @param message 格式为 "ExceptionType: message" 或纯消息
     * @param cause   原因异常
     * @return 创建的异常对象
     */
    private Throwable createException(String message, Throwable cause) {
        // 尝试解析 "ExceptionType: message" 格式
        final int colonIndex = message.indexOf(':');
        if (colonIndex > 0 && colonIndex < message.length() - 1) {
            final var exceptionType = message.substring(0, colonIndex).trim();
            final var exceptionMessage = message.substring(colonIndex + 1).trim();
            
            // 尝试根据类型创建对应的异常
            final var exception = createExceptionByType(exceptionType, exceptionMessage, cause);
            if (exception != null) {
                return exception;
            }
            
            // 如果无法识别类型，使用 RuntimeException
            return new RuntimeException(exceptionMessage, cause);
        }
        
        // 纯消息格式，使用 RuntimeException
        return new RuntimeException(message, cause);
    }
    
    /**
     * 根据异常类型名称创建对应的异常实例
     * 
     * @param typeName 异常类型简单名称
     * @param message  异常消息
     * @param cause    原因异常
     * @return 创建的异常实例，如果类型不支持则返回 null
     */
    private Throwable createExceptionByType(String typeName, String message, Throwable cause) {
        // 只支持常见的运行时异常类型
        return switch (typeName) {
            case "RuntimeException" -> new RuntimeException(message, cause);
            case "IllegalArgumentException" -> new IllegalArgumentException(message, cause);
            case "IllegalStateException" -> new IllegalStateException(message, cause);
            case "NullPointerException" -> new NullPointerException(message);
            case "IOException" -> new java.io.IOException(message, cause);
            case "SecurityException" -> new SecurityException(message, cause);
            case "UnsupportedOperationException" -> new UnsupportedOperationException(message, cause);
            default -> null; // 不支持的类型，返回 null 让调用方使用默认异常
        };
    }
}
