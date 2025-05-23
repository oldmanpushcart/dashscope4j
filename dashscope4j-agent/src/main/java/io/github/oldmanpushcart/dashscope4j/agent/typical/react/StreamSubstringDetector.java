package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

/**
 * 检测流式输出的子串
 */
class StreamSubstringDetector {

    private final char[] chars;
    private int index;
    private boolean detected;

    /**
     * 创建检测器
     *
     * @param target 目标子串
     */
    public StreamSubstringDetector(String target) {
        this.chars = target.toCharArray();
    }

    /**
     * 重置检测器
     */
    public void reset() {
        index = 0;
        detected = false;
    }

    /**
     * 向检测器提供一个新的字符串片段进行处理
     *
     * @param chunk 新到达的字符串片段
     * @return 最终检测到在当前字符串片段的位置，-1表示未发现
     */
    public int feed(String chunk) {
        int position = 0;
        if (detected) {
            return position;
        }
        final char[] textCharArray = chunk.toCharArray();
        for (; position < textCharArray.length; position++) {
            final char textChar = textCharArray[position];
            if (textChar == chars[index]) {
                index++;
                if (index == chars.length) {
                    break;
                }
            } else {
                index = 0;
                if (textChar == chars[index]) {
                    index++;
                }
            }
        }

        //noinspection AssignmentUsedAsCondition
        return (detected = index == chars.length)
                ? position + 1
                : -1;
    }

}
