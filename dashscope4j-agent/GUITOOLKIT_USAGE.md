# GuiToolkit 使用指南

## 概述

GuiToolkit 为 LLM Agent 提供了完整的桌面自动化能力，包括：
- 📸 屏幕截图（支持全屏和区域截图）
- 🖱️ 鼠标操作（移动、点击、拖拽、滚动）
- ⌨️ 键盘操作（按键、文本输入、组合键）
- 📋 剪贴板操作（读取和设置）

## 快速开始

### 1. 创建 GuiToolkit

```java
// 完整功能
var guiToolkit = GuiToolkit.newBuilder()
    .enableScreenshot(true)
    .enableMouse(true)
    .enableKeyboard(true)
    .enableClipboard(true)
    .build();

// 只读模式（仅截图）
var readOnlyKit = GuiToolkit.newBuilder()
    .enableScreenshot(true)
    .enableMouse(false)
    .enableKeyboard(false)
    .enableClipboard(false)
    .build();
```

### 2. 集成到 Agent

```java
var toolbox = HashMapToolbox.newBuilder()
    .indexer(HashMapToolIndexer.newBuilder()
        .client(client)
        .model(ChatModel.QWEN_FLASH)
        .build())
    .loaders(List.of(
        ToolkitLoader.of(guiToolkit),
        ToolkitLoader.of(SystemToolkit.create()),
        ToolkitLoader.of(FileOpsToolkit.newBuilder()
            .workspace(Path.of("./"))
            .build())
    ))
    .build();

var agent = ReActAgent.newBuilder()
    .client(client)
    .model(ChatModel.QWEN_PLUS)
    .toolbox(toolbox)
    .build();
```

## 工具详解

### 📸 屏幕截图 (gui$screenshot)

**截取全屏：**
```java
// LLM 会自动调用
agent.async("session-001", Message.user("帮我看看当前屏幕上显示什么"));
```

**截取指定区域：**
```java
// 参数：x, y, width, height
agent.async("session-001", Message.user("截取屏幕左上角 800x600 的区域"));
```

**返回结果：**
```json
{
  "image_base64": "iVBORw0KGgoAAAANSUhEUgAA...",
  "width": 1920,
  "height": 1080,
  "format": "png",
  "timestamp": 1713254400000,
  "region": {
    "x": 0,
    "y": 0,
    "width": 1920,
    "height": 1080
  }
}
```

### 🖱️ 鼠标操作

#### 移动鼠标 (gui$mouse$move)

```java
// 移动到坐标 (500, 300)
agent.async("session-001", Message.user("把鼠标移动到屏幕中间位置"));
```

**参数：**
- `x`: X 坐标（像素）
- `y`: Y 坐标（像素）

#### 鼠标点击 (gui$mouse$click)

```java
// 左键单击（默认）
agent.async("session-001", Message.user("点击当前位置"));

// 右键单击
agent.async("session-001", Message.user("右键点击打开菜单"));

// 双击
agent.async("session-001", Message.user("双击打开文件"));
```

**支持的按钮类型：**
- `left` - 左键单击（默认）
- `right` - 右键单击
- `middle` - 中键单击
- `double_left` - 左键双击

#### 鼠标拖拽 (gui$mouse$drag)

```java
// 从 (100, 100) 拖拽到 (500, 500)
agent.async("session-001", Message.user("把这个文件拖到文件夹里"));
```

**参数：**
- `from_x`, `from_y` - 起始位置
- `to_x`, `to_y` - 目标位置

#### 鼠标滚轮 (gui$mouse$scroll)

```java
// 向下滚动
agent.async("session-001", Message.user("向下滚动页面"));

// 向上滚动
agent.async("session-001", Message.user("向上滚动一点"));
```

**参数：**
- `amount` - 滚动量（正数向下，负数向上）

### ⌨️ 键盘操作

#### 按键 (gui$key$press)

```java
// 按下 Enter 键
agent.async("session-001", Message.user("按回车确认"));

// 按下 Esc 键
agent.async("session-001", Message.user("按 ESC 取消"));

// 按下 F5 刷新
agent.async("session-001", Message.user("按 F5 刷新页面"));
```

**支持的按键：**
- 字母：A-Z
- 数字：0-9
- 功能键：F1-F12
- 特殊键：ENTER, ESCAPE, TAB, SPACE, BACK_SPACE, DELETE
- 方向键：UP, DOWN, LEFT, RIGHT
- 修饰键：CONTROL, ALT, SHIFT, META
- 其他：HOME, END, PAGE_UP, PAGE_DOWN, INSERT, CAPS_LOCK

