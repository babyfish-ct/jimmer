package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.lang.Ref
import org.babyfish.jimmer.sql.TransientResolver
import org.babyfish.jimmer.sql.loader.AbstractDataLoader
import java.sql.Connection
import java.util.*

interface KTransientResolver<ID: Any, V> : TransientResolver<ID, V> {

    /**
     * @param ids A batch of ids of the current objects that are resolving calculated property,
     *            it is not empty
     * @return A map contains resolved values
     */
    override fun resolve(ids: Collection<ID>): Map<ID, V>

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
            get() = AbstractDataLoader.transientResolverConnection()
    }
}