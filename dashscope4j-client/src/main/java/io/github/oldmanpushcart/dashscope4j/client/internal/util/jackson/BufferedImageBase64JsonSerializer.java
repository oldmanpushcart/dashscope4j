package io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class BufferedImageBase64JsonSerializer extends JsonSerializer<BufferedImage> {

    @Override
    public void serialize(BufferedImage image, JsonGenerator generator, SerializerProvider provider) throws IOException {

        if (image == null) {
            generator.writeNull();
            return;
        }

        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 将 BufferedImage 写入字节数组（指定格式）
            boolean success = ImageIO.write(image, "JPG", baos);
            if (!success) {
                throw new IOException("ImageIO does not support format: JPG");
            }

            // 转为 Base64 字符串
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

            // 写入 JSON 字符串
            generator.writeString(base64Image);

        }

    }

}
