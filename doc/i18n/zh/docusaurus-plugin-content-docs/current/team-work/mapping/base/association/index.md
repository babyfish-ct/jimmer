---
sidebar_position: 3
title: 关联映射
---

在这里，你会了解到ORM最重要的能力：实体关系关系映射。你会了解到这些注解：

-   org.babyfish.jimmer.sql.OneToOne
-   org.babyfish.jimmer.sql.ManyToOne
-   org.babyfish.jimmer.sql.OneToMany
-   org.babyfish.jimmer.sql.ManyToMany
-   org.babyfish.jimmer.sql.JoinColumn
-   org.babyfish.jimmer.sql.JoinTable

:::caution
对于关联而言，实体类型中的基础属性的类型应该是关联对象，而非关联Id。

如果想定义关联Id属性，请

-   先按本目录的文档完成关联映射

-   再按照[IdView](../../advanced/view/id-view)文档添加关联Id属性
:::