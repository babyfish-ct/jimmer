package org.babyfish.jimmer.sql.example.business.resolver

import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.TransientResolver
import org.babyfish.jimmer.sql.runtime.DefaultTransientResolverProvider
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

//@Component
class MyTransientResolverProvider(
    private val ctx: ApplicationContext
) : DefaultTransientResolverProvider() {

    override fun get(type: Class<TransientResolver<*, *>>, sqlClient: JSqlClient?): TransientResolver<*, *> {
        return ctx.getBean(type);
    }
}