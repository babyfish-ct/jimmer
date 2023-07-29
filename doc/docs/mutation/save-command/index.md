---
sidebar_position: 3
title: Save command
---

保存指令是Jimmer一个非常强大的能力，可以大幅简化复杂数据结构保存业务的开发难度。

如果说[对象抓取器](../../query/object-fetcher)让作为输出信息的数据结构可以是任何形状，那么，保存指令就是让作为输入信息的数据结构也可以是任何形状。

如果读者对Web前端技术有一定的了解，可以用[React](https://react.dev/)或[Vue](https://vuejs.org/)中的`Virtual DOM diff`来类比。

调用保存指令只需要让一行代码，但其内部隐藏了千遍万化的细节，文档难以穷举大量案例。因此，保存指令有独立的示例项目：

-   Java: [example/java/save-command](https://github.com/babyfish-ct/jimmer/tree/main/example/java/save-command)

-   Kotlin: [example/kotlin/save-command-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/save-command-kt)

用IDE随意打开其中一个，运行单元测试即可。