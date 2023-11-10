---
sidebar_position: 5
title: DTO转换 
---

即使实体对象支持了动态性以表达任意形状的数据结构，和DTO之间的转换也是无法完全避免的。

Jimmer提供动态实体，可以很好地解决很大一部分DTO爆炸问题。所以，一般情况下不需要定义输出型的DTO类型来表达查询返回结果。

然而，并非所有DTO类型都可以被消灭，其中，输入型的DTO对象很难去除。

>   以GraphQL为例，虽然从output的角度讲，为客户端返回动态的`GraphQLObject`数据；但是，从input的角度讲，接受客户端提交的静态的`GraphQLInput`数据。
>   
>   GraphQL协议为什么将`GraphQLInput`定义为静态类型呢？是因为API的明确性和系统的安全性是非常重要需求，请参考[动态对象作为输入参数的问题](../../mutation/save-command/input-dto/problem)。
>
>   GraphQL协议面对的问题，Jimmer也同样需要面对，必须给出完整的解决方案。

作为一个综合性解决方案，Jimmer不局限于ORM本身，而是为整个项目的考虑，为解决此问题，提供了两种途径。

-   DTO语言

    为Jimmer量身定制的方案，具有极高的开发效率。

    [DTO语言](./dto-language.mdx)是为了无法被消灭的那部分DTO类型而设计，目的是为了它们变得极其廉价。

-   MapStruct

    结合[MapStruct](https://mapstruct.org/)框架的方案，能实现任意复杂的转化逻辑。