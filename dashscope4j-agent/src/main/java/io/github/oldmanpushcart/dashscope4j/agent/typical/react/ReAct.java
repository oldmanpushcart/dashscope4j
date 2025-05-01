package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ReAct模式
 */
@Data
class ReAct {

    public static final String NAME_OBSERVATION = "Observation";

    private static final Set<State> waitingParseStates = new HashSet<>(Arrays.asList(
            State.THOUGHT,
            State.OBSERVATION,
            State.ACTION,
            State.ACTION_INPUT,
            State.FINAL_ANSWER
    ));

    private String thought;
    private String observation;
    private String action;
    private String actionInput;
    private String finalAnswer;

    /**
     * @return 是否有最终答案
     */
    public boolean hasFinalAnswer() {
        return finalAnswer != null;
    }

    /**
     * @return 是否有动作
     */
    public boolean hasAction() {
        return action != null;
    }

    /**
     * 解析ReAct
     *
     * @param text 文本
     * @return ReAct
     */
    public static ReAct valueOf(String text) {
        final ReAct reAct = new ReAct();
        final AtomicReference<State> stateRef = new AtomicReference<>(State.NONE);
        final StringBuilder stringBuf = new StringBuilder();
        try (final Scanner scanner = new Scanner(text)) {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                final Optional<State> stateOpt = waitingParseStates.stream()
                        .filter(state -> line.startsWith(state.prefix))
                        .findFirst();
                if (stateOpt.isPresent()) {
                    switchStateAndSetReAct(stateRef, stateOpt.get(), reAct, stringBuf);
                    stringBuf.setLength(0);
                    stringBuf.append(line.substring(line.indexOf(":") + 1).trim());
                } else {
                    stringBuf.append("\n");
                    stringBuf.append(line);
                }
            }
            switchStateAndSetReAct(stateRef, State.NONE, reAct, stringBuf);
        }
        return reAct;
    }

    // 状态切换并设置ReAct
    private static void switchStateAndSetReAct(AtomicReference<State> stateRef, State next, ReAct reAct, StringBuilder stringBuf) {
        final State current = stateRef.get();
        if (current != State.NONE) {
            switch (current) {
                case THOUGHT:
                    reAct.thought = stringBuf.toString();
                    break;
                case OBSERVATION:
                    reAct.observation = stringBuf.toString();
                    break;
                case ACTION:
                    reAct.action = stringBuf.toString();
                    break;
                case ACTION_INPUT:
                    reAct.actionInput = stringBuf.toString();
                    break;
                case FINAL_ANSWER:
                    reAct.finalAnswer = stringBuf.toString();
                    break;
                default:
                    throw new IllegalStateException("Unreachable state: " + current);
            }
        }
        stateRef.set(next);
    }

    /**
     * 解析状态
     */
    @AllArgsConstructor
    private enum State {
        NONE("None:"),
        THOUGHT("Thought:"),
        OBSERVATION("Observation:"),
        ACTION("Action:"),
        ACTION_INPUT("Action Input:"),
        FINAL_ANSWER("Final Answer:");
        private final String prefix;
    }

}