#### 文本输入 (gui$key$type)

```java
// 输入文本
agent.async("session-001", Message.user("在搜索框中输入 'Java 编程'"));
```

**注意事项：**
- 支持 ASCII 字符（英文、数字、常见符号）
- 不支持中文等非 ASCII 字符（需使用剪贴板方案）
- 支持转义字符：`\n` 表示换行

#### 组合键 (gui$key$combo)

```java
// Ctrl+C 复制
agent.async("session-001", Message.user("复制选中的内容"));

// Ctrl+V 粘贴
agent.async("session-001", Message.user("粘贴刚才复制的内容"));

// Alt+Tab 切换窗口
agent.async("session-001", Message.user("切换到上一个窗口"));

// Ctrl+S 保存
agent.async("session-001", Message.user("保存当前文件"));
```

**常用组合键示例：**
- Windows/Linux: `["CONTROL", "C"]`, `["CONTROL", "V"]`, `["CONTROL", "Z"]`
- macOS: `["META", "C"]`, `["META", "V"]`, `["META", "Z"]`
- 通用: `["ALT", "F4"]`, `["CONTROL", "S"]`, `["SHIFT", "TAB"]`

### 📋 剪贴板操作

#### 获取剪贴板 (gui$clipboard$get)

```java
// 读取剪贴板内容
agent.async("session-001", Message.user("我刚才复制了什么内容？"));
```

**返回结果：**
```json
{
  "content": "复制的文本内容",
  "length": 12,
  "has_content": true
}
```

#### 设置剪贴板 (gui$clipboard$set)

```java
// 设置剪贴板内容
agent.async("session-001", Message.user("把这段文字复制到剪贴板"));
```

**典型工作流程（输入中文）：**
```java
// 1. 设置剪贴板为中文
// 2. 聚焦输入框
// 3. 粘贴
agent.async("session-001", Message.user("""
    在文档中输入中文"你好世界"：
    1. 先设置剪贴板内容为"你好世界"
    2. 然后点击文档编辑区域
    3. 最后按 Ctrl+V 粘贴
"""));
```

## 实战场景

### 场景 1：自动化表单填写

```java
agent.async("form-session", Message.user("""
    帮我填写登录表单：
    1. 截图查看当前页面
    2. 找到用户名输入框并点击
    3. 输入用户名 "admin"
    4. 按 Tab 键切换到密码框
    5. 输入密码 "password123"
    6. 点击登录按钮
"""));
```

### 场景 2：文件管理

```java
agent.async("file-session", Message.user("""
    帮我把桌面上的 test.txt 文件移动到 Documents 文件夹：
    1. 截图查看桌面
    2. 找到 test.txt 文件
    3. 拖拽到 Documents 文件夹
"""));
```

### 场景 3：数据录入

```java
agent.async("data-entry", Message.user("""
    把这个 Excel 表格的数据录入到网页表单：
    1. 打开 Excel 文件
    2. 复制第一行数据
    3. 切换到浏览器
    4. 粘贴到第一个输入框
    5. 依次填写其他字段
    6. 提交表单
"""));
```

### 场景 4：UI 测试辅助

```java
agent.async("ui-test", Message.user("""
    测试这个应用的界面：
    1. 截图记录初始状态
    2. 点击各个按钮
    3. 每次点击后截图
    4. 验证界面响应是否正确
"""));
```

### 场景 5：远程协助

```java
agent.async("remote-help", Message.user("""
    帮我关闭所有打开的应用程序：
    1. 截图查看当前打开的窗口
    2. 逐个点击窗口的关闭按钮
    3. 或者使用 Alt+F4 快捷键
"""));
```

## 高级技巧

### 1. 结合图像识别

```java
// LLM 可以分析截图内容，然后决定下一步操作
agent.async("smart-session", Message.user("""
    查看当前屏幕，如果看到"错误提示"就点击"确定"按钮，
    否则继续执行后续操作
"""));
```

### 2. 精确控制

```java
// 通过多次小幅度操作实现精确控制
agent.async("precision", Message.user("""
    微调鼠标位置：
    1. 先移动到大致位置
    2. 截图确认
    3. 根据截图调整到精确位置
"""));
```

### 3. 批量操作

```java
// 循环执行相似操作
agent.async("batch", Message.user("""
    处理这 10 个文件：
    对每个文件执行：
    1. 双击打开
    2. 按 Ctrl+S 保存
    3. 按 Alt+F4 关闭
    4. 继续下一个
"""));
```

