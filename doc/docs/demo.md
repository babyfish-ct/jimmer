---
sidebar_position: 5
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

These two projects are Spring Shell programs, each of them supports three commands

### books

Find all books

The command has multiple optional parameters, as follows

|Parameter|Description|Default value|
|---|----|-----|
|--fetch|The first column of the SQL query is an object of type `Book`. If this parameter is specified, it means `Book.store`, `Book.store.avgPrice` and `Book.authors` are also required.||
|--name <string_value>|The filter of `Book.name`||
|--store-name <string_value>|The fitler of `Book.store.name`||
|--author-name <string_value>|The filter of `Book.authors.firstName` or `Book.authors.lastName`||
|--page-size <int_value>|Number of records per page|2|

:::warning
There is no value after the `--fetch`, do not enter true or false after it
:::

### trees

Query all root (`parent` is null) nodes and recursively fetch all child nodes, no matter how deep

The command has multiple optional parameters, as follows

|Parameter|Description|Default value|
|---|----|-----|
|--root-name <string_value>|The filter of `TreeNode.name`||
|--no-recursive <string_value>|Node names that do not require recursion, if there are multiple, separate them with commas||

### save-tree

Save (create or replace) a tree with any depth and breadth

The command parameters are as follows

|Parameter|Description|Mandatory|Default|
|---|----|---|-----|
|--root-name <string_value>|The name of the root node of the tree to save|Yes||
|--depth <int_value>|The depth of the tree to save, excluding the root node itself|No|2|
|--breadth <int_value>|The number of children of each node except leaf nodes|No|2|

For example
```
save-tree --root-name MyNode --depth 2 --breadth 2
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

### delete-tree

Delete a tree node, including all its subordinate nodes

The command parameters are as follows

|Parameter|Description|
|---|----|
|--id <int_value>|node id|

## [jimmer-sql-graphql](https://github.com/babyfish-ct/jimmer/tree/main/example/java/jimmer-sql) and [jimmer-sql-graphql-kt](https://github.com/babyfish-ct/jimmer/tree/main/example/kotlin/jimmer-sql-kt)

These two projects are GraphQL services. After startup, you can use your browser to visit http://localhost:8080/graphiql.

This page provides the online documentation of the GraphQL API. according to its instructions, you can enter the query or mutation request and execute it.

