---
sidebar_position: 8
title: 附带demo
---

本框架附带3个例子，每个例子针对Java和Kotlin提供两个工程，共计6个工程。

|Java|Kotlin|描述|
|----|------|---|
|[example/java/jimmer-core](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-core)|[example/kotlin/jimmer-core-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-core-kt)|展示jimmer-core相关的功能|
|[example/java/jimmer-sql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql)|[example/kotlin/jimmer-sql-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-sql-kt)|展示jimmer-sql相关的功能|
|[example/java/jimmer-sql-graphql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql-graphql)|[example/kotlin/jimmer-core-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-sql-graphql-kt)|展示对SpringGraphQL的支持|

这6个工程，有两个是简单程序，另外4个是spring-boot应用。

所以，你可以直接用intellij打开，等待gradle依赖下载完毕后，直接运行`main`函数即可。

:::caution
Jimmer使用了预编译器，具备编译时生成源代码的能力，所以某些项目初次打开后会有编译错误。不要管这些错误，直接运行就可以消除这些错误。
:::

## 数据库相关demo运行模式

[jimmer-sql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql)，[jimmer-sql-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-sql-kt)，[jimmer-sql-graphql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql-graphql)和[jimmer-sql-graphql-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql-graphql-kt)都涉及了数据库操作，具备缓存和非缓存两种运行模式。

### 非缓存模式

这4个工程是spring boot项目，默认profile所对应的的`application.yml`文件可以以无缓存模式运行例子。

:::note
无缓存模式使用内嵌的H2内存数据库，每次运行都会自动初始化数据库。

无需任何环境准备就可以直接运行，对数据库的修改会随着程序的退出而丢失。
:::

### 有缓存模式

这4个工程是spring boot项目，`application-cache.yml`文件可以以有缓存模式运行例子。

:::note
有缓存模式使用docker容器内部的MySQL数据库，配套的安装脚本会自动初始化数据库。

对数据库的修改会永久保留，要重置数据库，必须重新安装MySQL数据库。
:::

环境安装

1. 确保本机安装了docker
2. 打开命令行，进入&lt;project-cloned-home&gt;/example/env-with-cache
3. 执行`install.sh`

采用以下任何一种方式，以缓存模式运行demo
- 选择一：在Program arguemnts中输入`--spring.profiles.active=cache`
- 选择二：在VM options中输入`-Dspring.profiles.active=cache`

## [jimmer-core](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-core)和[jimmer-core-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-core-kt)

这两个项目是最简单的例子，也不涉及数据库操作，直接运行，观察输出即可

## [jimmer-sql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql)和[jimmer-sql-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-sql-kt)

这两个项目是Spring REST程序，启动程序后，用浏览器访问http://localhost:8080/ui或http://localhost:8080/swagger-ui/index.html，即可进入在线文档直接体验。

这个例子支持多租户，可以为当前操作者设置全局的租户名
- 对查询操作而言，租户设置是可选的
- 对修改操作而言，租户设置是必须的
- 租户可以设置为`a`或`b`

要为当前操作者设置全局的租户名，你可以点击在线文档的`Authorize`按钮

![swagger-authorize](@site/static/img/swagger-authorize.jpg)

支持如下操作

### GET /books

查询书籍

该命令具有多个可选参数，如下

|参数|描述|默认值|
|---|----|-----|
|fetch|查询返回`Book`类型的对象。如果此参数为true，表示附带查询`Book.store`、`Book.store.avgPrice`和`Book.authors`。|false|
|name|`Book.name`的过滤条件||
|storeName|`Book.store.name`的过滤条件||
|authorName|`Book.authors.firstName`或`Book.authors.lastName`的过滤条件||
|pageIndex|分页索引|0|
|pageSize|分页大小|5|

### PUT /book

保存书籍及其关联对象

该命令要求RequstBody是一个`Book`类型的动态实体对象。格式任意，但是
- 对当前`Book`对象而言，要么指定`id`，要么指定`name`和`edition`
- 如果指定了当前对象的多对一关联属性`Book.store`，对关联对象而言，对象要么指定`id`，要么指定`name`
- 如果指定了当前对象的多对多关联属性`Book.authors`，对每个关联对象而言，对象要么指定`id`，要么指定`firstName`和`lastName`
- 如果是最终操作是insert而非update，必须当前Book对象的`price`属性
- 全局的租户必须设置

### GET /stores

查询所有书店

该命令具有可选参数，如下

|参数|描述|默认值|
|---|----|-----|
|fetch|查询返回`BookStore`类型的对象。如果此参数为true，表示附带查询`BookStore.avgPrice`、`BookStore.books`和`BookStore.books.authors`。|false|

### GET /authors

查询作者

该命令具有可选参数，如下

|参数|描述|默认值|
|---|----|-----|
|fetch|查询返回`Author`类型的对象。如果此参数为true，表示附带查询`Author.books`、`Author.books.store`和`Author.books.store.avgPrice`。|false|
|firstName|模糊查询条件|""|
|lastName|模糊查询条件|""|
|gender|作者性别|null|

### GET /trees

查询所有的根（`parent`为null）节点，并递归抓取所有子节点，无论多深

该命令具有多个可选参数，如下

|参数|描述|默认值|
|---|----|-----|
|rootName|`TreeNode.name`的过滤条件||
|norecursiveNames|不需要递归的节点名称，如果有多个，用逗号分隔||

### PUT /tree

保存（新建或替换）一颗树，深度和广度不限

该命令参数如下

|参数|描述|必需|默认值|
|---|----|---|-----|
|rootName|要保存的树的根节点的名称|是||
|depth|要保存的树的深度，不包含根节点|否|2|
|breadth|除叶子节点外，每个节点的子节点个数|否|2|

例如
```
PUT /tree rootName=MyNode&depth=2&breadth=2
```
表示，保存（新建或替换）这样一颗树
```json
{
    "name" : "MyNode",
    "parent" : null,
    "childNodes" : [
        {
            "name" : "MyNode-1",
            "childNodes" : [
                {
                    "name" : "MyNode-1-1",
                    "childNodes" : []
                }, 
                {
                    "name" : "MyNode-1-2",
                    "childNodes" : []
                }
            ]
        }, 
        {
            "name" : "MyNode-2",
            "childNodes" : [
                {
                    "name" : "MyNode-2-1",
                    "childNodes" : []
                }, 
                {
                    "name" : "MyNode-2-2",
                    "childNodes" : []
                }
            ]
        }
    ]
}
```

### DELETE /tree/{id}

根据根节点id删除某个树，包含其所有下级节点

## [jimmer-sql-graphql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql)和[jimmer-sql-graphql-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-sql-kt)

这两个项目是GraphQL服务。启动后，可以使用浏览器访问http://localhost:8080/graphiql。

此页面提供了GraphQL API的线上文档，按其指导输入query或mutation请求并执行即可

这个例子支持多租户，可以为当前操作者设置全局的租户名
- 对查询操作而言，租户设置是可选的
- 对修改操作而言，租户设置是必须的
- 租户可以设置为`a`或`b`

要为当前操作者设置全局的租户名，你可以点击在线文档的`Authorize`按钮

![graphiql-headers](@site/static/img/graphiql-headers.jpg)
