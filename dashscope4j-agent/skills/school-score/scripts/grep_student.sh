#!/bin/bash

# grep_student.sh - 在 students.txt文件中搜索指定信息
# 用法：./grep_student.sh <搜索关键词>

# 检查是否提供了搜索关键词
if [ $# -eq 0 ]; then
    echo "错误：请提供搜索关键词" >&2
    echo "用法：$0 <搜索关键词>" >&2
    exit 1
fi

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 构建 students.txt文件的绝对路径
STUDENTS_FILE="${SCRIPT_DIR}/../assets/students.txt"

# 检查文件是否存在
if [ ! -f "$STUDENTS_FILE" ]; then
    echo "错误：找不到学生文件 $STUDENTS_FILE" >&2
    exit 1
fi

# 执行 grep 搜索并输出结果
grep "$@" "$STUDENTS_FILE"
