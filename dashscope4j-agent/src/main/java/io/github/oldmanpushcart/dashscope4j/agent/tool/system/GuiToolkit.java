package io.github.oldmanpushcart.dashscope4j.agent.tool.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.tool.Toolkit;
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
 * 提供图形界面自动化能力给 LLM 使用，包括：
 * </p>
 * <ul>
 *   <li><b>screenshot</b>: 截取屏幕并保存为临时文件，返回文件 URI</li>
 *   <li><b>mouse$move</b>: 移动鼠标到指定坐标</li>
 *   <li><b>mouse$click</b>: 执行鼠标点击（左键/右键/中键）</li>
 *   <li><b>mouse$drag</b>: 拖拽鼠标从一个位置到另一个位置</li>
 *   <li><b>mouse$scroll</b>: 滚动鼠标滚轮</li>
 *   <li><b>key$press</b>: 按下指定按键</li>
 *   <li><b>key$type</b>: 输入文本字符串</li>
 *   <li><b>key$combo</b>: 执行组合键（如 Ctrl+C）</li>
 *   <li><b>clipboard$get</b>: 获取剪贴板内容</li>
 *   <li><b>clipboard$set</b>: 设置剪贴板内容</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * </p>
 * <ul>
 *   <li>所有操作都需要 AWT 权限，在某些无头环境（headless）可能无法使用</li>
 *   <li>截图功能会将图片保存为临时文件，JVM 退出时自动清理</li>
 *   <li>键盘输入不支持中文等非 ASCII 字符，建议使用剪贴板方案</li>
 *   <li>可通过 Builder 选择性启用/禁用各项功能</li>
 * </ul>
 *
 * @see #newBuilder() 创建构建器实例
 * @see Toolkit 工具包接口
 */
public class GuiToolkit implements io.github.oldmanpushcart.dashscope4j.agent.tool.Toolkit {

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

    /**
     * 构造函数
     * <p>
     * 初始化 AWT Robot 实例并根据配置启用相应的工具功能。
     * </p>
     *
     * @param builder 构建器，包含各项功能的启用配置
     * @throws RuntimeException 当 AWT Robot 初始化失败时抛出（通常在无头环境下）
     */
    private GuiToolkit(Builder builder) {
        try {
            // 创建 AWT Robot 实例，用于模拟鼠标和键盘操作
            this.robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException("Failed to initialize AWT Robot", e);
        }
        // 从构建器中读取各项功能的启用状态
        this.enableScreenshot = builder.enableScreenshot;
        this.enableMouse = builder.enableMouse;
        this.enableKeyboard = builder.enableKeyboard;
        this.enableClipboard = builder.enableClipboard;
    }

