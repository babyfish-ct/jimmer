package org.babyfish.jimmer.spring;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClientKt;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

/**
 * Designed for single data source.
 *
 * <p>However, it is unnecessary to use this class directly
 * because the sqlClient for single data source is created
 * by the jimmer spring boot starter automatically</p>
 */
public class SqlClients {

    public static JSqlClient java(ApplicationContext ctx) {
        return java(ctx, null);
    }

    public static JSqlClient java(ApplicationContext ctx, @Nullable DataSource dataSource) {
        return new JSpringSqlClient(ctx, dataSource, false);
    }

    public static KSqlClient kotlin(ApplicationContext ctx) {
        return kotlin(ctx, null);
    }

    public static KSqlClient kotlin(ApplicationContext ctx, @Nullable DataSource dataSource) {
        return KSqlClientKt.toKSqlClient(
                new JSpringSqlClient(ctx, dataSource, true)
        );
    }
}
