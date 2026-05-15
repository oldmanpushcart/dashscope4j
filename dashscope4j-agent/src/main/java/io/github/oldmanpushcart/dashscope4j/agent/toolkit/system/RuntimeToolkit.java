package io.github.oldmanpushcart.dashscope4j.agent.toolkit.system;

import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时工具包
 * <p>
 * 提供运行时环境信息给 LLM 使用：
 * - datetime: 获取当前日期时间
 * - os: 获取操作系统信息
 * - env: 获取环境变量
 * </p>
 */
public class RuntimeToolkit implements Toolkit {

    @Override
    public List<Tool> tools() {
        return List.of(
                os(),
                env(),
                datetime()
        );
    }

    // ==================== Builder ====================

    public static RuntimeToolkit create() {
        return new RuntimeToolkit();
    }

    /**
     * 创建 runtime$os 工具
     */
    private FunctionTool os() {
        return FunctionTool.newBuilder()
                .name("runtime$os")
                .description("""
                        获取当前操作系统的详细信息。
                        
                        【使用场景】
                        - 判断运行平台（Windows/Linux/Mac）
                        - 获取系统架构（x86_64/aarch64）
                        - 了解 Java 运行时环境
                        
                        【返回结果】
                        - 包含多个系统属性的 Map：
                          * os.name: 操作系统名称
                          * os.version: 操作系统版本
                          * os.arch: 系统架构
                          * java.version: Java 版本
                          * user.dir: 工作目录
                          * 等等...
                        
                        【注意事项】
                        - 无需参数
                        - 返回完整的系统属性列表
                        """)
                .supplier(System::getProperties)
                .build();
    }

    /**
     * 创建 runtime$env 工具
     */
    private FunctionTool env() {
        return FunctionTool.newBuilder()
                .name("runtime$env")
                .description("""
                        获取当前进程的环境变量列表。
                        
                        【使用场景】
                        - 查看配置的环境变量
                        - 调试环境问题
                        - 获取 PATH、HOME 等关键变量
                        
                        【返回结果】
                        - 包含所有环境变量的 Map
                        - Key: 环境变量名
                        - Value: 环境变量值
                        
                        【注意事项】
                        - 无需参数
                        - 敏感变量（如密码）也会被返回，请注意安全
                        - 不同操作系统环境变量名大小写敏感性不同
                        """)
                .supplier(() -> System.getenv())
                .build();
    }

    /**
     * 创建 runtime$datetime 工具
     */
    private FunctionTool datetime() {
        return FunctionTool.newBuilder()
                .name("runtime$datetime")
                .description("""
                        获取当前系统的完整日期和时间信息。
                        
                        【使用场景】
                        - 回答关于当前时间、日期、星期几的问题
                        - 计算相对时间（如“上周三”、“下个月”）
                        - 为日志、文件命名添加时间戳
                        - 判断是否为工作日、周末
                        - 获取时区相关信息
                        
                        【返回结果】
                        包含以下完整信息的结构化数据：
                        - current_datetime: ISO 8601 格式的完整日期时间（含时区）
                        - date: 日期部分（YYYY-MM-DD）
                        - time: 时间部分（HH:mm:ss.SSS）
                        - timezone: 时区信息（如 Asia/Shanghai）
                        - timezone_offset: UTC 偏移量（如 +08:00）
                        - day_of_week: 星期几（中文，如“星期三”）
                        - day_of_week_en: 星期几（英文，如 Wednesday）
                        - day_of_year: 一年中的第几天（1-366）
                        - week_of_year: 一年中的第几周
                        - is_weekend: 是否为周末
                        - is_leap_year: 是否为闰年
                        - quarter: 当前季度（Q1-Q4）
                        - unix_timestamp: Unix 时间戳（秒）
                        
                        【典型应用场景】
                        1. 用户问“今天是星期几？” → 查看 day_of_week
                        2. 用户说“帮我安排下周三的会议” → 结合 current_datetime 和 day_of_week 推算
                        3. 需要生成带日期的文件名 → 使用 date 或 current_datetime
                        4. 判断是否在工作时间 → 结合 time 和 is_weekend
                        
                        【注意事项】
                        - 无需参数
                        - 返回服务器本地时间
                        - 所有字段都经过格式化，可直接用于推理
                        """)
                .supplier(() -> {
                    final ZonedDateTime now = ZonedDateTime.now();
                    final LocalDate date = now.toLocalDate();
                    final LocalTime time = now.toLocalTime();
                    final DayOfWeek dayOfWeek = date.getDayOfWeek();
                    final ZoneId zone = now.getZone();

                    // 获取周数（使用 ISO 标准）
                    final WeekFields weekFields = WeekFields.ISO;
                    final int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());

                    // 判断是否为闰年
                    final boolean isLeapYear = date.isLeapYear();

                    // 计算季度
                    final int month = date.getMonthValue();
                    final String quarter = "Q" + ((month - 1) / 3 + 1);

                    // 构建完整的日期时间信息
                    final Map<String, Object> dateTimeInfo = new HashMap<>();
                    dateTimeInfo.put("current_datetime", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    dateTimeInfo.put("date", date.toString());
                    dateTimeInfo.put("time", time.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
                    dateTimeInfo.put("timezone", zone.getId());
                    dateTimeInfo.put("timezone_offset", now.format(DateTimeFormatter.ofPattern("XXX")));
                    dateTimeInfo.put("day_of_week", dayOfWeek);
                    dateTimeInfo.put("day_of_week_en", dayOfWeek.toString());
                    dateTimeInfo.put("day_of_year", date.getDayOfYear());
                    dateTimeInfo.put("week_of_year", weekOfYear);
                    dateTimeInfo.put("is_weekend", dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
                    dateTimeInfo.put("is_leap_year", isLeapYear);
                    dateTimeInfo.put("quarter", quarter);
                    dateTimeInfo.put("unix_timestamp", now.toEpochSecond());

                    return dateTimeInfo;
                })
                .build();
    }

}
