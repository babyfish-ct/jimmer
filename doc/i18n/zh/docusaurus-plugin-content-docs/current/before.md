---
sidebar_position: 1
title: 进入Jimmer世界之前
---

和JPA2.0 Criteria、QueryDsl、Fluent-MyBatis等强类型SQL DSL实现一样, Jimmer需要根据用户代码生成更多的源代码。

- 对于Java而言，就是annotation processor
- 对于kotlin而言，就是ksp

当用户第一次使用IDE（比如Intellij）打开项目配套的例子时，会发现一些本该被自动生成的类不存在。

**这时不要惧怕，点下运行按钮，所有错误会自动消失。**