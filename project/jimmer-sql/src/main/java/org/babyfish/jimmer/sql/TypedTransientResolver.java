package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Map;

/**
 * A specialized variant of {@link TransientResolver}. When conditions are appropriate, it will use
 * {@link #resolve(Collection, Context)} to provide an extra {@link Context} parameter, which supplies
 * the collection of entities currently being calculated, allowing access to other properties during calculation.
 * <p>
 * You need to implement both abstract {@code resolve} methods to ensure that when the {@link Context}
 * generation conditions are not met, it can fall back to the same behavior as {@link TransientResolver}.
 * <p>
 * {@link Context} may not be generated in scenarios where caching is configured.
 * <p>
 * This type is only for Java. If you use Kotlin, see {@code KTypedTransientResolver}.
 * <p>
 * NOTE: This type is experimental and may undergo major changes or be removed in future versions.
 *
 * @param <E>  The entity type
 * @param <ID> The id type of current entity
 * @param <V>  The calculated type, there are three possibilities
 *             <ul>
 *                <li>If the calculated property is NOT association,
 *                {@code V} should be the property type</li>
 *                <li>If the calculated property is associated reference,
 *                {@code V} should be the associated-id type</li>
 *                <li>If the calculated property is associated list,
 *                {@code V} should be the type of list whose elements
 *                are associated ids.</li>
 *             </ul>
 * @author Forte Scarlet
 * @see TransientResolver
 * @since 0.9.121
 */
@ApiStatus.Experimental
public interface TypedTransientResolver<E, ID, V> extends TransientResolver<ID, V> {
    /**
     * When the conditions for generating {@link Context} are met, this method will be used 
     * instead of {@link #resolve(Collection)} to perform complex property calculations.
     *
     * @param ids     A batch of ids of the current objects that are resolving calculated property,
     *                it is not null and not empty
     * @param context The context of current entities being resolved
     * @return A map contains resolved values
     */
    Map<ID, V> resolve(Collection<ID> ids, Context<E> context);

    /**
     * Represents a context providing additional data related to the current entities being processed within a resolver.
     * <p>
     * NOTE: This interface is experimental and subject to potential breaking changes or removal in future versions.
     *
     * @param <E> The type of entities contained within the context
     * @since 0.9.121
     */
    @ApiStatus.Experimental
    interface Context<E> {
        Collection<? extends E> getContent();

        ImmutableProp getProp();
    }
}
