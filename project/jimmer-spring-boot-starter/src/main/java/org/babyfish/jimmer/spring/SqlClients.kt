package org.babyfish.jimmer.spring

import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.cfg.KSqlClientDsl
import org.babyfish.jimmer.sql.kt.toKSqlClient
import org.springframework.context.ApplicationContext
import java.util.function.Consumer
import javax.sql.DataSource

object SqlClients {

    @JvmStatic
    fun java(
        ctx: ApplicationContext
    ): JSqlClient =
        java(ctx, null, null)

    @JvmStatic
    fun java(
        ctx: ApplicationContext,
        dataSource: DataSource?
    ): JSqlClient =
        java(ctx, dataSource, null)

    @JvmStatic
    fun java(
        ctx: ApplicationContext,
        block: Consumer<JSqlClient.Builder>?
    ): JSqlClient =
        java(ctx, null, block)

    @JvmStatic
    fun java(
        ctx: ApplicationContext,
        dataSource: DataSource?,
        block: Consumer<JSqlClient.Builder>?
    ): JSqlClient =
        JSpringSqlClient(ctx, dataSource, block, false)

    @JvmStatic
    fun kotlin(ctx: ApplicationContext): KSqlClient =
        kotlin(ctx, null, null)

    @JvmStatic
    fun kotlin(
        ctx: ApplicationContext,
        dataSource: DataSource?
    ): KSqlClient =
        kotlin(ctx, dataSource, null)

    @JvmStatic
    fun kotlin(
        ctx: ApplicationContext,
        block: (KSqlClientDsl.() -> Unit)?
    ): KSqlClient =
        kotlin(ctx, null, block)

    @JvmStatic
    fun kotlin(
        ctx: ApplicationContext,
        dataSource: DataSource?,
        block: (KSqlClientDsl.() -> Unit)?
    ): KSqlClient =
        JSpringSqlClient(
            ctx,
            dataSource,
            block?.let {
                Consumer {
                    KSqlClientDsl(it).block()
                }
            },
            true
        ).toKSqlClient()
}