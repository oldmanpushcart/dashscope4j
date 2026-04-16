package io.github.oldmanpushcart.dashscope4j.agent.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * GUI 自动化工具包
 * <p>
 * 提供图形界面自动化能力给 LLM 使用：
 * - screenshot: 截取屏幕并返回 Base64 编码的图片
 * - mouse$move: 移动鼠标到指定坐标
 * - mouse$click: 执行鼠标点击（左键/右键/中键）
 * - mouse$drag: 拖拽鼠标从一个位置到另一个位置
 * - mouse$scroll: 滚动鼠标滚轮
 * - key$press: 按下指定按键
 * - key$type: 输入文本字符串
 * - key$combo: 执行组合键（如 Ctrl+C）
 * - clipboard$get: 获取剪贴板内容
 * - clipboard$set: 设置剪贴板内容
 * </p>
 * <p>
 * 注意：所有操作都需要 AWT 权限，在某些无头环境可能无法使用。
 * </p>
 */
public class GuiToolKit implements ToolKit {

    /**
     * 默认截图格式
     */
    private static final String DEFAULT_IMAGE_FORMAT = "png";

    /**
     * 最大截图尺寸（防止过大）
     */
    private static final int MAX_SCREENSHOT_SIZE = 1920 * 1080;

    /**
     * AWT Robot 实例
     */
    private final Robot robot;

    /**
     * 是否启用截图功能
     */
    private final boolean enableScreenshot;

    /**
     * 是否启用鼠标操作
     */
    private final boolean enableMouse;

    /**
     * 是否启用键盘操作
     */
    private final boolean enableKeyboard;

    /**
     * 是否启用剪贴板操作
     */
    private final boolean enableClipboard;

