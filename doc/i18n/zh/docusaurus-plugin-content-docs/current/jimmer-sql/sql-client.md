---
sidebar_position: 3
title: SqlClient
---

SqlClient是jimmer-sql所有API的入口。

## 创建SqlClient

SqlClient是不可变对象，基于Builder模式创建。

```java
JSqlClient sqlClient = JSqlClient
    .newBuilder()
    ...doSomething...
    .builde();
```

SqlClient需要一个实例，应该被全局共享。

- 如果没有使用Spring，应该通过静态变量共享它。

- 如果使用了Spring，应该将其作为一个Spring的单例Bean对象。

毫无疑问，采用Spring/SpringBoot几乎是所有服务端程序的固定模式。

```java
@Configuration
public class SqlClientConfig {

    @Bean
    public JSqlClient sqlClient() {
        return JSqlClient
            .newBuilder()
            ...doSomething...
            .builde();
    }
}
```

然后，可以在任何地方注入SqlClient对象来使用jimmer-sql的所有功能。

## ConnectionManager

jimmer-sql中一切可执行的语句和指令都继承自`Executable`接口

```java title="Executable.java"
package org.babyfish.jimmer.sql.ast;

import java.sql.Connection;

public interface Executable<R> {

    R execute();

    R execute(Connection con);
}
```

- `execute(Connection)`：在用户指定的JDBC连接上执行。

    以查询为例：
    ```java
    public List<Book> findBooks(Connection con) {
        return sqlClient
            .createQuery(BookTable.class, (q, book) -> {
                return q.select(book);
            })
            // highlight-next-line
            .execute(con);
    }
    ```

- `execute()`：由jimmer-sql自主决定在某个JDBC连接上执行。

    以查询为例：
    ```java
    public List<Book> findBooks() {
        return sqlClient
            .createQuery(BookTable.class, (q, book) -> {
                return q.select(book);
            })
            // highlight-next-line
            .execute();
    }
    ```

:::caution
要使用第1种行为，无需对SqlClient做出特别配置。

要使用第2种行为，必须为SqlClient配置ConnectionManager。否则将会导致异常。

毫无疑问，第2种方式更符合业务系统开发要求，更推荐得使用。所以，强烈建议为SqlClient配置ConnectionManager。
:::

### 简单的ConnectionManager

```java
javax.sql.DataSource dataSource = ...;

JSqlClient sqlClient = JSqlClient
    .newBuilder()
    .setConnectionManager(
        ConnectionManager
            // highlight-next-line
            .simpleConnectionManager(dataSource)
    )
    .builde();
```

:::danger
`ConnectionManager.simpleConnectionManager`仅负责从DataSource获取连接，并没有事务管理机制。

这种方式仅适用于没有使用Spring的场合，除学习和尝试外，不建议在实际项目使用simpleConnectionManager。
:::

### 受Spring事务管理的ConnectionManager

正如前文所说，采用Spring/SpringBoot几乎是所有服务端程序的固定模式。

jimmer-sql只专注于生成SQL和执行SQL，在连接管理和事务管理方面，不想开发和Spring/SpringBoot重复的功能，更不想因此增加和Spring/SpringBoot整合的难度。

当你使用了spring-jdbc后，可以基于`org.springframework.jdbc.datasource.DataSourceUtils`实现ConnectionManager，这样jimmer-sql就受到Spring事务的管理了。

```java
@Bean
public JSqlClient sqlClient(
    // Inject dataSoruce of spring-jdbc
    // highlight-next-line
    DataSource dataSource
) {
    return JSqlClient.newBuilder()
        .setConnectionManager(
            new ConnectionManager() {
                @Override
                public <R> R execute(
                    Function<Connection, R> block
                ) {
                    Connection con = DataSourceUtils
                        // highlight-next-line
                        .getConnection(dataSource);
                    try {
                        return block.apply(con);
                    } finally {
                        DataSourceUtils
                            // highlight-next-line
                            .releaseConnection(con, dataSource);
                    }
                }
            }
        )
        .build();
}
```

:::info
让jimmer-sql受到Spring事务的管理，是推荐用法。
:::

## Dialect

和大部分ORM一样，需要为不同的数据库设置不同的方言。到目前为止，支持的方言如下:

- org.babyfish.jimmer.sql.dialect.H2Dialect
- org.babyfish.jimmer.sql.dialect.MySqlDialect
- org.babyfish.jimmer.sql.dialect.PostgresDialect
- org.babyfish.jimmer.sql.dialect.OracleDialect
- org.babyfish.jimmer.sql.dialect.SqlServerDialect

以H2为例，方言设置如下

