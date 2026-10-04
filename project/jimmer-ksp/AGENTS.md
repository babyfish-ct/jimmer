# KSP 模块维护

README.md 是消费入口，处理器公开入口为 `org.babyfish.jimmer.ksp.JimmerProcessorProvider`，通过 KSP 服务发现加载。生产依赖为 jimmer-core、jimmer-dto-compiler、KSP API、KotlinPoet 与 Jackson，版本统一由根版本目录维护。

沿用现有 Context、元数据校验和生成器边界；不要为局部缺陷重建处理器框架。普通 getter 必须先完成注解合法性校验，再排除出 ImmutableType 的属性集合；抽象属性与显式 Formula 仍由原逻辑建模。错误由 MetaException 和处理器诊断报告，不要静默吞掉非法注解。

测试使用真实 KSP 编译生成代码，且应检查运行时 getter 语义、继承属性及非法注解诊断。执行 `./gradlew :jimmer-ksp:test :jimmer-sql-kotlin:test`，涉及 DDL 时同时执行 `:jimmer-ddl-compiler:test`。内嵌 KSP 使用现有测试基类规定的 JVM 目标与 JDK 版本约定。
