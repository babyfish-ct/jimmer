package org.babyfish.jimmer.sql.kt.model.calc

import org.babyfish.jimmer.sql.kt.ExperimentalTransientResolverContext
import org.babyfish.jimmer.sql.kt.KTransientResolver
import org.babyfish.jimmer.sql.kt.KTransientResolverContext

class BookStoreResolverPropNameResolver : KTransientResolver<Long, String> {

    override fun resolve(ids: Collection<Long>): Map<Long, String> =
        error("This test resolver requires KTransientResolverContext")

    @OptIn(ExperimentalTransientResolverContext::class)
    override fun resolve(ids: Collection<Long>, ctx: KTransientResolverContext): Map<Long, String> =
        ids.associateWith { ctx.prop.name }
}
