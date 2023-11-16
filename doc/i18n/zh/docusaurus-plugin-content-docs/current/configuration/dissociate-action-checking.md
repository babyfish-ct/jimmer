---
sidebar_position: 13
title: 默认脱钩方式
---

在[OnDissociate](../mapping/advanced/on-dissociate)一文中，我们知道有5种脱钩模式。

-   NONE *(默认)*

-   LAX

-   CHECK

-   SET_NULL

-   DELETE

对于伪外键 *(请参见[真假外键](../mapping/base/foreignkey))* 关联属性而言，当其脱钩模式为`NONE`时

-   如果全局配置`jimmer.default-dissociation-action-checkable`为false *(默认)*，等价于`NONE`。

-   如果全局配置`jimmer.default-dissociation-action-checkable`为true，等价于`CHECK`。
