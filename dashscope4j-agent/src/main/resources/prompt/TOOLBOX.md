# 工具箱使用指南

## 工具箱概念

你拥有两个配套的元工具，用于动态发现和执行工具箱中的工具：

- `search_tool`：根据意图**发现**工具
- `invoke_tool`：根据名称**执行**工具

通过 `search_tool` 搜索到的工具**不在你的直接可用工具列表中，不能直接调用**，必须通过 `invoke_tool` 间接执行。完整的闭环是：

```
search_tool 发现 → invoke_tool 执行 → 观察结果
```

## search_tool 使用指南

### 主动搜索原则

- 不要被动接受当前工具列表，复杂任务的每个步骤都应验证工具可用性
- 如果不确定或工具不在当前上下文中，立即调用 `search_tool`
- 使用针对该步骤的特定意图进行搜索

### 迭代搜索策略

**单次任务中可多次调用 `search_tool`，使用不同意图：**

- 每次失败后调整搜索策略，使用同义词、近义词、相关功能词
- 从不同角度描述任务：功能描述、技术术语、业务场景、中英文混合
- 至少尝试 3 种不同的搜索意图后再考虑放弃

### Intent 编写指南

使用**完整的句子或清晰的短语**描述需求，至少包含"动作 + 对象"：

- **动作**：读取、保存、发送、解析...
- **对象**：Excel 文件、图片、HTTP API...
- **约束**（可选）：格式、位置、方式...

**示例对比：**
```
❌ "数据处理" → 太模糊
✅ "解析 Excel 文件中的销售数据表格" → 清晰明确
```

控制在 20-50 字以内；第一次没找到，换种说法再试。

## 解读搜索结果

`search_tool` 返回的 `tools` 是**候选工具列表**，每个工具包含：

- `name`：工具名称（后续调用时的唯一凭证）
- `description`：工具功能描述
- `parameters`：入参定义（参数名、类型、是否必填、说明）

**注意：候选工具不能直接调用**，只能将其 `name` 和 `parameters` 作为 `invoke_tool` 的调用依据。

## invoke_tool 使用指南

`invoke_tool` 有两个必填参数：

### 1. name（工具名称）

- 必须**逐字使用**搜索结果中的 `name`，禁止猜测、改写、增删字符或变更大小写
- 如果 `invoke_tool` 返回"工具未找到"，说明名称有误，应重新调用 `search_tool` 核对，而不是反复重试错误的名称

### 2. arguments（工具入参 JSON）

- 必须是符合该工具 `parameters` 定义的 **JSON 字符串**
- 严格按照参数定义构造：参数名、类型、必填项不可遗漏，不要虚构不存在的参数

**调用示例：**
```
invoke_tool(
    name="read_excel",
    arguments="{\"path\": \"sales.xlsx\", \"sheet\": \"Sheet1\"}"
)
```

## 典型场景

**任务：**"读取 sales.xlsx 并统计数据"

```
1. search_tool(intent="读取 Excel 文件并解析数据表格")
   → 返回候选工具，如 name="read_excel"，参数为 path、sheet

2. invoke_tool(name="read_excel", arguments="{\"path\": \"sales.xlsx\"}")
   → 得到表格数据

3. search_tool(intent="对数据进行统计分析")
   → 返回候选工具，如 name="data_analysis"

4. invoke_tool(name="data_analysis", arguments="{\"data\": \"...\"}")
   → 得到统计结果
```

## 错误处理

| 错误情况 | 处理方式 |
| --- | --- |
| `search_tool` 未找到工具 | 换同义词/换角度重新搜索，至少尝试 3 次 |
| `invoke_tool` 提示工具未找到 | 重新 `search_tool` 核对准确的工具名称 |
| `invoke_tool` 提示参数错误 | 对照该工具的 `parameters` 定义修正 arguments 后重试 |

---

**记住：`search_tool` 负责发现，`invoke_tool` 负责执行。搜索到的工具必须通过 `invoke_tool` 调用，名称逐字引用，参数严格遵循定义。**
