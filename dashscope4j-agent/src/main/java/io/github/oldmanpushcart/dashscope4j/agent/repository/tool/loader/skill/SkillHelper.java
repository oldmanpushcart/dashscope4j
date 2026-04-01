package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

class SkillHelper {

    public static String toToolName(String skillName) {
        return "skill$" + skillName;
    }

}
