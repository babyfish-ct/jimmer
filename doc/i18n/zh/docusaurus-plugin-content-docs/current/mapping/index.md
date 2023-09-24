---
sidebar_position: 2
title: 映射篇
---

在这个章节中，我们会介绍如何映射关系性数据库和实体模型。

-   对于有ORM经验 *(尤其是JPA经验)* 的读者，可以快速阅读。

    和JPA相比，区别很大的内容有：

    -   基础映射：
        -   [可空性](./base/nullity.mdx)

    -   高级映射
        -   [视图属性](./advanced/view/)

        -   [计算属性](./advanced/calculated/)

        -   [远程关联](./advanced/remote)

        -   [Key](./advanced/key)
        
            Key对[保存指令](../mutation/save-command/)而言非常重要

        -   [OnDissociate](./advanced/on-dissociate)

-   对于没有ORM经验的读者，只能慢慢阅读。

    这个过程是枯燥的，但这是任何ORM学习必经之路，ORM中无论多么强大和酷炫的功能，都必须以这些映射作为基础。

    为避免长时间枯燥，给没有任何ORM经验的初学者一个建议。先只阅读基础映射，保证能阅读本文档的大部分内容；后续接触到需要高级映射的内容时再回头查看。