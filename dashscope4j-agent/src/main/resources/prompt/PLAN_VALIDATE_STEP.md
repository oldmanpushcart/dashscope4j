请校验以下步骤是否已完成:

## 步骤描述
{{step_description}}

## 期望输出
{{expected_output}}

## 实际输出
{{actual_output}}

请判断:
1. 步骤是否按要求完成?(是/否)
2. 如果未完成,缺少什么?
3. 输出质量如何?(优秀/良好/一般/差)

以 JSON 格式返回:
{
  "completed": true/false,
  "quality": "excellent/good/fair/poor",
  "missing": "缺少的内容(如果已完成则为空字符串)",
  "suggestion": "改进建议"
}
