package io.github.oldmanpushcart.dashscope4j.agent.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件工具类
 * 提供文件路径安全校验、编码检测、二进制文件识别等通用功能
 */
public class FileUtils {

    /**
     * 检查并解析路径，防止路径穿越攻击
     *
     * @param workspace 工作区根路径（支持相对路径，会自动转换为绝对路径）
     * @param userPath  用户提供的相对路径
     * @return 解析后的绝对路径
     * @throws SecurityException 如果路径非法或尝试穿越
     */
    public static Path checkPathEscape(Path workspace, String userPath) {
        // 将 workspace 转换为绝对路径并规范化
        Path absoluteWorkspace = workspace.toAbsolutePath().normalize();
        
        // 拒绝绝对路径，防止路径穿越
//        if (Path.of(userPath).isAbsolute()) {
//            throw new SecurityException("拒绝访问：不支持绝对路径：" + userPath);
//        }

        // 解析用户路径并规范化
        Path resolved = absoluteWorkspace.resolve(userPath).normalize();

        // 双重验证：确保解析后的路径仍在工作目录内
        if (!resolved.startsWith(absoluteWorkspace)) {
            throw new SecurityException("拒绝访问：路径超出工作目录范围：" + userPath);
        }

        return resolved;
    }

    /**
     * 检测是否为二进制文件
     * 通过魔数和控制字符比例综合判断
     *
     * @param file 文件路径
     * @return true 如果是二进制文件，false 如果是文本文件
     * @throws IOException 如果读取文件失败
     */
    public static boolean isBinaryFile(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            byte[] header = new byte[80];
            int bytesRead = is.read(header);
            if (bytesRead == -1) {
                return false; // 空文件视为文本文件
            }

            // 检查常见的二进制文件魔数（扩展支持更多格式）
            if (bytesRead >= 4) {
                int magic = ((header[0] & 0xFF) << 24) |
                        ((header[1] & 0xFF) << 16) |
                        ((header[2] & 0xFF) << 8) |
                        (header[3] & 0xFF);

                // ZIP/JAR/PNG/GIF/PDF/Class 等格式
                switch (magic) {
                    case 0x504B0304: // ZIP/JAR
                    case 0x89504E47: // PNG
                    case 0x47494638: // GIF
                    case 0x25504446: // PDF (%PDF)
                    case 0xCAFEBABE: // Java Class
                        return true;
                }

                // 检查 JPEG (FFD8FF)
                if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                    return true;
                }

                // 检查 MP3 (ID3)
                if (header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
                    return true;
                }
            }

            // 尝试将字节解码为 UTF-8 字符串，如果能成功解码则为文本文件
            try {
                String content = new String(header, 0, bytesRead, StandardCharsets.UTF_8);

                // 统计控制字符比例（排除常见的空白字符）
                int controlChars = 0;
                int nonWhitespaceChars = 0;
                for (int i = 0; i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (!Character.isWhitespace(c)) {
                        nonWhitespaceChars++;
                        // 控制字符且不是制表符、换行、回车
                        if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                            controlChars++;
                        }
                    }
                }

                // 如果没有非空白字符，视为文本文件
                if (nonWhitespaceChars == 0) {
                    return false;
                }

                // 如果控制字符超过非空白字符的 10%，则可能是二进制文件
                return (double) controlChars / nonWhitespaceChars > 0.1;

            } catch (Exception e) {
                // 如果 UTF-8 解码失败，可能是二进制文件
                return true;
            }
        }
    }

    /**
     * 检查文件自指定时间戳后是否被修改
     *
     * @param file         文件路径
     * @param lastModified 预期的最后修改时间戳（毫秒）
     * @throws SecurityException 如果文件已被修改
     * @throws IOException       如果读取文件属性失败
     */
    public static void checkFileUnmodified(Path file, long lastModified) throws SecurityException, IOException {
        if (!Files.exists(file)) {
            throw new SecurityException("文件不存在：" + file);
        }

        long currentModified = Files.getLastModifiedTime(file).toMillis();
        if (currentModified != lastModified) {
            throw new SecurityException(
                    String.format("文件已被修改：预期时间戳 %d，当前时间戳 %d", lastModified, currentModified)
            );
        }
    }

}