    private GuiToolKit(Builder builder) {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException("Failed to initialize AWT Robot", e);
        }
        this.enableScreenshot = builder.enableScreenshot;
        this.enableMouse = builder.enableMouse;
        this.enableKeyboard = builder.enableKeyboard;
        this.enableClipboard = builder.enableClipboard;
    }

    @Override
    public List<Tool> tools() {
        final var tools = new ArrayList<Tool>();

        if (enableScreenshot) {
            tools.add(screenshot());
        }

        if (enableMouse) {
            tools.add(mouseMove());
            tools.add(mouseClick());
            tools.add(mouseDrag());
            tools.add(mouseScroll());
        }

        if (enableKeyboard) {
            tools.add(keyPress());
            tools.add(keyType());
            tools.add(keyCombo());
        }

        if (enableClipboard) {
            tools.add(clipboardGet());
            tools.add(clipboardSet());
        }

        return Collections.unmodifiableList(tools);
    }

    // ==================== 屏幕截图工具 ====================

    /**
     * gui$screenshot 工具
     */
    private FunctionTool screenshot() {
        return FunctionTool.newBuilder()
                .name("gui$screenshot")
                .description("""
                        截取当前屏幕或指定区域的截图，保存为临时文件并返回文件 URI。
                        
                        【使用场景】
                        - 查看当前屏幕显示内容
                        - 识别界面上的元素、文字、图标
                        - 分析应用界面布局
                        - 调试 UI 问题
                        
                        【返回结果】
                        - file_uri: 临时图片文件的 URI（file:// 协议）
                        - file_path: 临时图片文件的绝对路径
                        - width: 图片宽度（像素）
                        - height: 图片高度（像素）
                        - format: 图片格式（png）
                        - timestamp: 截图时间戳
                        
                        【注意事项】
                        - 不指定区域时截取整个屏幕
                        - 可以指定矩形区域（x, y, width, height）
                        - 返回的文件 URI 可以直接用于图像识别模型
                        - 临时文件需要手动清理或在不再需要时删除
                        - 大尺寸截图会消耗较多 token，建议只截取需要的区域
                        """)
                .parameterType(ScreenshotSpec.class)
                .<ScreenshotSpec>function(new BiFunction<>() {

                    @Override
                    public Object apply(Tool.Caller caller, ScreenshotSpec spec) {
                        try {

                            // 截取区域
                            final var captureRect = getCaptureRect(spec);

                            // 检查截图尺寸限制
                            final long pixelCount = (long) captureRect.width * captureRect.height;
                            if (pixelCount > MAX_SCREENSHOT_SIZE) {
                                return Result.error("SCREENSHOT_TOO_LARGE",
                                        "截图尺寸过大：%dx%d，最大支持 %d 像素".formatted(
                                                captureRect.width,
                                                captureRect.height,
                                                MAX_SCREENSHOT_SIZE
                                        ));
                            }

                            // 执行截图
                            final var screenCapture = robot.createScreenCapture(captureRect);

                            // 创建临时文件
                            final var tempFile = Files.createTempFile("screenshot-", ".png").toFile();
                            tempFile.deleteOnExit(); // JVM 退出时自动删除

                            // 保存图片到临时文件
                            ImageIO.write(screenCapture, DEFAULT_IMAGE_FORMAT, tempFile);

                            // 生成文件 URI
                            final var fileUri = tempFile.toURI();

                            // 返回结果
                            final var result = Map.of(
                                    "file_uri", fileUri.toString(),
                                    "file_path", tempFile.getAbsolutePath(),
                                    "width", screenCapture.getWidth(),
                                    "height", screenCapture.getHeight(),
                                    "format", DEFAULT_IMAGE_FORMAT,
                                    "timestamp", System.currentTimeMillis(),
                                    "region", Map.of(
                                            "x", captureRect.x,
                                            "y", captureRect.y,
                                            "width", captureRect.width,
                                            "height", captureRect.height
                                    )
                            );

                            return Result.success(result);

                        } catch (Exception ex) {
                            return Result.error("SCREENSHOT_FAILED", "截图失败：" + ex.getMessage());
                        }
                    }

                    /**
                     * 获取截图区域
                     *
                     * @param spec 截屏请求
                     * @return 截图区域
                     */
                    private static Rectangle getCaptureRect(ScreenshotSpec spec) {

                        final Rectangle captureRect;

                        // 截取指定区域
                        if (spec.x() != null && spec.y() != null && spec.width() != null && spec.height() != null) {
                            captureRect = new Rectangle(spec.x(), spec.y(), spec.width(), spec.height());
                        }

                        // 截取整个屏幕
                        else {
                            final var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                            final var gd = ge.getDefaultScreenDevice();
                            captureRect = gd.getDefaultConfiguration().getBounds();
                        }
                        return captureRect;
                    }

                })
                .build();
    }

    // ==================== 鼠标操作工具 ====================

    /**
     * gui$mouse$move 工具
     */
    private FunctionTool mouseMove() {
        return FunctionTool.newBuilder()
                .name("gui$mouse$move")
                .description("""
                        移动鼠标指针到指定的屏幕坐标位置。
                        
                        【使用场景】
                        - 将鼠标移动到按钮、链接或其他 UI 元素上
                        - 准备进行点击或拖拽操作
                        - 悬停触发菜单或提示
                        
                        【坐标系说明】
                        - 原点 (0, 0) 在屏幕左上角
                        - X 轴向右递增，Y 轴向下递增
                        - 坐标单位为像素
                        
                        【返回结果】
                        - success: 是否成功
                        - current_position: 当前鼠标位置 {x, y}
                        
                        【注意事项】
                        - 坐标必须在屏幕范围内
                        - 多显示器环境下，坐标可能是负数（副屏在主屏左侧）
                        - 移动后立即生效，无需延迟
                        """)
                .parameterType(MouseMoveSpec.class)
                .<MouseMoveSpec>function((caller, spec) -> {
                    try {
                        robot.mouseMove(spec.x(), spec.y());

                        final var location = MouseInfo.getPointerInfo().getLocation();
                        final var result = Map.of(
                                "success", true,
                                "current_position", Map.of(
                                        "x", location.x,
                                        "y", location.y
                                )
                        );

                        return Result.success(result);

                    } catch (Exception ex) {
                        return Result.error("MOUSE_MOVE_FAILED", "鼠标移动失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    /**
     * gui$mouse$click 工具
     */
    private FunctionTool mouseClick() {
        return FunctionTool.newBuilder()
                .name("gui$mouse$click")
                .description("""
                        在当前位置执行鼠标点击操作。
                        
                        【使用场景】
                        - 点击按钮、链接、菜单项
                        - 选择文件、文件夹
                        - 激活窗口或控件
                        
                        【支持的按键】
                        - left: 左键单击（默认）
                        - right: 右键单击（打开上下文菜单）
                        - middle: 中键单击（滚轮按下）
                        - double_left: 左键双击
                        
                        【返回结果】
                        - success: 是否成功
                        - button: 实际点击的按键
                        - click_count: 点击次数
                        
                        【注意事项】
                        - 点击前确保鼠标已在目标位置
                        - 某些操作可能需要等待响应
                        - 右键点击会触发上下文菜单
                        """)
                .parameterType(MouseClickSpec.class)
                .<MouseClickSpec>function((caller, spec) -> {
                    try {
                        final String button = spec.button() != null ? spec.button().toLowerCase() : "left";
                        int clickCount = 1;

                        switch (button) {
                            case "left":
                                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                                robot.delay(50);
                                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                                break;
                            case "right":
                                robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                                robot.delay(50);
                                robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                                break;
                            case "middle":
                                robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
                                robot.delay(50);
                                robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
                                break;
                            case "double_left":
                                clickCount = 2;
                                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                                robot.delay(50);
                                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                                robot.delay(100);
                                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                                robot.delay(50);
                                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                                break;
                            default:
                                return Result.error("INVALID_BUTTON",
                                        "无效的按键类型：" + button + "，支持：left, right, middle, double_left");
                        }

                        final var result = Map.of(
                                "success", true,
                                "button", button,
                                "click_count", clickCount
                        );

                        return Result.success(result);

                    } catch (Exception ex) {
                        return Result.error("MOUSE_CLICK_FAILED", "鼠标点击失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    /**
     * gui$mouse$drag 工具
     */
    private FunctionTool mouseDrag() {
        return FunctionTool.newBuilder()
                .name("gui$mouse$drag")
                .description("""
                        执行鼠标拖拽操作，从起始位置拖拽到目标位置。
                        
                        【使用场景】
                        - 拖拽文件到文件夹
                        - 调整窗口大小
                        - 在画布上绘制
                        - 滑动滑块
                        - 选择文本区域
                        
                        【返回结果】
                        - success: 是否成功
                        - from: 起始位置 {x, y}
                        - to: 目标位置 {x, y}
                        
                        【注意事项】
                        - 会自动按下左键并拖拽
                        - 拖拽过程中保持左键按下状态
                        - 到达目标位置后释放左键
                        """)
                .parameterType(MouseDragSpec.class)
                .<MouseDragSpec>function((caller, spec) -> {
                    try {
                        // 移动到起始位置
                        robot.mouseMove(spec.fromX(), spec.fromY());
                        robot.delay(50);

                        // 按下左键
                        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                        robot.delay(50);

                        // 移动到目标位置
                        robot.mouseMove(spec.toX(), spec.toY());
                        robot.delay(50);

                        // 释放左键
                        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

                        final var result = Map.of(
                                "success", true,
                                "from", Map.of(
                                        "x", spec.fromX(),
                                        "y", spec.fromY()
                                ),
                                "to", Map.of(
                                        "x", spec.toX(),
                                        "y", spec.toY()
                                )
                        );

                        return Result.success(result);

                    } catch (Exception ex) {
                        return Result.error("MOUSE_DRAG_FAILED", "鼠标拖拽失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    /**
     * gui$mouse$scroll 工具
     */
    private FunctionTool mouseScroll() {
        return FunctionTool.newBuilder()
                .name("gui$mouse$scroll")
                .description("""
                        滚动鼠标滚轮。
                        
                        【使用场景】
                        - 上下滚动网页或文档
                        - 在列表中浏览项目
                        - 缩放视图（配合 Ctrl 键）
                        
                        【参数说明】
                        - amount: 滚动量
                          * 正数：向下滚动
                          * 负数：向上滚动
                          * 典型值：1-10（小幅度），10-50（大幅度）
                        
                        【返回结果】
                        - success: 是否成功
                        - amount: 实际滚动量
                        
                        【注意事项】
                        - 不同系统/应用的滚动速度可能不同
                        - 如果需要精确控制，可以多次调用小幅度滚动
                        """)
                .parameterType(MouseScrollSpec.class)
                .<MouseScrollSpec>function((caller, spec) -> {
                    try {
                        robot.mouseWheel(spec.amount());

                        final var result = Map.of(
                                "success", true,
                                "amount", spec.amount()
                        );

                        return Result.success(result);

                    } catch (Exception ex) {
                        return Result.error("MOUSE_SCROLL_FAILED", "鼠标滚动失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    // ==================== 键盘操作工具 ====================

    /**
     * gui$key$press 工具
     */
    private FunctionTool keyPress() {
        return FunctionTool.newBuilder()
                .name("gui$key$press")
                .description("""
                        按下并释放指定的键盘按键。
                        
                        【使用场景】
                        - 按下功能键（F1-F12）
                        - 按下方向键、Enter、Esc 等
                        - 触发快捷键的一部分
                        
                        【支持的按键名称】
                        - 字母：A-Z（大写）
                        - 数字：0-9
                        - 功能键：F1-F12
                        - 特殊键：ENTER, ESCAPE, TAB, SPACE, BACK_SPACE, DELETE
                        - 方向键：UP, DOWN, LEFT, RIGHT
                        - 修饰键：CONTROL, ALT, SHIFT, META（Windows/Command）
                        - 其他：HOME, END, PAGE_UP, PAGE_DOWN, INSERT, CAPS_LOCK
                        
                        【返回结果】
                        - success: 是否成功
                        - key: 按下的按键名称
                        
                        【注意事项】
                        - 只执行单次按键（按下+释放）
                        - 如需组合键，请使用 key$combo
                        - 如需输入文本，请使用 key$type
                        """)
                .parameterType(KeyPressSpec.class)
                .<KeyPressSpec>function((caller, spec) -> {
                    try {
                        int keyCode = parseKeyCode(spec.key());

                        robot.keyPress(keyCode);
                        robot.delay(50);
                        robot.keyRelease(keyCode);

                        final var result = Map.of(
                                "success", true,
                                "key", spec.key()
                        );

                        return Result.success(result);

                    } catch (IllegalArgumentException ex) {
                        return Result.error("INVALID_KEY", ex.getMessage());
                    } catch (Exception ex) {
                        return Result.error("KEY_PRESS_FAILED", "按键失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    /**
     * gui$key$type 工具
     */
    private FunctionTool keyType() {
        return FunctionTool.newBuilder()
                .name("gui$key$type")
                .description("""
                        模拟键盘输入文本字符串。
                        
                        【使用场景】
                        - 在表单中输入文本
                        - 搜索框输入关键词
                        - 编辑器中输入代码或文档
                        - 命令行输入命令
                        
                        【支持字符】
                        - 英文字母：a-z, A-Z
                        - 数字：0-9
                        - 常见标点：.,;:'"!?@#$%^&*()_+-=[]{}|\\<>,./?
                        - 空格
                        - 回车换行：\\n
                        
                        【返回结果】
                        - success: 是否成功
                        - text_length: 输入的字符数
                        
                        【注意事项】
                        - 需要确保焦点在正确的输入框
                        - 不支持中文等非 ASCII 字符（使用剪贴板方案）
                        - 特殊字符可能需要 Shift 键，已自动处理
                        - 输入速度较快，某些应用可能需要延迟
                        """)
                .parameterType(KeyTypeSpec.class)
                .<KeyTypeSpec>function((caller, spec) -> {
                    try {
                        final String text = spec.text();
                        if (text == null || text.isEmpty()) {
                            return Result.error("EMPTY_TEXT", "输入文本不能为空");
                        }

                        typeString(text);

                        final var result = Map.of(
                                "success", true,
                                "text_length", text.length()
                        );

                        return Result.success(result);

                    } catch (Exception ex) {
                        return Result.error("KEY_TYPE_FAILED", "文本输入失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    /**
     * gui$key$combo 工具
     */
    private FunctionTool keyCombo() {
        return FunctionTool.newBuilder()
                .name("gui$key$combo")
                .description("""
                        执行组合键操作（同时按下多个键）。
                        
                        【使用场景】
                        - 复制粘贴：Ctrl+C, Ctrl+V
                        - 保存文件：Ctrl+S
                        - 撤销重做：Ctrl+Z, Ctrl+Y
                        - 切换标签：Ctrl+Tab
                        - 全选：Ctrl+A
                        - Mac 系统：Command+C, Command+V 等
                        
                        【参数格式】
                        - keys: 按键名称列表，按顺序按下
                        - 示例：["CONTROL", "C"] 表示 Ctrl+C
                        - 示例：["ALT", "F4"] 表示 Alt+F4
                        
                        【常用组合键】
                        - Windows/Linux: CONTROL, ALT, SHIFT
                        - macOS: META（Command 键）, ALT（Option）, SHIFT
                        
                        【返回结果】
                        - success: 是否成功
                        - keys: 执行的按键组合
                        
                        【注意事项】
                        - 按键按顺序按下，然后逆序释放
                        - 最多支持 5 个按键组合
                        - 确保使用正确的修饰键名称
                        """)
                .parameterType(KeyComboSpec.class)
                .<KeyComboSpec>function((caller, spec) -> {
                    try {
                        final List<String> keys = spec.keys();
                        if (keys == null || keys.isEmpty()) {
                            return Result.error("EMPTY_KEYS", "按键列表不能为空");
                        }

                        if (keys.size() > 5) {
                            return Result.error("TOO_MANY_KEYS",
                                    "组合键最多支持 5 个按键，当前：" + keys.size());
                        }

                        // 解析所有按键
                        final int[] keyCodes = new int[keys.size()];
                        for (int i = 0; i < keys.size(); i++) {
                            keyCodes[i] = parseKeyCode(keys.get(i));
                        }

                        // 按下所有按键
                        for (int keyCode : keyCodes) {
                            robot.keyPress(keyCode);
                            robot.delay(30);
                        }

                        // 逆序释放按键
                        for (int i = keyCodes.length - 1; i >= 0; i--) {
                            robot.keyRelease(keyCodes[i]);
                            robot.delay(30);
                        }

                        final var result = Map.of(
                                "success", true,
                                "keys", keys
                        );

                        return Result.success(result);

                    } catch (IllegalArgumentException ex) {
                        return Result.error("INVALID_KEY", ex.getMessage());
                    } catch (Exception ex) {
                        return Result.error("KEY_COMBO_FAILED", "组合键失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    // ==================== 剪贴板工具 ====================

    /**
     * gui$clipboard$get 工具
     */
    private FunctionTool clipboardGet() {
        return FunctionTool.newBuilder()
                .name("gui$clipboard$get")
                .description("""
                        获取系统剪贴板的文本内容。
                        
                        【使用场景】
                        - 读取用户复制的文本
                        - 获取之前通过 key$combo(Ctrl+C) 复制的内容
                        - 在不同应用间传递文本
                        
                        【返回结果】
                        - content: 剪贴板中的文本内容
                        - length: 文本长度
                        - has_content: 是否有内容
                        
                        【注意事项】
                        - 只能获取文本内容
                        - 如果剪贴板为空或包含非文本内容，返回空字符串
                        - 某些安全设置可能阻止访问剪贴板
                        """)
                .supplier(() -> {
                    try {
                        final var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        String content = "";

                        if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                            content = (String) clipboard.getData(DataFlavor.stringFlavor);
                        }

                        return Map.of(
                                "content", content,
                                "length", content.length(),
                                "has_content", !content.isEmpty()
                        );

                    } catch (Exception ex) {
                        return Map.of(
                                "content", "",
                                "length", 0,
                                "has_content", false,
                                "error", "获取剪贴板失败：" + ex.getMessage()
                        );
                    }
                })
                .build();
    }

    /**
     * gui$clipboard$set 工具
     */
    private FunctionTool clipboardSet() {
        return FunctionTool.newBuilder()
                .name("gui$clipboard$set")
                .description("""
                        设置系统剪贴板的文本内容。
                        
                        【使用场景】
                        - 准备要粘贴的文本
                        - 跨应用传递大量文本（比 key$type 更可靠）
                        - 复制生成的内容供用户粘贴到其他应用
                        - 输入中文等非 ASCII 字符（先设置剪贴板，再 Ctrl+V）
                        
                        【返回结果】
                        - success: 是否成功
                        - length: 设置的文本长度
                        
                        【典型工作流程】
                        1. 设置剪贴板：gui$clipboard$set(text="要复制的文本")
                        2. 聚焦目标输入框
                        3. 粘贴：gui$key$combo(keys=["CONTROL", "V"])
                        
                        【注意事项】
                        - 会覆盖剪贴板原有内容
                        - 支持任意 Unicode 字符（包括中文）
                        - 大文本可能会占用较多内存
                        """)
                .parameterType(ClipboardSetSpec.class)
                .<ClipboardSetSpec>function((caller, spec) -> {
                    try {
                        final var text = spec.text() != null ? spec.text() : "";

                        final var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        final var selection = new StringSelection(text);
                        clipboard.setContents(selection, selection);

                        final var result = Map.of(
                                "success", true,
                                "length", text.length()
                        );

                        return Result.success(result);

                    } catch (Exception ex) {
                        return Result.error("CLIPBOARD_SET_FAILED", "设置剪贴板失败：" + ex.getMessage());
                    }
                })
                .build();
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析按键名称为 KeyEvent 常量
     */
    private int parseKeyCode(String keyName) {
        if (keyName == null || keyName.isEmpty()) {
            throw new IllegalArgumentException("按键名称不能为空");
        }

        final String upperKey = keyName.toUpperCase().trim();

        // 单字符按键
        if (upperKey.length() == 1) {
            final char c = upperKey.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return KeyEvent.VK_A + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return KeyEvent.VK_0 + (c - '0');
            }
        }

        // 特殊按键映射
        return switch (upperKey) {
            case "ENTER", "RETURN" -> KeyEvent.VK_ENTER;
            case "ESCAPE", "ESC" -> KeyEvent.VK_ESCAPE;
            case "TAB" -> KeyEvent.VK_TAB;
            case "SPACE" -> KeyEvent.VK_SPACE;
            case "BACK_SPACE", "BACKSPACE" -> KeyEvent.VK_BACK_SPACE;
            case "DELETE", "DEL" -> KeyEvent.VK_DELETE;
            case "UP" -> KeyEvent.VK_UP;
            case "DOWN" -> KeyEvent.VK_DOWN;
            case "LEFT" -> KeyEvent.VK_LEFT;
            case "RIGHT" -> KeyEvent.VK_RIGHT;
            case "HOME" -> KeyEvent.VK_HOME;
            case "END" -> KeyEvent.VK_END;
            case "PAGE_UP", "PAGEUP" -> KeyEvent.VK_PAGE_UP;
            case "PAGE_DOWN", "PAGEDOWN" -> KeyEvent.VK_PAGE_DOWN;
            case "INSERT", "INS" -> KeyEvent.VK_INSERT;
            case "CAPS_LOCK", "CAPSLOCK" -> KeyEvent.VK_CAPS_LOCK;
            case "CONTROL", "CTRL" -> KeyEvent.VK_CONTROL;
            case "ALT" -> KeyEvent.VK_ALT;
            case "SHIFT" -> KeyEvent.VK_SHIFT;
            case "META", "COMMAND", "WIN", "WINDOWS" -> KeyEvent.VK_META;
            case "F1" -> KeyEvent.VK_F1;
            case "F2" -> KeyEvent.VK_F2;
            case "F3" -> KeyEvent.VK_F3;
            case "F4" -> KeyEvent.VK_F4;
            case "F5" -> KeyEvent.VK_F5;
            case "F6" -> KeyEvent.VK_F6;
            case "F7" -> KeyEvent.VK_F7;
            case "F8" -> KeyEvent.VK_F8;
            case "F9" -> KeyEvent.VK_F9;
            case "F10" -> KeyEvent.VK_F10;
            case "F11" -> KeyEvent.VK_F11;
            case "F12" -> KeyEvent.VK_F12;
            default -> throw new IllegalArgumentException("不支持的按键：" + keyName);
        };
    }

    /**
     * 输入字符串（支持大小写和特殊字符）
     */
    private void typeString(String text) {
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                // 换行符
                robot.keyPress(KeyEvent.VK_ENTER);
                robot.delay(50);
                robot.keyRelease(KeyEvent.VK_ENTER);
            } else if (Character.isUpperCase(c) || isShiftRequired(c)) {
                // 需要 Shift 的字符
                robot.keyPress(KeyEvent.VK_SHIFT);
                robot.delay(10);

                final int keyCode = getUpperCaseKeyCode(c);
                robot.keyPress(keyCode);
                robot.delay(50);
                robot.keyRelease(keyCode);

                robot.delay(10);
                robot.keyRelease(KeyEvent.VK_SHIFT);
            } else {
                // 普通字符
                final int keyCode = getLowerCaseKeyCode(c);
                if (keyCode != -1) {
                    robot.keyPress(keyCode);
                    robot.delay(50);
                    robot.keyRelease(keyCode);
                }
            }
            robot.delay(20); // 字符间延迟
        }
    }

    /**
     * 判断字符是否需要 Shift 键
     */
    private boolean isShiftRequired(char c) {
        return "!@#$%^&*()_+{}|:\"<>?~".indexOf(c) != -1;
    }

    /**
     * 获取大写字母或符号的按键码
     */
    private int getUpperCaseKeyCode(char c) {
        if (c >= 'A' && c <= 'Z') {
            return KeyEvent.VK_A + (c - 'A');
        }

        return switch (c) {
            case '!' -> KeyEvent.VK_1;
            case '@' -> KeyEvent.VK_2;
            case '#' -> KeyEvent.VK_3;
            case '$' -> KeyEvent.VK_4;
            case '%' -> KeyEvent.VK_5;
            case '^' -> KeyEvent.VK_6;
            case '&' -> KeyEvent.VK_7;
            case '*' -> KeyEvent.VK_8;
            case '(' -> KeyEvent.VK_9;
            case ')' -> KeyEvent.VK_0;
            case '_' -> KeyEvent.VK_MINUS;
            case '+' -> KeyEvent.VK_EQUALS;
            case '{' -> KeyEvent.VK_OPEN_BRACKET;
            case '}' -> KeyEvent.VK_CLOSE_BRACKET;
            case '|' -> KeyEvent.VK_BACK_SLASH;
            case ':' -> KeyEvent.VK_SEMICOLON;
            case '"' -> KeyEvent.VK_QUOTE;
            case '<' -> KeyEvent.VK_COMMA;
            case '>' -> KeyEvent.VK_PERIOD;
            case '?' -> KeyEvent.VK_SLASH;
            case '~' -> KeyEvent.VK_BACK_QUOTE;
            default -> -1;
        };
    }

    /**
     * 获取小写字母的按键码
     */
    private int getLowerCaseKeyCode(char c) {
        if (c >= 'a' && c <= 'z') {
            return KeyEvent.VK_A + (c - 'a');
        }
        if (c >= '0' && c <= '9') {
            return KeyEvent.VK_0 + (c - '0');
        }

        return switch (c) {
            case '-' -> KeyEvent.VK_MINUS;
            case '=' -> KeyEvent.VK_EQUALS;
            case '[' -> KeyEvent.VK_OPEN_BRACKET;
            case ']' -> KeyEvent.VK_CLOSE_BRACKET;
            case '\\' -> KeyEvent.VK_BACK_SLASH;
            case ';' -> KeyEvent.VK_SEMICOLON;
            case '\'' -> KeyEvent.VK_QUOTE;
            case ',' -> KeyEvent.VK_COMMA;
            case '.' -> KeyEvent.VK_PERIOD;
            case '/' -> KeyEvent.VK_SLASH;
            case '`' -> KeyEvent.VK_BACK_QUOTE;
            case ' ' -> KeyEvent.VK_SPACE;
            default -> -1;
        };
    }

    // ==================== Spec 数据结构 ====================

    /**
     * screenshot 参数
     */
    record ScreenshotSpec(
            @JsonPropertyDescription("截图区域 X 坐标（可选，不指定则截取全屏）")
            @JsonProperty("x")
            Integer x,

            @JsonPropertyDescription("截图区域 Y 坐标（可选，不指定则截取全屏）")
            @JsonProperty("y")
            Integer y,

            @JsonPropertyDescription("截图区域宽度（可选，不指定则截取全屏）")
            @JsonProperty("width")
            Integer width,

            @JsonPropertyDescription("截图区域高度（可选，不指定则截取全屏）")
            @JsonProperty("height")
            Integer height
    ) {
    }

    /**
     * mouse$move 参数
     */
    record MouseMoveSpec(
            @JsonPropertyDescription("目标 X 坐标（像素）")
            @JsonProperty(value = "x", required = true)
            int x,

            @JsonPropertyDescription("目标 Y 坐标（像素）")
            @JsonProperty(value = "y", required = true)
            int y
    ) {
    }

    /**
     * mouse$click 参数
     */
    record MouseClickSpec(
            @JsonPropertyDescription("点击的按键：left, right, middle, double_left（默认 left）")
            @JsonProperty("button")
            String button
    ) {
    }

    /**
     * mouse$drag 参数
     */
    record MouseDragSpec(
            @JsonPropertyDescription("起始 X 坐标")
            @JsonProperty(value = "from_x", required = true)
            int fromX,

            @JsonPropertyDescription("起始 Y 坐标")
            @JsonProperty(value = "from_y", required = true)
            int fromY,

            @JsonPropertyDescription("目标 X 坐标")
            @JsonProperty(value = "to_x", required = true)
            int toX,

            @JsonPropertyDescription("目标 Y 坐标")
            @JsonProperty(value = "to_y", required = true)
            int toY
    ) {
    }

    /**
     * mouse$scroll 参数
     */
    record MouseScrollSpec(
            @JsonPropertyDescription("滚动量（正数向下，负数向上）")
            @JsonProperty(value = "amount", required = true)
            int amount
    ) {
    }

    /**
     * key$press 参数
     */
    record KeyPressSpec(
            @JsonPropertyDescription("按键名称（如 ENTER, ESCAPE, A, F1 等）")
            @JsonProperty(value = "key", required = true)
            String key
    ) {
    }

    /**
     * key$type 参数
     */
    record KeyTypeSpec(
            @JsonPropertyDescription("要输入的文本字符串")
            @JsonProperty(value = "text", required = true)
            String text
    ) {
    }

    /**
     * key$combo 参数
     */
    record KeyComboSpec(
            @JsonPropertyDescription("组合键的按键名称列表（如 [\"CONTROL\", \"C\"]）")
            @JsonProperty(value = "keys", required = true)
            List<String> keys
    ) {
    }

    /**
     * clipboard$set 参数
     */
    record ClipboardSetSpec(
            @JsonPropertyDescription("要设置到剪贴板的文本内容")
            @JsonProperty(value = "text", required = true)
            String text
    ) {
    }

    // ==================== 结果数据结构 ====================

    /**
     * 统一的结果封装
     */
    record Result(
            @JsonProperty("error")
            String error,

            @JsonProperty("message")
            String message,

            @JsonProperty("data")
            Object data
    ) {
        static Result success(Object data) {
            return new Result(null, null, data);
        }

        static Result error(String error, String message) {
            return new Result(error, message, null);
        }
    }

    // ==================== Builder ====================

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<GuiToolKit, Builder> {
        private boolean enableScreenshot = true;
        private boolean enableMouse = true;
        private boolean enableKeyboard = true;
        private boolean enableClipboard = true;

        /**
         * 启用/禁用截图功能
         */
        public Builder enableScreenshot(boolean enable) {
            this.enableScreenshot = enable;
            return this;
        }

        /**
         * 启用/禁用鼠标操作
         */
        public Builder enableMouse(boolean enable) {
            this.enableMouse = enable;
            return this;
        }

        /**
         * 启用/禁用键盘操作
         */
        public Builder enableKeyboard(boolean enable) {
            this.enableKeyboard = enable;
            return this;
        }

        /**
         * 启用/禁用剪贴板操作
         */
        public Builder enableClipboard(boolean enable) {
            this.enableClipboard = enable;
            return this;
        }

        @Override
        public GuiToolKit build() {
            return new GuiToolKit(this);
        }
    }

}
