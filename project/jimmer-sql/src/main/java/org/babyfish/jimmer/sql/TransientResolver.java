package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.sql.loader.AbstractDataLoader;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

/**
 * Only for java, kotlin developers should use `KTransientResolver`
 *
 * @param <ID>
 * @param <V>
 */
public interface TransientResolver<ID, V> {

    /**
     *
     * @param ids A batch of ids of the current objects that are resolving calculated property,
     *            it is not null and not empty
     * @return A map contains resolved values
     */
    Map<ID, V> resolve(Collection<ID> ids);

    /**
     * Please ignore this method if the current calculated property
     * returns nullable type or LIST of entity objects
     * @return Then default value, null will be ignored by jimmer.
     */
    default V getDefaultValue() {
        return null;
    }

    /**
     * Please ignore this method if cache for current calculated property is not enabled.
     * @return The reference wrapper of parameterMap
     * <ul>
     *      <li>If the `Ref` wrapper itself is null, it means there is some filter but not cacheable filter</li>
     *      <li>If the `Ref` wrapper itself is not null,
     *      it means there is no filter(wrapped value is null) or
     *      there is a cacheable filter(wrapped value is not null)</li>
     * </ul>
     */
    default Ref<SortedMap<String, Object>> getParameterMapRef() {
        return Ref.empty();
    }

    /**
     * Get the database connection should be used,
     * it can be ignored if the current resolver is injected by spring
     * and the spring-transaction is enabled.
     * @return the database connection should be used, never return null.
     * @exception IllegalStateException cannot retrieve the current connection.
     */
    @NotNull
    static Connection currentConnection() {
        return AbstractDataLoader.transientResolverConnection();
    }
}