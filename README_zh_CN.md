[![logo](logo.png)](https://babyfish-ct.github.io/jimmer/)

# 针对Java和Kotlin的革命性框架, 以及完整的集成解决方案

## 语言选择

[English](https://github.com/babyfish-ct/jimmer) | 中文

## 核心功能

-   便捷的查询API，健全的Java DSL，优美的Kotlin DSL
    -   动态查询为多表查询设计
    -   DSL支持混入原生SQL表达式以使用非标准的数据库特有能力
	-   拓展SQL的能力，轻松支持原生SQL实现成本高昂的高级功能
    -   高级的SQL优化能力
        -   自动去除无用的表连接
        -   自动合并逻辑等价的表连接
        -   自动合并逻辑等价的隐式子查询
        -   分页查询可自动生成并优化count查询
-   DTO语言，以及相应的编译时代码生成器，让DTO变得极其廉价
    -   Output DTO，用作复杂查询的返回
    -   Input DTO, 用作复杂保存的参数
    -   Specification DTO, 用作复杂查询的参数
    -   ORM自身支持的DTO可以与其无缝集成，不会为业务代码引入额外逻辑
-   查询任意图结构
    -   没有`N + 1`问题
    -   任何层次的对象都可以不完整
    -   可递归查询自关联属性
	-   既可以直接返回实体，也可返回Output DTO
-   保存任意图结构
    -   利用数据库本身的upsert能力merge数据
    -   每一层的多个对象都用批量DML操作
    -   自动翻译违背约束的异常
    -   任何层次的被保存对象都可以不完整    
    -   既可以直接保存实体，也可保存Input DTO
    > 这部分经过了大升级，文档在积极重构中，可以先看配套例子中关于save-command的例子
-   强大的缓存
    -   多级缓存，每一级缓存都可以做自由技术选型
    -   不仅仅是对象缓存 *(关联、计算值、多视图)*
    -   自动维持缓存一致性
-   对GraphQL的快速支持
-   基于文档注释客户端契约 *(OpenAPI、TypeScript)*

## 2. 核心理念

Jimmer的核心理念，在于任意形状的的数据结构作为一个整体进行读写操作，而非简单的处理实体对象。

-   Jimmer实体对象**并非POJO**，表达任意形状的数据结构。

-   任意形状的数据结构，都可以作为一个整体进行

    -   **读**：Jimmer创建这种无限灵活的数据结构，传递给你

    -   **写**：你创建这种无限灵活的数据结构，传递给Jimmer

既然Jimmer的设计理念是为了读写任意形状的数据结构，而非处理简单的对象，那么它具备类似能力的技术有什么差异呢？

<table>
<thead>
<tr>
<th>比较</th>
<th>描述</th>
</tr>
</thead>
<tbody>
<tr>
<td rowspan="2">GraphQL</td>
<td>GraphQL只关注查询任意形状的数据结构；Jimmer不仅如此，还关注如何写入任意形状的数据结构</td>
</tr>
<tr>
<td>GraphQL不支持基于自关联属性的递归查询，Jimmer支持</td>
</tr>
<tr>
<td rowspan="5">JPA</td>
<td>JPA中，为控制被保存数据结构的形状，必须为使属性配置insertable、updatable或cascade<i>(针对关联属性)</i>，
无论如何配置，被保存的数据结构是固定的；Jimmer实体并非POJO，其数据结构的形状千变万化，
无需事先规划和设计，任何业务场景都可以构建它需要的数据结构并直接保存</td>
</tr>
<tr>
<td>对于查询而言，JPA的EntityGraphQL非常复杂；Jimmer提供了两种手段来实现类似的功能：
控制返回实体对象的格式，或者通过极其廉价的方式生成DTO并直接查询，无论哪种方式，都远比EntityGraph简单</td>
</tr>
<tr>
<td>

在JPA中，如果需要为只查询部分属性而使用DTO对象，那么DTO必须是一个没有任何关联简单对象。即，丢失了ORM最宝贵的能力，从`ORM`退化成了`OM`；
Jimmer自动生成的DTO支持任意复杂的层级关系，**Jimmer是目前唯一一个支持基于DTO的嵌套投影的ORM**

</td>
</tr>
<tr>
<td>

在JPA中，更新对象会导致所有可更新的列被修改。为了简便，开发人员很少使用`update`，选择先查询完整的对象，修改部分属性，最后保存整个对象；Jimmer可以构建并直接保存残缺对象

</td>
</tr>
<tr>
<td>JPA的EntityGraphQL不支持基于自关联属性的递归查询，Jimmer支持</td>
</tr>
<tr>
<td>MongoDB</td>
<td>
在MonoDB中，每个文档结构都是一个数据孤岛。虽然MongoDB的数据结构是弱类型的，但从业务层面讲，有哪些数据孤岛以及每个数据孤岛内部的层级结构需要实现设计和约定。
一旦完成设计和约定，整个数据视图的格式就定死了，必须按照固定的视角处理数据；
在Jimmer中，数据结构的形状无需实现设计，任何业务场景都可以随意规划出一个数据结构的格式，并将对应的数据结构作为一个整体进行读写。
</td>
</tr>
</tbody>
</table>

**基于此核心理念，Jimmer将会为你带来以前在任何技术路线想都难以企及的便捷性，
这会让你从繁琐的细节处理中解脱出来，专注于复杂业务的快速实现。**

## 3. 全面的能力
![feature](./feature.svg)

## 4. 极致的性能
![performance](./performance.jpg)

## 5. 注意事项

由于Jimmer是一个编译时框架，考虑到并非所有用户都熟悉Apt和Ksp，有必要提及一个重要细节。

Apt/Ksp是行业内的标准技术，Java IDE会给予支持。

-   大部分情况下，你的修改都会包含Java或Kotlin代码的变化，
    例如，实体类型变化，或Web Controller变化*(Jimmer有自己的OpenAPI和TypeScript生成的实现)*。这时只需点击IDE的Run或Debug按钮运行一次，无需全量编译，就可以触发所有的预编译行为，自动生成的源代码和资源文件都会自动变化

-   少部分情况下，如果仅仅修改DTO文件，即，除了DTO文件外，同一个工程内没有任何Java或Kotlin源码变动，这时，你有三个选择
    -   采用配套的DTO插件
    -   全量编译，maven或gradle命令，或IDE的Rebuild按钮，都可以达到这个目的
    -   删除受影响工程的编译输出目录后，再点击IDE的Run或Debug按钮

## 6. 链接

-   例子：https://github.com/babyfish-ct/jimmer-examples

-   文档: https://babyfish-ct.github.io/jimmer-doc/zh

-   QQ群：622853051

-   主要视频

    最重要的四个视频以供快速了解，建议按顺序观看

    -   https://www.bilibili.com/video/BV1yG411B74x
    -   https://www.bilibili.com/video/BV1rc411x71Q
    -   https://www.bilibili.com/video/BV1a94y1u7XT
    -   https://www.bilibili.com/video/BV1jypxeEEuF
