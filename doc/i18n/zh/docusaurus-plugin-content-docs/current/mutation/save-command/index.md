---
sidebar_position: 3
title: 保存指令
---

保存指令是Jimmer一个非常强大的能力，可以大幅简化复杂数据结构保存业务的开发难度。

如果说[对象抓取器](../../query/object-fetcher)让作为输出信息的数据结构可以是任何形状，那么，保存指令就是让作为输入信息的数据结构也可以是任何形状。

如果读者对Web前端技术有一定的了解，可以用[React](https://react.dev/)或[Vue](https://vuejs.org/)中的`Virtual DOM diff`来类比。

:::tip
保存指令需要开发人员**彻底改变过去的思维模式**

-   以前的传统思维模式

    自己去对比要保存的数据结构和数据库现有数据的差异，对有变化的局部执行`INSERT`、`UPDATE`或`DELETE`

-   保存指令的思维模式

    接受客户端传递的数据结构，作为一个整体保存即可。Jimmer会处理好一切 *(自动对比要保存的数据结构和数据库现有数据的差异，对有变化的局部执行`INSERT`、`UPDATE`或`DELETE`)*
:::

调用保存指令只需要让一行代码，但其内部隐藏了千遍万化的细节，文档难以穷举大量案例。因此，保存指令有独立的示例项目：

-   Java: [example/java/save-command](https://github.com/babyfish-ct/jimmer/tree/main/example/java/save-command)

-   Kotlin: [example/kotlin/save-command-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/save-command-kt)

用IDE随意打开其中一个，运行单元测试即可。
