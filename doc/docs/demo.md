---
sidebar_position: 8
title: Demos
---

This framework comes with 3 examples, each example provides two projects for Java and Kotlin, a total of 6 projects.

|Java|Kotlin|Description|
|----|------|---|
|[example/java/jimmer-core](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-core)|[example/kotlin/jimmer-core-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-core-kt)|Show jimmer-core related functions|
|[example/java/jimmer-sql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql)|[example/kotlin/jimmer-sql-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-sql-kt)|Show jimmer-sql related functions|
|[example/java/jimmer-sql-graphql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql-graphql)|[example/kotlin/jimmer-core-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-sql-graphql-kt)|Demonstrate support for SpringGraphQL|

Of these six projects, two are simple programs, and the other four are spring-boot applications.

Therefore, you can open it with intellij, wait for the gradle dependencies to be downloaded, and run the `main` function directly.

## Mode for database related demos

[jimmer-sql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql), [jimmer-sql-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-sql-kt), [jimmer-sql-graphql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql-graphql) and [jimmer-sql-graphql-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql-graphql-kt)both involve database operations and have two modes: cached and non-cached.

### Non-cached mode

These four projects are spring boot projects, and the `application.yml` file corresponding to the default profile can run the example in uncached mode.

:::note
Non-cached mode uses embedded H2 in-memory database, which is automatically initialized on every run.

It can be run directly without any environment preparation, and the modifications to the database will be lost when the program exits.
:::

### Cached mode

These four projects are spring boot projects, and the `application-cache.yml` file can run the example in cached mode.

:::note
The cached mode uses the MySQL database in docker, the database is initialized by the install script.

Modifications to the database are permanent. To reset the database, the MySQL database must be reinstalled.
:::

Environment installation

1. Make sure docker is installed on local machine
2. Open the command line and go to &lt;project-cloned-home&gt;/example/env-with-cache
3. Execute `install.sh`

Use any of the following methods to run the demo in cached mode
- Option 1: Add `--spring.profiles.active=cache` into program arguemnts
- Option 2: Add `-Dspring.profiles.active=cache` into VM options

## [jimmer-core](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-core) and [jimmer-core-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-core-kt)

These two projects are the simplest examples and do not involve database operations. Just run them directly and observe the output.

## [jimmer-sql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql) and [jimmer-sql-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-sql-kt)

These two projects are Spring Shell programs, each of them supports 4 urls

### GET /books

Find all books

The command has multiple optional parameters, as follows

|Parameter|Description|Default value|
|---|----|-----|
|fetch|The return type of query is `Book`. If this parameter is specified, it means `Book.store`, `Book.store.avgPrice` and `Book.authors` are also required.||
|name|The filter of `Book.name`||
|storeName|The fitler of `Book.store.name`||
|authorName|The filter of `Book.authors.firstName` or `Book.authors.lastName`||
|pageIndex|Page index|0|
|pageSize|Number of records per page|5|

### GET /trees

Query all root (`parent` is null) nodes and recursively fetch all child nodes, no matter how deep

The command has multiple optional parameters, as follows

|Parameter|Description|Default value|
|---|----|-----|
|rootName|The filter of `TreeNode.name`||
|noRecursiveNames|Node names that do not require recursion, if there are multiple, separate them with commas||

### PUT /tree

Save (create or replace) a tree with any depth and breadth

The command parameters are as follows

|Parameter|Description|Mandatory|Default|
|---|----|---|-----|
|rootName|The name of the root node of the tree to save|Yes||
|depth|The depth of the tree to save, excluding the root node itself|No|2|
|breadth|The number of children of each node except leaf nodes|No|2|

For example
```
PUT /tree rootName=`MyNode`&depth=`2`&breadth=`3`
```
means to save (create or replace) such a tree
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

Delete tree node by root node id, including all its subordinate nodes

## [jimmer-sql-graphql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql) and [jimmer-sql-graphql-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-sql-kt)

These two projects are GraphQL services. After startup, you can use your browser to visit http://localhost:8080/graphiql.

This page provides the online documentation of the GraphQL API. according to its instructions, you can enter the query or mutation request and execute it.