```java
@Configuration
public class SqlClientConfig {

    @Bean
    public JSqlClient sqlClient() {
        return JSqlClient
            .newBuilder()
            .setConnectionManager(...)
            // highlight-next-line
            .setDialect(new H2Dialect())
            .build();
    }
}
```

## Executor

Executor是jimmer-sql执行最终SQL的入口，作为SQL执行的拦截器。

:::note
设置Executor是可选行为。
:::

```java title="Executor.java"
package org.babyfish.jimmer.sql.runtime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public interface Executor {

    <R> R execute(
            Connection con,
            String sql,
            List<Object> variables,
            SqlFunction<PreparedStatement, R> block
    );
}
```

此接口只有一个`execute`方法

1. `con`: SQL基于此连接执行

2. `sql`: 即将被执行的SQL语句

3. `variables`: 即将被执行的SQL所附带的参数

    > 注意：
    >
    > 该列表不可能包含null，所有的null值都被事先替换成了`org.babyfish.jimmer.sql.runtime.DbNull`类型的对象。

4. `block`: 将要执行的实质性操作。

    > 注意：
    >
    > 不要在自定义Executor中直接调用`block`，应该调用`org.babyfish.jimmer.sql.runtime.DefaultExecutor`的`execute`方法。

默认的DefaultExecutor已经可胜任所有工作，所以指定自定义Exector是可选的。

如果遇到以下场景之一，就可以指定自定义Executor，覆盖默认的DefaultExecutor。

### 在日志中打印SQL语句和其参数

```java
@Configuration
public class SqlClientConfig {

    private static final Logger LOGGER = 
        LoggerFactory.getLogger(SqlClientConfig.class);

    @Bean
    public JSqlClient sqlClient() {
        return JSqlClient
            .newBuilder()
            .setConnectionManager(...)
            .setDialect(new H2Dialect())
            // highlight-next-line
            .setExecutor(
                new Executor() {
                    @Override
                    public <R> R execute(
                            Connection con,
                            String sql,
                            List<Object> variables,
                            SqlFunction<PreparedStatement, R> block
                    ) {
                        // Log SQL and variables.
                        LOGGER.info(
                            "Execute sql : \"{}\", " +
                            "with variables: {}", 
                            sql, 
                            variables
                        );
                        // Call DefaultExecutor
                        // highlight-next-line
                        return DefaultExecutor
                            .INSTANCE
                            .execute(
                                con,
                                sql,
                                variables,
                                block
                            );
                    }
                }
            )
            .build();
    }
}
```

### 搜集执行缓慢的SQL以求改进

```java
@Configuration
public class SqlClientConfig {

    private static final Logger LOGGER = 
        LoggerFactory.getLogger(SqlClientConfig.class);

    @Bean
    public JSqlClient sqlClient() {
        return JSqlClient
            .newBuilder()
            .setConnectionManager(...)
            .setDialect(new H2Dialect())
            // highlight-next-line
            .setExecutor(
                new Executor() {
                    @Override
                    public <R> R execute(
                            Connection con,
                            String sql,
                            List<Object> variables,
                            SqlFunction<PreparedStatement, R> block
                    ) {
                        long millis = System.currentTimeMillis();
                        // Call DefaultExecutor
                        // highlight-next-line
                        R result = DefaultExecutor
                            .INSTANCE
                            .execute(
                                con,
                                sql,
                                variables,
                                block
                            );
                        millis = System.currentTimeMillis() - millis;
                        if (millis > 1000) { // Slow SQL
                            ...sendMessageToTechTeam...
                        }
                        return result;
                    }
                }
            )
            .build();
    }
}
```

## ScalarProvider。

可以为SqlClient添加多个`ScalarProvider`，每个`ScalarProvider`告诉据库如何处理一种自定义数据类型。

`ScalarProvider`定义如下

```java title="ScalarProvider"
package org.babyfish.jimmer.sql.runtime;

import java.util.function.Consumer;

public abstract class ScalarProvider<T, S> {

    protected ScalarProvider(Class<T> scalarType, Class<S> sqlType) {
        this.scalarType = scalarType;
        this.sqlType = sqlType;
    }

    public abstract T toScalar(S sqlValue);

    public abstract S toSql(T scalarValue);
 }
```

- 范型参数`T`: Java中数据类型
- 范型参数`S`: 数据库中数据类型
- 方法`toScalar`: 把数据库中读取到的非null数据转换为Java数据
- 方法`toSql`: 把Java的非null数据转换为数据库可接受的数据

### 自定义ScalarProvider

例如某数据库不支持UUID类型，可以如此处理

