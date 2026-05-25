# 达梦数据库管理工具（dm-admin-tool）

轻量级 Java 工具，依《达梦数据库管理工具开发》需求实现：**多连接 HikariCP 池**、**使用者与权限管理**、**DMP 导入（JNI）**，采用分层架构（DAO / Service / Util / Exception），并以工厂模式建立导入处理器、观察者模式广播导入日志与进度。另提供 **Swing 图形界面**（套件 `com.dmadmin.ui`，JDK 内建，无需 JavaFX 依赖）。

## 环境需求

- JDK 8 或以上
- Maven 3.6+
- 达梦官方 JDBC 驱动 `DmJdbcDriver18.jar`（执行时加入 classpath，勿提交至版本库）
- DMP 导入：达梦安装目录 `bin` 下的 **`DmDexpDimp.dll`**（Windows），且 JNI 函数名需与官方一致；若官方 DLL 导出符号与本项目 `com.dmadmin.dmp.DmDimpNativeBridge#dll_imp_dm` 不一致，需自行编写薄 C 桥接层（见源代码 JavaDoc）

## 编译

```text
cd dm-admin-tool
mvn clean package
```

编译不依赖本机是否已放置 `DmJdbcDriver18.jar`；执行连接或 SQL 前请将驱动放入 `lib/` 并加入 classpath。

## 执行

将 `DmJdbcDriver18.jar` 复制到 `lib/` 后。

### Swing 图形界面（预设）

无参数或明确指定 `gui` 即开启窗口（连接 / 使用者 / DMP 导入分页）：

```text
java -cp "target/classes;lib/DmJdbcDriver18.jar" com.dmadmin.DmAdminApplication
```

或：

```text
java -cp "target/classes;lib/DmJdbcDriver18.jar" com.dmadmin.DmAdminApplication gui
```

DMP 分页仍须正确设定 `dm.dimp.native.library.dir`（或系统可找到 `DmDexpDimp.dll`）。

### 命令列

```text
java -cp "target/classes;lib/DmJdbcDriver18.jar" com.dmadmin.DmAdminApplication help
```

测试连接（默认端口常为 **5236**）：

```text
java -cp "target/classes;lib/DmJdbcDriver18.jar" com.dmadmin.DmAdminApplication test-connection mydm 127.0.0.1 5236 SYSDBA 你的密码
```

DMP 导入演示（需本机可加载 `DmDexpDimp.dll`，并设定 `dm.dimp.native.library.dir` 或将 `bin` 加入 `PATH` / `java.library.path`）：

```text
java -Ddm.dimp.native.library.dir="C:\dmdbms\bin" -cp "target/classes;lib/DmJdbcDriver18.jar" com.dmadmin.DmAdminApplication demo-import 127.0.0.1 5236 SYSDBA 密码 TABLE MYSCHEMA MYTABLE C:\data\export.dmp C:\data\import.log
```

模式参数：`TABLE`、`SCHEMA`、`FULL`。表名或模式名不需要时可传 `null`。最后可选参数为模式映射字串，例如 `SRC_SCHEMA:DST_SCHEMA`。

## 设定档

- `src/main/resources/application.properties`：连接池、JNI 字元集、重试等
- 工作目录可再放 **`dm-admin-local.properties`** 覆写相同键值

## 程序结构摘要

| 套件 | 说明 |
|------|------|
| `com.dmadmin.pool` | `ConnectionPoolManager` 单例管理多个 HikariCP |
| `com.dmadmin.service` | `ConnectionManagementService`、`UserManagementService`、`DmpImportService` |
| `com.dmadmin.dao` | `UserManagementDao`（try-with-resources） |
| `com.dmadmin.dmp` | JNI 桥接、导入工厂、三种模式处理器、`ImportProgressNotifier` |
| `com.dmadmin.exception` | 统一 `DmAdminException` 体系 |
| `com.dmadmin.util` | 密码强度、识别符校验、属性加载 |

## 使用示例（程序内调用）

```java
AppProperties props = new AppProperties();
ConnectionPoolManager pools = ConnectionPoolManager.getInstance();
DbConnectionProfile profile = new DbConnectionProfile("id1", "127.0.0.1", 5236, "SYSDBA", "pwd");
new ConnectionManagementService(pools, props).registerConnection(profile);
new ConnectionManagementService(pools, props).testConnection("id1");

UserManagementService users = new UserManagementService("id1", pools);
UserCreateRequest req = UserCreateRequest.builder()
    .username("APPUSER")
    .password("Aa1!xxxxxx")
    .defaultTablespace("MAIN")
    .grantPublicRole(true)
    .build();
users.createUser(req);
```

## 达梦 JDBC 驱动安装说明

1. 从达梦安装目录或官网取得 `DmJdbcDriver18.jar`
2. 复制到本项目 `lib/DmJdbcDriver18.jar`（与 `PLACE_DM_JDBC_HERE.txt` 同目录说明）
3. 执行时使用 `-cp ...;lib/DmJdbcDriver18.jar`（Windows）或对应分隔符

亦可使用 `mvn install:install-file` 安装至本机 Maven 仓库后改为一般 `<dependency>`（本示例未内建该依赖，避免无 jar 时无法编译）。

## DMP JNI（DmDexpDimp.dll）配置

1. 确认数据库客户端/服务器安装中包含 **`DmDexpDimp.dll`**
2. 在 `application.properties` 设定 `dm.dimp.native.library.dir` 为含该 DLL 的**绝对路径目录**，或将该目录加入系统 `PATH`
3. 若出现 `UnsatisfiedLinkError`：多为路径错误、32/64 位元与 JVM 不一致，或 DLL 依赖项缺失（可用依赖检查工具排查）

## 注意事项

- 查询 `DBA_USERS`、`DBA_ROLE_PRIVS` 等需 **DBA 或相应字典检视权限**
- 使用者与模式在达梦中关联紧密；建立使用者通常会一并建立同名模式
- 动态 SQL 中的识别符已做白名单校验；密码仅做强度校验与双引号跳脱，请勿在不可信环境直接暴露日志
- `DBA_USERS.DEFAULT_INDEX_TABLESPACE` 若在你使用的版本中不存在，请调整 `UserManagementDao#listUsers` 的查询字段

## 导入失败“回滚”

JNI 层返回非 0 时，本工具仅记录 `rollbackOnFailure` 标志并写日志；**实际资料回滚**须依达梦备份/还原策略或导入前快照自行处理。

## 常见问题

| 现象 | 可能原因 | 处理方式 |
|------|----------|----------|
| ClassNotFoundException: dm.jdbc.driver.DmDriver | classpath 未含驱动 jar | 加入 `DmJdbcDriver18.jar` |
| 连接超时 | 防火墙、端口错误、服务未启 | 确认主机端口（预设 5236）与网络 |
| UnsatisfiedLinkError | 找不到 DmDexpDimp | 设定 `dm.dimp.native.library.dir` 或 PATH |
| 查询 DBA_USERS 失败 | 权限不足 | 使用 SYSDBA 或授权字典检视 |
| JNI 返回非 0 | dmp 与库版本不符、参数错误 | 查看指定日志档、核对模式与映射字串 |

## 单元测试

```text
mvn test
```

目前包含密码强度、识别符、导入工厂等与数据库无关的测试；整合测试需在具达梦实例的环境自行扩充。