### 4. 错误恢复

```java
// LLM 会自动处理异常情况
agent.async("robust", Message.user("""
    尝试打开这个应用程序，如果打不开就尝试其他方法：
    1. 先尝试双击图标
    2. 如果没反应，尝试右键->打开
    3. 还是不行，尝试从开始菜单打开
"""));
```

## 注意事项

### ⚠️ 环境要求

1. **图形界面** - 必须在有 GUI 的环境中运行
   - ✅ 个人电脑（Windows/Mac/Linux 桌面版）
   - ❌ 纯服务器环境（无头模式）
   - ❌ Docker 容器（除非配置了 X11 转发）

2. **权限授权** - 需要用户授权
   - macOS: 系统偏好设置 → 安全性与隐私 → 隐私 → 辅助功能
   - Windows: 以管理员身份运行
   - Linux: 可能需要 xhost + 或 Wayland 配置

3. **安全软件** - 某些杀毒软件可能会阻止自动化操作
   - 将应用添加到白名单
   - 临时禁用实时保护

### 💡 最佳实践

1. **截图优化**
   ```java
   // ❌ 避免：截取整个屏幕（消耗大量 token）
   agent.async("session", Message.user("截取全屏"));
   
   // ✅ 推荐：只截取需要的区域
   agent.async("session", Message.user("截取左上角 800x600 区域"));
   ```

2. **输入中文**
   ```java
   // ❌ 不推荐：直接输入中文（不支持）
   agent.async("session", Message.user("输入 '你好世界'"));
   
   // ✅ 推荐：使用剪贴板
   agent.async("session", Message.user("""
       1. 设置剪贴板为"你好世界"
       2. 点击输入框
       3. 按 Ctrl+V 粘贴
   """));
   ```

3. **等待响应**
   ```java
   // LLM 会自动判断是否需要等待
   agent.async("session", Message.user("""
       点击"加载"按钮，等待页面加载完成后再继续
   """));
   ```

4. **错误处理**
   ```java
   // LLM 会尝试多种方法
   agent.async("session", Message.user("""
       尝试关闭窗口，如果失败就尝试其他方法
   """));
   ```

## 故障排查

### 问题 1：Robot 初始化失败

**错误信息：** `Failed to initialize AWT Robot`

**解决方案：**
- 确保在有图形界面的环境中运行
- 检查 DISPLAY 环境变量（Linux）
- 确认 Java 版本 >= 17

### 问题 2：截图全黑

**原因：** 权限不足或无头模式

**解决方案：**
- macOS: 授予屏幕录制权限
- Linux: 检查 X11 权限
- 确保不是 SSH 远程连接

### 问题 3：鼠标/键盘操作无效

**原因：** 焦点不在目标窗口

**解决方案：**
- 先点击目标窗口使其获得焦点
- 使用截图确认当前状态
- 检查是否有模态对话框阻挡

### 问题 4：中文输入乱码

**原因：** key$type 不支持非 ASCII 字符

**解决方案：**
- 使用剪贴板方案（clipboard$set + Ctrl+V）
- 或使用输入法 API（需要额外开发）

## 性能优化

### 1. 选择性启用功能

```java
// 如果只需要截图，禁用其他功能以减少资源占用
var screenshotOnly = GuiToolkit.newBuilder()
    .enableScreenshot(true)
    .enableMouse(false)
    .enableKeyboard(false)
    .enableClipboard(false)
    .build();
```

### 2. 限制截图尺寸

GuiToolkit 内置了最大截图尺寸限制（1920x1080），超出会自动报错。

### 3. 减少不必要的截图

```java
// ❌ 避免：频繁截图
agent.async("session", Message.user("截图...再截图...再截图..."));

// ✅ 推荐：只在关键节点截图
agent.async("session", Message.user("操作前截图一次，操作后截图一次"));
```

## 总结

GuiToolkit 为 LLM Agent 提供了强大的桌面自动化能力，可以实现：
- ✅ 视觉感知（截图）
- ✅ 鼠标控制（移动、点击、拖拽、滚动）
- ✅ 键盘输入（按键、文本、组合键）
- ✅ 剪贴板操作（读写）

通过合理组合这些工具，Agent 可以像人类一样操作计算机，实现各种复杂的自动化任务。

**记住：** 强大的能力意味着更大的责任，请谨慎使用自动化工具，确保操作安全可靠！
