# DDL 编译器维护

README.md 是模块使用入口，公开入口为 KSP 的 `JimmerDdlCompilerProcessorProvider`、APT 的 `JimmerDdlCompilerAptProcessor` 及共享的 `JimmerDdlCompiler`。消费方通过 `ksp("org.babyfish.jimmer:jimmer-ddl-compiler:<version>")` 或 `annotationProcessor(...)` 接入。

生产依赖由 `gradle/libs.versions.toml` 管理，包含 LSI、ddlgenerator 的核心/适配器/方言、KSP API 和数据库模型。不要在共享编译器中依赖原生 KSP/APT 符号；跨处理轮次的元数据必须经过稳定快照，`LsiField.isComputed` 也必须保留。普通计算 getter 的排除规则由 ddlgenerator 适配器实现。

KSP 在处理期间收集并冻结元数据，在 finish 阶段生成。快照目录与增量迁移遵循 README 的约定；JDBC 连接按已有 use 生命周期关闭，错误沿现有编译诊断路径报告。禁止在测试外嵌入业务数据库、Flyway 启动或资源同步逻辑。

在 project 目录执行 `./gradlew :jimmer-ddl-compiler:test`。Kotlin 回归同时运行真实 Jimmer 与 DDL 处理器，覆盖建表、增量、无变化重复生成、ORM 元数据以及无计算列数据库上的真实查询。编译器测试依赖 JDK 编译工具。
