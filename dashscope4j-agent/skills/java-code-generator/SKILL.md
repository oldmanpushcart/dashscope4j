---
name: java-code-generator
description: Java 开发工作流指南，指导如何完成 Java 项目的创建、编辑、编译和运行
version: 1.0.0
author: Dashscope4j Team
---

# Java 开发工作流

本 Skill 指导你如何完成 Java 开发任务，包括文件操作、代码编辑、编译和运行。

## 核心工作流程

### 阶段 1：项目结构规划
在开始编码前，先规划：
- 确定包名（package）和类名
- 设计目录结构
- 识别需要的依赖

### 阶段 2：文件创建与编辑
创建和编辑 Java 源文件

### 阶段 3：编译验证
使用编译工具验证代码正确性

### 阶段 4：运行测试
执行编译后的程序验证功能

## 文件操作

### 创建文件

创建新的 Java 源文件时，需要：

1. **确保目录存在**
   - 如果目标目录不存在，先创建目录结构
   - 目录结构应符合 Java 包命名规范

2. **编写文件内容**
   - 包含正确的 package 声明
   - 添加必要的 import 语句
   - 编写完整的类定义

3. **示例：创建 UserService.java**
   
   文件路径：`src/main/java/com/example/UserService.java`
   
   ```java
   package com.example;
   
   public class UserService {
       public String greet(String name) {
           return "Hello, " + name;
       }
   }
   ```

**注意事项**：
- 文件路径必须符合 Java 包结构规范
- 内容必须是完整的、可编译的 Java 代码
- 文件名必须与 public 类名一致

### 读取文件

在修改现有代码前，应该先读取文件内容：

- 了解现有代码结构
- 确认需要修改的位置
- 检查代码是否符合预期

**示例**：读取 `src/main/java/com/example/UserService.java` 查看当前实现

### 编辑文件

修改现有 Java 文件时：

1. **精确定位要修改的代码段**
   - 找到需要修改的具体位置
   - 确保匹配原文（包括空格和换行）

2. **执行替换操作**
   - 可以一次替换多处
   - 保持逻辑连贯性

3. **验证修改结果**
   - 重新读取文件确认修改已生效
   - 检查是否有语法错误

**示例**：为 UserService 添加 logout 方法

原始代码：
```java
    }
}
```

修改为：
```java
    }
    
    public void logout(String userId) {
        // 登出逻辑
    }
}
```

### 查看目录结构

使用目录列表功能：
- 确认文件是否创建成功
- 了解项目组织结构
- 查找特定文件位置

**示例**：查看 `src/main/java/com/example` 目录下的所有文件

## 编译与运行

### 编译 Java 代码

#### 方式 1：使用 javac 命令

**基本用法**：
```bash
javac -d target/classes src/main/java/com/example/UserService.java
```

**带 classpath 编译**（有外部依赖时）：
```bash
javac -cp lib/*.jar -d target/classes src/main/java/com/example/*.java
```

**编译整个项目**：
```bash
find src/main/java -name '*.java' | xargs javac -d target/classes
```

#### 方式 2：使用 Maven

对于 Maven 项目，直接使用：
```bash
mvn clean compile
```

**注意事项**：
- 确保所有依赖都在 classpath 中
- 检查编译错误并修复
- 编译成功后再进行下一步

### 运行 Java 程序

#### 方式 1：使用 java 命令

**基本用法**：
```bash
java -cp target/classes com.example.Main
```

**带参数运行**：
```bash
java -cp target/classes com.example.Main arg1 arg2
```

**带 classpath 和依赖**：
```bash
java -cp 'target/classes:lib/*.jar' com.example.Main
```

#### 方式 2：使用 Maven

对于 Maven 项目：
```bash
mvn exec:java -Dexec.mainClass=com.example.Main
```

### 常用命令

以下是一些常用的 shell 命令：

**创建目录**：
```bash
mkdir -p src/main/java/com/example/service
```

**清理编译输出**：
```bash
rm -rf target/classes/*
```

**查看文件列表**：
```bash
ls -la src/main/java/com/example
```

**查看文件内容**：
```bash
cat src/main/java/com/example/UserService.java
```

**运行测试**：
```bash
mvn test
```

**打包项目**：
```bash
mvn package
```

## 典型工作流示例

### 场景 1：创建新的 Java 类

**需求**：创建一个 UserService 类，包含添加和查询用户的方法

**步骤**：

1. **创建目录结构**
   
   执行命令创建目录：
   ```bash
   mkdir -p src/main/java/com/example/service
   ```

2. **创建 Java 文件**
   
   在 `src/main/java/com/example/service/UserService.java` 创建文件，内容为：
   ```java
   package com.example.service;
   
   import java.util.HashMap;
   import java.util.Map;
   
   public class UserService {
       private Map<String, String> users = new HashMap<>();
       
       public void addUser(String id, String name) {
           users.put(id, name);
       }
       
       public String getUser(String id) {
           return users.get(id);
       }
   }
   ```

3. **验证文件创建**
   
   读取文件内容，确认文件已正确创建

