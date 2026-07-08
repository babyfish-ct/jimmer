package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.lang.Ref
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.TransientResolver
import org.babyfish.jimmer.sql.loader.AbstractDataLoader
import org.babyfish.jimmer.sql.loader.TransientResolverContext
import java.sql.Connection
import java.util.*

/**
 * @param [ID] The id type of current entity
 * @param [V] The calculated type, there are three possibilities
 * - If the calculated property is NOT association,
 * `V` should be the property type
 * - If the calculated property is associated reference,
 * `V`should be the associated-id type
 * - If the calculated property is associated list,
 * `V` should be the type of list whose elements
 * are associated ids
 */
interface KTransientResolver<ID: Any, V> : TransientResolver<ID, V> {

    /**
     * @param ids A batch of ids of the current objects that are resolving calculated property,
     *            it is not empty
     * @return A map contains resolved values
     */
    override fun resolve(ids: Collection<ID>): Map<ID, V>

    /**
     * @param ids A batch of ids of the current objects that are resolving calculated property,
     *            it is not empty
     * @param ctx The current transient resolver context, including connection and property metadata.
     * @return A map contains resolved values
     */
    @ExperimentalTransientResolverContext
    fun resolve(ids: Collection<ID>, ctx: KTransientResolverContext): Map<ID, V> =
        resolve(ids)

    @OptIn(ExperimentalTransientResolverContext::class)
    override fun resolve(ids: Collection<ID>, ctx: TransientResolverContext): Map<ID, V> =
        resolve(ids, KTransientResolverContext(ctx))

    /**
     * Please ignore this method if the current calculated property
     * returns nullable type or LIST of entity objects
     *
     * @return The default value, null will be ignored by jimmer.
     */
    override fun getDefaultValue(): V? = null

    /**
     * Please ignore this method if cache for current calculated property is not enabled.
     *
     * @return The reference wrapper of parameterMap
     * <ul>
     *      <li>If the `Ref` wrapper itself is null, it means there is some filter but not cacheable filter</li>
     *      <li>If the `Ref` wrapper itself is not null,
     *      it means there is no filter(wrapped value is null) or
     *      there is a cacheable filter(wrapped value is not null)</li>
     * </ul>
     */
    override fun getParameterMapRef(): Ref<SortedMap<String, Any>?>? =
        Ref.empty()

    companion object {

        /**
         * The database connection should be used,
         *  it can be ignored if the current resolver is injected by spring
         *  and the spring-transaction is enabled.
         * @return
         */
        @JvmStatic
        val currentConnection: Connection
            get() = TransientResolver.currentConnection()
    }
}

/**
 * Kotlin wrapper of [TransientResolverContext].
 */
@ExperimentalTransientResolverContext
class KTransientResolverContext internal constructor(
    private val javaContext: TransientResolverContext,
) {

    /**
     * The database connection that should be used by the current resolver.
     */
    val connection: Connection
        get() = javaContext.connection

    /**
     * The transient property currently being resolved.
     */
    val prop: ImmutableProp
        get() = javaContext.prop

    /**
     * The resolver currently being invoked.
     */
    val resolver: TransientResolver<*, *>
        get() = javaContext.resolver

    /**
     * Source ids currently being resolved.
     */
    val sourceIds: Set<Any>
        get() = javaContext.sourceIds
}
