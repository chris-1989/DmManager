# 达梦数据库管理工具（dm-admin-tool）

轻量级 Java Swing 工具，提供 **多连接池管理**、**使用者与权限管理**、**DMP 批量导入** 三大功能模块。采用分层架构，集成 FlatLaf 现代外观。

## 环境需求

- JDK 8+
- Maven 3.6+
- 达梦 JDBC 驱动 `DmJdbcDriver18.jar`（放入 `lib/` 目录）

## 快速开始

### 1. 放置驱动

将 `DmJdbcDriver18.jar` 复制到项目 `lib/` 目录下。

### 2. 编译

```bash
cd dm-admin-tool
mvn clean package
```

### 3. 启动 GUI

```bash
java -cp "target/dm-admin-tool-1.0.0-SNAPSHOT-jar-with-dependencies.jar;lib/DmJdbcDriver18.jar" com.dmadmin.DmAdminApplication
```

启动后显示三页签界面：**连接**、**使用者**、**DMP 导入**。

> 使用者与 DMP 导入页签初始为锁定状态，需先在连接页签成功连接数据库后自动解锁。

---

## 功能模块

### 连接管理

- 注册命名连接池（HikariCP），可同时管理多个数据库连接
- 测试连接（`SELECT 1 FROM DUAL`，支持重试）
- 连接成功后自动解锁「使用者」和「DMP 导入」页签，连接失败则重新锁定

### 使用者管理

- **用户列表**：查询 `DBA_USERS`，双击可查看角色、系统权限、表级权限详情
- **单用户创建**：设置用户名、密码、表空间；可选密码策略
- **批量创建**：在配置文件中定义用户类别，一键批量创建
- **角色管理**：授予/回收角色，支持按类别批量授予
- **删除用户**：支持 CASCADE 级联删除
- **权限查询**：查询指定用户的完整权限列表

### DMP 批量导入

- **文件夹扫描**：选择一个文件夹，自动扫描其中所有 `.dmp` 文件并逐一导入
- **导入前校验**：每个 DMP 文件导入前检查目标用户在数据库中是否已有数据，有则跳过并记录日志
- **日志自动管理**：日志目录从配置读取，每个 DMP 文件自动生成独立日志（`<用户名>_<时间戳>.log`）
- **实时进度**：进度条按文件数推进，日志区显示每个文件的导入结果
- **连接信息自动填充**：连接页签成功后，切换到 DMP 导入页签时自动填入主机、端口、用户、密码

---

## 配置文件

主配置文件：`src/main/resources/application.properties`

```properties
# JDBC 驱动
dm.jdbc.driverClassName=dm.jdbc.driver.DmDriver

# 连接池参数
dm.pool.connectionTimeoutMs=30000
dm.pool.maximumPoolSize=10
dm.pool.minimumIdle=2

# 连接测试重试
dm.connection.testRetries=2
dm.connection.retryDelayMs=1000

# DMP 导入配置（路径请使用正斜杠 /）
dm.dimp.native.library.dir=D:/dmdbms/bin
dm.dimp.tool.path=D:/dmdbms/bin/dimp.exe
dm.dimp.log.dir=D:/dmdbms/logs/dmp
dm.dimp.native.charset=GB18030

# 批量建用户配置
dm.user.categories=dev,analyst,dba
dm.user.category.dev.name=开发组
dm.user.category.dev.users=USER1,USER2,USER3
dm.user.category.dev.password=YourPassword
dm.user.category.dev.tablespace=MAIN
```

> **注意**：配置值中的路径请使用正斜杠 `/` 或双反斜杠 `\\`，单个 `\` 会被 Java Properties 当作转义符吃掉。

本地覆盖配置：在项目根目录创建 `dm-admin-local.properties`，其中的键值会覆盖默认配置。

---

## 项目结构

```
com.dmadmin/
├── DmAdminApplication.java          # 入口（GUI / CLI 分发）
├── model/                           # 数据模型
│   ├── DbConnectionProfile.java     # 连接参数
│   ├── UserCreateRequest.java       # 用户创建请求
│   ├── UserSummary.java             # 用户列表行
│   └── DmpImportOptions.java         # 导入选项
├── pool/
│   └── ConnectionPoolManager.java   # HikariCP 连接池单例
├── service/
│   ├── ConnectionManagementService.java  # 连接注册与测试
│   ├── UserManagementService.java        # 用户 CRUD
│   └── DmpImportService.java             # DMP 导入编排
├── dao/
│   └── UserManagementDao.java       # 用户相关 SQL（DBA_USERS 等）
├── dmp/
│   ├── ImportMode.java              # 导入模式枚举
│   ├── DmpImportContext.java        # 导入上下文
│   ├── DmpImportHandler.java        # 导入处理器接口
│   ├── DmpImportHandlerFactory.java # 处理器工厂
│   ├── AbstractDmpImportHandler.java     # dimp.exe ProcessBuilder 基类
│   ├── TableDmpImportHandler.java        # 表级导入
│   ├── SchemaDmpImportHandler.java       # 模式级导入
│   ├── FullDatabaseDmpImportHandler.java # 全库导入
│   ├── ImportProgressListener.java       # 进度观察者接口
│   └── ImportProgressNotifier.java       # 进度事件分发
├── exception/                       # 统一异常体系
├── util/
│   ├── AppProperties.java           # 配置加载（UTF-8）
│   ├── PasswordStrengthValidator.java
│   └── SqlIdentifierValidator.java  # SQL 注入防护
└── ui/
    ├── DmAdminSwingLauncher.java    # GUI 启动（FlatLaf）
    ├── MainFrame.java               # 主窗口（三页签）
    ├── ConnectionPanel.java         # 连接页签
    ├── UserManagementPanel.java     # 使用者页签
    ├── DmpImportPanel.java          # DMP 批量导入页签
    └── SessionState.java            # 跨页签会话状态
```

---

## 设计模式

| 模式 | 应用 |
|------|------|
| 单例 | `ConnectionPoolManager` |
| 工厂 | `DmpImportHandlerFactory` → 按 `ImportMode` 创建处理器 |
| 观察者 | `ImportProgressNotifier` → `ImportProgressListener` 广播日志与进度 |
| 构建者 | `UserCreateRequest.Builder` |
| 模板方法 | `AbstractDmpImportHandler.invoke()` → 子类 `execute()` |

## 安全措施

- SQL 标识符白名单校验（`SqlIdentifierValidator`），防注入
- 密码强度校验（最少 9 位，含小写字母和数字）
- 所有 JDBC 操作使用 `try-with-resources`

---

## 命令行模式

```bash
# 显示帮助
java -cp "..." com.dmadmin.DmAdminApplication help

# 测试连接
java -cp "..." com.dmadmin.DmAdminApplication test-connection mydm 127.0.0.1 5236 SYSDBA 密码
```

---

## 常见问题

| 现象 | 可能原因 | 处理方式 |
|------|----------|----------|
| 找不到 dm.jdbc.driver.DmDriver | 驱动 jar 不在 classpath | 将 DmJdbcDriver18.jar 放入 lib/ |
| 连接超时 | 防火墙、端口错误 | 确认端口（默认 5236）和网络 |
| DMP 导入跳过所有文件 | 目标用户在数据库中已有数据 | 检查用户是否有表（正常保护行为） |
| 配置中文路径乱码 | 使用反斜杠被转义 | 改用正斜杠 `/` 或双反斜杠 `\\` |
| 查询 DBA_USERS 失败 | 权限不足 | 使用 SYSDBA 或授权字典视图 |
