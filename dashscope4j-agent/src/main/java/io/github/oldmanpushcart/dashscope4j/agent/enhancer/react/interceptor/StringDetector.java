package io.github.oldmanpushcart.dashscope4j.agent.enhancer.react.interceptor;

/**
 * 检测字符串
 */
class StringDetector {

    private final char[] chars;
    private int index;
    private boolean detected;

    public StringDetector(String target) {
        this.chars = target.toCharArray();
    }

    public void reset() {
        index = 0;
        detected = false;
    }

    /**
     * 检测字符串
     *
     * @param text 待检测的字符串
     * @return 发现的位置，-1表示未发现
     */
    public int detect(String text) {
        int position = 0;
        if (detected) {
            return position;
        }
        final char[] textCharArray = text.toCharArray();
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