4. **编译验证**
   
   执行编译命令：
   ```bash
   javac -d target/classes src/main/java/com/example/service/UserService.java
   ```

如果编译成功，说明代码没有问题。如果失败，查看错误信息并修复。

### 场景 2：修改现有代码

**需求**：给 UserService 添加删除用户的功能

**步骤**：

1. **读取现有代码**
   
   读取 `src/main/java/com/example/service/UserService.java` 了解当前实现

2. **编辑文件添加新方法**
   
   在文件中找到合适位置，添加 deleteUser 方法：
   ```java
   public boolean deleteUser(String id) {
       return users.remove(id) != null;
   }
   ```

3. **验证修改**
   
   重新读取文件，确认修改已生效

4. **重新编译**
   
   执行编译命令验证代码正确性：
   ```bash
   javac -d target/classes src/main/java/com/example/service/UserService.java
   ```

### 场景 3：创建完整 Maven 项目

**需求**：创建一个 Maven 项目，包含主类和单元测试

**步骤**：

1. **创建项目目录结构**
   
   执行命令创建目录：
   ```bash
   mkdir -p myproject/src/main/java/com/example
   mkdir -p myproject/src/test/java/com/example
   ```

2. **创建 pom.xml**
   
   在 `myproject/pom.xml` 创建 Maven 配置文件：
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <project xmlns="http://maven.apache.org/POM/4.0.0">
       <modelVersion>4.0.0</modelVersion>
       <groupId>com.example</groupId>
       <artifactId>myproject</artifactId>
       <version>1.0.0</version>
       
       <properties>
           <maven.compiler.source>17</maven.compiler.source>
           <maven.compiler.target>17</maven.compiler.target>
       </properties>
   </project>
   ```

3. **创建主类**
   
   在 `myproject/src/main/java/com/example/Main.java` 创建主类：
   ```java
   package com.example;
   
   public class Main {
       public static void main(String[] args) {
           System.out.println("Hello World");
       }
   }
   ```

4. **使用 Maven 编译**
   
   进入项目目录并编译：
   ```bash
   cd myproject && mvn clean compile
   ```

5. **运行程序**
   
   执行主类：
   ```bash
   cd myproject && mvn exec:java -Dexec.mainClass=com.example.Main
   ```

## 最佳实践

### 1. 文件命名规范
- Java 文件名必须与 public 类名一致
- 遵循驼峰命名法（UserService.java）
- 包名使用小写（com.example.service）

### 2. 目录结构规范
```
project/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── example/
│   │               └── *.java
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── *Test.java
├── target/
│   └── classes/
└── pom.xml (如果是 Maven 项目)
```

### 3. 编译前检查清单
- [ ] 所有 import 语句正确
- [ ] 包声明与目录结构匹配
- [ ] 语法正确（括号匹配、分号等）
- [ ] 依赖的类已编译或存在

### 4. 错误处理策略
- 编译失败时，仔细阅读错误信息
- 定位到具体文件和行号
- 查看问题代码
- 修复错误
- 重新编译验证

### 5. 操作选择原则
- **简单文件操作**: 直接创建、读取、编辑文件
- **批量操作**: 使用 shell 命令
- **Maven 项目**: 优先使用 mvn 命令
- **快速验证**: 使用 javac 直接编译单个文件

## 常见问题与解决

### Q1: 编译时找不到类
**原因**: classpath 设置错误或依赖缺失

**解决**: 
```bash
# 检查 classpath
javac -cp lib/*.jar -d target/classes src/**/*.java
```

### Q2: 包声明与目录不匹配
**原因**: package 语句与文件路径不一致

**解决**: 确保 `package com.example;` 对应 `com/example/` 目录

### Q3: 编辑后编译仍报错
**原因**: 可能编辑未生效或有其他依赖错误

**解决**:
1. 确认编辑已保存
2. 清理编译输出: `rm -rf target/classes/*`
3. 重新编译所有相关文件

## 注意事项

⚠️ **重要提醒**:
1. 每次编辑后都要验证（读取文件或编译）
2. 保持文件路径与包声明一致
3. 编译成功后再考虑运行
4. 遇到错误时逐步排查，不要跳过
5. 对于复杂项目，使用 Maven/Gradle 管理

❌ **禁止行为**:
- 不要假设文件已创建成功，必须验证
- 不要在编译失败时强行运行
- 不要忽略编译警告（可能是潜在问题）
- 不要手动修改 .class 文件

## 操作优先级

推荐的操作顺序：

1. **文件操作**: 
   - 创建文件
   - 读取文件
   - 编辑文件
   - 查看目录

2. **编译**: 
   - Maven 项目: `mvn compile`
   - 单文件: `javac`
   - 通用: 执行 shell 命令

3. **运行**: 
   - Maven 项目: `mvn exec:java`
   - 直接运行: `java -cp ...`
   - 通用: 执行 shell 命令

4. **验证**: 
   - 查看文件内容
   - 查看目录结构
   - 执行命令检查结果