```java
@Configuration
public class SqlClientConfig {

    private static final Logger LOGGER = 
        LoggerFactory.getLogger(SqlClientConfig.class);

    @Bean
    public JSqlClient sqlClient() {
        return JSqlClient
            .newBuilder()
            .setConnectionManager(...)
            .setDialect(new H2Dialect())
            .setExecutor(...)
            // highlight-next-line
            .addScalarProvider(
                new ScalarProvider<>(
                    UUID.class,
                    String.class
                ) {
                    @Override
                    public UUID toScalar(String sqlValue) {
                        return UUID.fromString(sqlValue);
                    }

                    @Override
                    public String toSql(UUID scalarValue) {
                        return scalarValue.toString();
                    }
                }
            )
            .build();
    }
}
```
:::note
本框架附带例子中没有演示这样的用法，因为附带的例子基于H2，而H2是支持UUID类型的。
:::

### 内置的ScalarProvider

jimmer-sql内置了枚举所需的ScalarProvider。

假设有一个枚举类型：

```java title="GcObjColor.java"
public enum GcObjColor { 
    WHITE, 
    GRAY, 
    BLACK 
}
```

1. 按枚举的`name()`映射为字符串

    ```java
    return SqlBuilder
        .newBuilder()
        // highlight-next-line
        .addScalarProvoder(
            ScalarProvoder.enumProviderByString(
                GcObjColor.class
            )
        )
        ...doOtherthings...
        .build();
    ```

1. 按枚举的`name()`映射为字符串，但把`GRAY`映射为`GREY`

    ```java
    return SqlBuilder
        .newBuilder()
        .addScalarProvoder(
            ScalarProvoder.enumProviderByString(
                GcObjColor.class, 
                // highlight-next-line
                it -> it.map(GcObjColor.GRAY, "GREY")
            )
        )
        ...doOtherthings...
        .build();
    ```

3. 按枚举的`ordinal()`映射为整数

    ```java
    return SqlBuilder
        .newBuilder()
        // highlight-next-line
        .addScalarProvoder(
            ScalarProvoder.enumProviderByInt(
                GcObjColor.class
            )
        )
        ...doOtherthings...
        .build();
    ```

4. 按枚举的`ordinal()`映射为整数，但把`BLACK`映射为`3`*（默认是2）*

    ```java
    return SqlBuilder
        .newBuilder()
        // highlight-next-line
        .addScalarProvoder(
            ScalarProvoder.enumProviderByInt(
                GcObjColor.class,
                it -> it.map(GcObjColor.BLACK, 3)
            )
        )
        ...doOtherthings...
        .build();
    ```

## IdGenerator

可以动态地设置对象的id生成策略。

:::info

正常情况下，id生成策略应该在实体接口中静态地使用注解`@javax.persistance.GeneratedValue`配置，可以参考[映射](./mapping#javaxpersistancegeneratedvalue)以了解更多。

既然如此，为什么还要提供在SqlClient中动态指定id生成器的功能呢？

SqlClient中动态指定IdGenerator可以覆盖实体文件中注解`@javax.persistance.GeneratedValue`的静态配置，这对单元测试非常有用。
:::

```java title="UnitTestIdGenerator.java"
public class UnitTestIdGenerator implements UserIdGenerator {

    private final Iterator<Object> preAllocatedIdItr;
    
    public UnitTestIdGenerator(Object ... preAllocatedIds) {
        preAllocatedIdItr = Arrays
                .asList(preAllocatedIds)
                .iterator();
    }

    @Override
    public Object generate(Class<?> entityType) {
        return preAllocatedIdItr.next();
    }
}
```

```java title="MyTest.java"
class MyTest {

    private JSqlClient sqlClient;

    @BeforeEach
    public void init() {
        sqlClient = JSqlClient
            .newBuilder()
            // highlight-next-line
            .setIdGenerator(
                Department.class,
                new UnitTestIdGenerator(51L, 52L, 53L)
            )
            // highlight-next-line
            .setIdGenerator(
                Employee.class,
                new UnitTestIdGenerator(100L, 101L)
            )
            // highlight-next-line
            .setIdGenerator(
                // For other entity types
                new UnitTestIdGenerator(1L, 2L, 3L, 4L)
            )
            .build();
    }

    @Test
    public void test() {
        // TODO: Add unit test code here
    }
}
```

## DefaultBatchSize和DefaultListBatchSize

SqlClient支持两个配置：`DefaultBatchSize`和`DefaultListBatchSize`。

```java
@Configuration
public class SqlClientConfig {

    @Bean
    public JSqlClient sqlClient() {
        return JSqlClient
            .newBuilder()
            .setConnectionManager(...)
            .setDialect(new H2Dialect())
            .setExecutor(...)
            .addScalarProvider(...)
            // highlight-next-line
            .setDefaultBatchSize(256)
            // highlight-next-line
            .setDefaultListBatchSize(32)
            .build();
    }
}
```

具体的作用在[对象抓取器](./query/fetcher#batchsize)中做了详细的描述，本文不重复阐述。