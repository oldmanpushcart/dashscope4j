请校验原始目标是否已达成:

## 原始任务
{{original_task}}

## 已执行的步骤及结果
{{step_results}}

请判断:
1. 原始目标是否完全达成?(是/否)
2. 如果未达成,还缺少什么?
3. 需要补充哪些步骤?

以 JSON 格式返回:
{
  "goal_achieved": true/false,
  "missing_parts": ["缺失部分1", "缺失部分2"],
  "suggested_steps": [
    {
      "description": "补充步骤描述",
      "expected_output": "期望输出"
    }
  ]
}
