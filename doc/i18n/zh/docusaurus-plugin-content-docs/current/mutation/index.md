---
sidebar_position: 4
title: 修改篇
---

在这个章节中，我们会介绍Jimmer中修改数据库相关的所有功能。

其中，有两个功能具备颠覆性。

-   [保持指令](./save-command)

    一句话保存任意复杂的数据结构，自动找出DIFF并修改数据库，类似于React/Vue。

-   [触发器](./trigger)

    无论通过Jimmer自身能力还是借助现有CDC方案，都能感知数据的变化。这也是[缓存](../cache)自动清理的基础。