---
sidebar_position: 4
title: jimmer-sql子项目
---

:::tip
对于中文版，从本文档中挑取最重要的一部分，形成[B站视频集](https://www.bilibili.com/video/BV1dA4y1R7pV/?vd_source=60313648ad86e28e629f98e944f7fa2a)
:::

jimmer-sql是基于jimmer-core不可变对象的ORM框架。

长期以来，因为受限于数据实体的表达能力，众多ORM框架的能力难以有重大突破。

jimmer-core提供的数据实体有足够的表达能力，因此jimmer-sql能够提供更多强大的能力。

jimmer-sql提供了JPA、myBatis、JOOQ、exposed和ktorm等技术不提供的强大功能。

jimmer-sql的主要特性

1. 和JPA Criteria, QueryDSL以及JOOQ一样，强类型的SQL SDL，力求在编译时发现SQL错误，而非运行时、调试时、测试时。

2. 强类型SQL DSL和Native SQL可以自由混编，鼓励使用数据库特有功能

   请点击[这里](./expression#native-sql表达式)

3. 永远使用高性能的[ResultSet.getObject(int)](https://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html#getObject(int))；而非相对低效的[ResultSet.getObject(String)](https://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html#getObject(java.lang.String))

4. 隐式的动态的表连接，在复杂的动态查询实现中，自动合并不同代码逻辑分支中冲突的表连接 *（即便使用以可控性很强的myBatis，也难以实现此功能）*

   请点击[这里](./table-join#动态连接)

5. 通用的分页查询需要两条SQL，一条查数据，一条查行数。开发人员负责前者，后者由框架自动生成，自动优化。

   请点击[这里](./query/pagination)

6. **对象抓取器**：扩展SQL的能力，如果查询中某列是对象类型，可以指定为对象的查询格式，接受任意的关联深度和广度、甚至可以递归查询自关联属性。可以认为SQL被扩展到足够强大的的形态，具备和GraphQL相媲美的能力。

   请点击[这里](./query/fetcher)

7. **Sava指令**：被保存（插入或修改）的数据不再是简单的对象，而是一颗任意复杂的对象树。无论这棵树有多复杂，框架负责所有内部细节，开发人员只需一句话即可完成整体操作。这个功能是对象抓取器的逆功能。

   请点击[这里](./mutation/save-command)

8. 可外接任何缓存系统。默认情况下无缓存，只是一个非常轻量而强大的SQL生成器；但是用户可以挂接任何缓存，**并和业务系统的自身缓存技术保持一致**。和其他ORM不同，不仅支持对象缓存，而且支持关联缓存，且对对象抓取器有效，尤其是递归查询自关联属性时。

  > 这是一个新加的功能，目前暂时没有更细节的文档，会尽快补充。

9. 非常轻量，内部实现比myBatis还轻量，无任何反射行为，无任何动态字节码生成行为，保证graal友好

10. 对Spring GraphQL的支持。

    Spring Boot 2.7.0带来了Spring GraphQL，jimmer-sql为了提供了专门的API，加快Spring GraphQL的开发。

    请查看[这里](./spring-graphql.md)