    /**
     * 获取此工具包提供的所有工具列表
     * <p>
     * 根据构建时配置的启用标志，动态组装可用的工具集合。
     * 返回的列表是不可变的，防止外部修改。
     * </p>
     *
     * @return 不可变的工具列表，包含所有启用的 GUI 自动化工具
     */
    @Override
    public List<Tool> tools() {
        final var tools = new ArrayList<Tool>();

        // 根据配置添加截图工具
        if (enableScreenshot) {
            tools.add(screenshot());
        }

        // 根据配置添加鼠标操作工具
        if (enableMouse) {
            tools.add(mouseMove());
            tools.add(mouseClick());
            tools.add(mouseDrag());
            tools.add(mouseScroll());
        }

        // 根据配置添加键盘操作工具
        if (enableKeyboard) {
            tools.add(keyPress());
            tools.add(keyType());
            tools.add(keyCombo());
        }

        // 根据配置添加剪贴板操作工具
        if (enableClipboard) {
            tools.add(clipboardGet());
            tools.add(clipboardSet());
        }

        // 返回不可变列表，确保工具集合的安全性
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
                     * 计算并返回截图区域
                     * <p>
                     * 如果提供了完整的区域参数（x, y, width, height），则使用指定区域；
                     * 否则截取整个屏幕。
                     * </p>
                     *
                     * @param spec 截图请求参数，包含可选的区域坐标和尺寸
                     * @return 截图区域的矩形对象，单位为像素
                     */
                    private static Rectangle getCaptureRect(ScreenshotSpec spec) {

                        final Rectangle captureRect;

                        // 情况1：用户提供完整的区域参数，截取指定区域
                        if (spec.x() != null && spec.y() != null && spec.width() != null && spec.height() != null) {
                            captureRect = new Rectangle(spec.x(), spec.y(), spec.width(), spec.height());
                        }
                        // 情况2：未提供区域参数，截取整个屏幕
                        else {
                            // 获取默认屏幕设备的边界
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
     * 创建鼠标移动工具
     * <p>
     * 将鼠标指针移动到指定的屏幕坐标位置。
     * </p>
     *
     * @return 鼠标移动功能工具对象
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
     * 创建鼠标点击工具
     * <p>
     * 在当前位置执行鼠标点击操作，支持左键、右键、中键和双击。
     * </p>
     *
     * @return 鼠标点击功能工具对象
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
     * 创建鼠标拖拽工具
     * <p>
     * 执行鼠标拖拽操作，从起始位置按住左键拖拽到目标位置后释放。
     * </p>
     *
     * @return 鼠标拖拽功能工具对象
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
     * 创建鼠标滚动工具
     * <p>
     * 模拟鼠标滚轮滚动，支持向上和向下滚动。
     * </p>
     *
     * @return 鼠标滚动功能工具对象
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
     * 创建按键按下工具
     * <p>
     * 模拟按下并释放单个键盘按键，支持字母、数字、功能键、方向键等。
     * </p>
     *
     * @return 按键按下功能工具对象
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
     * 创建文本输入工具
     * <p>
     * 模拟键盘逐个字符地输入文本字符串，支持英文字母、数字和常见标点符号。
     * </p>
     *
     * @return 文本输入功能工具对象
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
     * 创建组合键工具
     * <p>
     * 同时按下多个按键并逆序释放，用于执行快捷键操作（如 Ctrl+C、Alt+F4 等）。
     * </p>
     *
     * @return 组合键功能工具对象
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
     * 创建获取剪贴板内容工具
     * <p>
     * 读取系统剪贴板中的文本内容，如果剪贴板为空或包含非文本内容则返回空字符串。
     * </p>
     *
     * @return 获取剪贴板内容功能工具对象
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
                        final var clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
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
     * 创建设置剪贴板内容工具
     * <p>
     * 将指定文本设置到系统剪贴板中，可用于跨应用传递文本或输入非 ASCII 字符。
     * </p>
     *
     * @return 设置剪贴板内容功能工具对象
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

                        final var clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
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
     * 将按键名称字符串解析为 AWT KeyEvent 常量值
     * <p>
     * 支持以下类型的按键：
     * - 单字符：A-Z（字母）、0-9（数字）
     * - 功能键：F1-F12
     * - 特殊键：ENTER、ESCAPE、TAB、SPACE、BACK_SPACE、DELETE 等
     * - 方向键：UP、DOWN、LEFT、RIGHT
     * - 修饰键：CONTROL、ALT、SHIFT、META（Windows/Command 键）
     * - 其他：HOME、END、PAGE_UP、PAGE_DOWN、INSERT、CAPS_LOCK
     * </p>
     *
     * @param keyName 按键名称字符串，不区分大小写（如 "enter"、"CTRL"、"A"）
     * @return 对应的 KeyEvent 常量值（如 KeyEvent.VK_ENTER）
     * @throws IllegalArgumentException 当按键名称不支持或为空时抛出
     */
    private int parseKeyCode(String keyName) {
        if (keyName == null || keyName.isEmpty()) {
            throw new IllegalArgumentException("按键名称不能为空");
        }

        // 转换为大写并去除首尾空格，统一处理
        final String upperKey = keyName.toUpperCase().trim();

        // 处理单字符按键（字母 A-Z 和数字 0-9）
        if (upperKey.length() == 1) {
            final char c = upperKey.charAt(0);
            // 字母：通过 VK_A + 偏移量计算
            if (c >= 'A' && c <= 'Z') {
                return KeyEvent.VK_A + (c - 'A');
            }
            // 数字：通过 VK_0 + 偏移量计算
            if (c >= '0' && c <= '9') {
                return KeyEvent.VK_0 + (c - '0');
            }
        }

        // 处理特殊按键名称映射
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
     * 模拟键盘输入文本字符串
     * <p>
     * 逐个字符地模拟键盘按键，支持：
     * - 小写字母：直接按下对应键
     * - 大写字母：按住 Shift + 对应字母键
     * - 特殊符号：按住 Shift + 对应符号键（如 !@#$% 等）
     * - 换行符：按下 Enter 键
     * </p>
     * <p>
     * 注意：不支持中文等非 ASCII 字符，如需输入中文应使用剪贴板方案。
     * </p>
     *
     * @param text 要输入的文本字符串
     */
    private void typeString(String text) {
        // 遍历每个字符并模拟按键
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                // 情况1：换行符，按下 Enter 键
                robot.keyPress(KeyEvent.VK_ENTER);
                robot.delay(50);
                robot.keyRelease(KeyEvent.VK_ENTER);
            } else if (Character.isUpperCase(c) || isShiftRequired(c)) {
                // 情况2：需要 Shift 键的字符（大写字母或特殊符号）
                // 步骤1：按下 Shift 键
                robot.keyPress(KeyEvent.VK_SHIFT);
                robot.delay(10);

                // 步骤2：按下对应的字符键
                final int keyCode = getUpperCaseKeyCode(c);
                robot.keyPress(keyCode);
                robot.delay(50);
                robot.keyRelease(keyCode);

                // 步骤3：释放 Shift 键
                robot.delay(10);
                robot.keyRelease(KeyEvent.VK_SHIFT);
            } else {
                // 情况3：普通字符（小写字母、数字、简单符号），直接按下
                final int keyCode = getLowerCaseKeyCode(c);
                if (keyCode != -1) {
                    robot.keyPress(keyCode);
                    robot.delay(50);
                    robot.keyRelease(keyCode);
                }
            }
            // 字符之间的延迟，模拟真实打字速度
            robot.delay(20);
        }
    }

    /**
     * 判断字符是否需要按住 Shift 键才能输入
     * <p>
     * 需要 Shift 的字符包括：
     * - 特殊符号：!@#$%^&*()_+{}|:"<>?~
     * </p>
     *
     * @param c 待判断的字符
     * @return true 如果需要 Shift 键，false 否则
     */
    private boolean isShiftRequired(char c) {
        return "!@#$%^&*()_+{}|:\"<>?~".indexOf(c) != -1;
    }

    /**
     * 获取大写字母或需要 Shift 键的符号对应的按键码
     * <p>
     * 此方法只返回基础按键码，不包含 Shift 键状态。
     * 调用者需要配合 Shift 键一起使用。
     * </p>
     * <p>
     * 示例：
     * - 'A' -> VK_A（需配合 Shift）
     * - '!' -> VK_1（需配合 Shift）
     * - '@' -> VK_2（需配合 Shift）
     * </p>
     *
     * @param c 大写字符或特殊符号
     * @return 对应的 KeyEvent 按键码，如果不支持则返回 -1
     */
    private int getUpperCaseKeyCode(char c) {
        // 大写字母：通过 VK_A + 偏移量计算
        if (c >= 'A' && c <= 'Z') {
            return KeyEvent.VK_A + (c - 'A');
        }

        // 特殊符号映射到对应的数字键或符号键
        return switch (c) {
            case '!' -> KeyEvent.VK_1;      // Shift + 1
            case '@' -> KeyEvent.VK_2;      // Shift + 2
            case '#' -> KeyEvent.VK_3;      // Shift + 3
            case '$' -> KeyEvent.VK_4;      // Shift + 4
            case '%' -> KeyEvent.VK_5;      // Shift + 5
            case '^' -> KeyEvent.VK_6;      // Shift + 6
            case '&' -> KeyEvent.VK_7;      // Shift + 7
            case '*' -> KeyEvent.VK_8;      // Shift + 8
            case '(' -> KeyEvent.VK_9;      // Shift + 9
            case ')' -> KeyEvent.VK_0;      // Shift + 0
            case '_' -> KeyEvent.VK_MINUS;  // Shift + -
            case '+' -> KeyEvent.VK_EQUALS; // Shift + =
            case '{' -> KeyEvent.VK_OPEN_BRACKET;   // Shift + [
            case '}' -> KeyEvent.VK_CLOSE_BRACKET;  // Shift + ]
            case '|' -> KeyEvent.VK_BACK_SLASH;     // Shift + \
            case ':' -> KeyEvent.VK_SEMICOLON;      // Shift + ;
            case '"' -> KeyEvent.VK_QUOTE;          // Shift + '
            case '<' -> KeyEvent.VK_COMMA;          // Shift + ,
            case '>' -> KeyEvent.VK_PERIOD;         // Shift + .
            case '?' -> KeyEvent.VK_SLASH;          // Shift + /
            case '~' -> KeyEvent.VK_BACK_QUOTE;     // Shift + `
            default -> -1;  // 不支持的字符
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
     * 工具执行的统一结果封装
     * <p>
     * 所有工具方法的返回值都使用此结构，包含三种状态：
     * - 成功：error 和 message 为 null，data 包含实际数据
     * - 失败：error 包含错误码，message 包含错误描述，data 为 null
     * </p>
     *
     * @param error   错误码，成功时为 null，失败时为非空字符串（如 "SCREENSHOT_FAILED"）
     * @param message 错误消息，成功时为 null，失败时为详细的错误描述
     * @param data    成功时的返回数据，失败时为 null
     */
    record Result(
            @JsonProperty("error")
            String error,

            @JsonProperty("message")
            String message,

            @JsonProperty("data")
            Object data
    ) {
        /**
         * 创建成功结果
         *
         * @param data 返回的业务数据
         * @return 成功结果对象
         */
        static Result success(Object data) {
            return new Result(null, null, data);
        }

        /**
         * 创建失败结果
         *
         * @param error   错误码，用于程序化判断错误类型
         * @param message 错误消息，用于人类阅读的错误描述
         * @return 失败结果对象
         */
        static Result error(String error, String message) {
            return new Result(error, message, null);
        }
    }

    // ==================== Builder ====================

    /**
     * 创建构建器实例
     * <p>
     * 使用示例：
     * <pre>{@code
     * GuiToolkit kit = GuiToolkit.newBuilder()
     *     .enableScreenshot(true)
     *     .enableMouse(true)
     *     .enableKeyboard(false)  // 禁用键盘操作
     *     .enableClipboard(true)
     *     .build();
     * }</pre>
     * </p>
     *
     * @return 新的构建器实例，默认启用所有功能
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * GUI 工具包构建器
     * <p>
     * 采用建造者模式，允许灵活配置各项功能的启用状态。
     * 所有功能默认启用，可根据需要选择性禁用。
     * </p>
     */
    public static class Builder implements Buildable<GuiToolkit, Builder> {
        // 各项功能的启用标志，默认全部启用
        private boolean enableScreenshot = true;
        private boolean enableMouse = true;
        private boolean enableKeyboard = true;
        private boolean enableClipboard = true;

        /**
         * 配置是否启用截图功能
         * <p>
         * 禁用后将不提供 gui$screenshot 工具。
         * 适用于不需要屏幕识别能力的场景，可减少工具数量。
         * </p>
         *
         * @param enable true 启用，false 禁用
         * @return 当前构建器实例，支持链式调用
         */
        public Builder enableScreenshot(boolean enable) {
            this.enableScreenshot = enable;
            return this;
        }

        /**
         * 配置是否启用鼠标操作功能
         * <p>
         * 禁用后将不提供以下工具：
         * - gui$mouse$move（移动鼠标）
         * - gui$mouse$click（点击鼠标）
         * - gui$mouse$drag（拖拽鼠标）
         * - gui$mouse$scroll（滚动滚轮）
         * </p>
         * <p>
         * 适用于只需要键盘操作或截图的场景。
         * </p>
         *
         * @param enable true 启用，false 禁用
         * @return 当前构建器实例，支持链式调用
         */
        public Builder enableMouse(boolean enable) {
            this.enableMouse = enable;
            return this;
        }

        /**
         * 配置是否启用键盘操作功能
         * <p>
         * 禁用后将不提供以下工具：
         * - gui$key$press（按下按键）
         * - gui$key$type（输入文本）
         * - gui$key$combo（组合键）
         * </p>
         * <p>
         * 适用于只需要鼠标操作或截图的场景。
         * </p>
         *
         * @param enable true 启用，false 禁用
         * @return 当前构建器实例，支持链式调用
         */
        public Builder enableKeyboard(boolean enable) {
            this.enableKeyboard = enable;
            return this;
        }

        /**
         * 配置是否启用剪贴板操作功能
         * <p>
         * 禁用后将不提供以下工具：
         * - gui$clipboard$get（获取剪贴板内容）
         * - gui$clipboard$set（设置剪贴板内容）
         * </p>
         * <p>
         * 适用于不需要跨应用传递文本的场景。
         * 注意：如需输入中文等非 ASCII 字符，建议启用剪贴板功能。
         * </p>
         *
         * @param enable true 启用，false 禁用
         * @return 当前构建器实例，支持链式调用
         */
        public Builder enableClipboard(boolean enable) {
            this.enableClipboard = enable;
            return this;
        }

        /**
         * 根据当前配置构建 GuiToolkit 实例
         * <p>
         * 此方法会初始化 AWT Robot，如果在不支持图形界面的环境
         * （如无头服务器）中调用，将抛出 RuntimeException。
         * </p>
         *
         * @return 配置好的 GuiToolkit 实例
         * @throws RuntimeException 当 AWT Robot 初始化失败时抛出
         */
        @Override
        public GuiToolkit build() {
            return new GuiToolkit(this);
        }
    }

}
