---
sidebar_position: 8
title: 修改
---

jimmer-sql提供了两种修改方式

1. 修改语句

    修改语句对应SQL的update和delete语句，适用于逻辑简单但需要批量操作的场合。

2. 修改指令

    修改指令，适用于逻辑复杂的场合。
    
    其中save指令非常强大，如果说GraphQL是一个强大的动态树输出手段，那么save指令就是一个动态树输入手段。

## 目录

- [Update语句](./update-statement)
- [Delete语句](./delete-statement)
- [Save指令](./save-command)
- [Delete指令](./delete-command)
- [修改中间表](./association)
- [Draft拦截器](./interceptor)
- [触发器](./trigger)