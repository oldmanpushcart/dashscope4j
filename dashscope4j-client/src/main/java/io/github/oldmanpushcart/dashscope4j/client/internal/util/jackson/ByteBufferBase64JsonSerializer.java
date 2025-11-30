package io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

public class ByteBufferBase64JsonSerializer extends JsonSerializer<ByteBuffer> {


    @Override
    public void serialize(ByteBuffer buffer, JsonGenerator generator, SerializerProvider provider) throws IOException {

        if (buffer == null) {
            generator.writeNull();
            return;
        }

        // 将 ByteBuffer 转换为字节数组
        byte[] bytes;
        if (buffer.hasArray() && buffer.position() == 0 && buffer.limit() == buffer.capacity()) {
            // 直接使用底层数组（避免复制），前提是 buffer 是 array-backed 且未被 slice/position 修改
            bytes = buffer.array();
        } else {
            // 否则安全地复制数据
            bytes = new byte[buffer.remaining()];
            buffer.asReadOnlyBuffer().get(bytes); // 使用只读副本避免影响原 buffer 的 position
        }

        // 编码为 Base64 字符串
        String base64 = Base64.getEncoder().encodeToString(bytes);

        // 写入 JSON 字符串
        generator.writeString(base64);

    }

}
