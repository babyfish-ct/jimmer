package org.babyfish.jimmer.runtime;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.PropId;
import org.jspecify.annotations.Nullable;

/**
 * Internal contract between generated draft implementations and the Jimmer runtime.
 * Application code should use generated draft APIs or {@link org.babyfish.jimmer.DraftObjects} instead.
 *
 * <p>Property IDs can identify a property by index or name. Loaded state determines whether a value
 * is available; visibility independently controls whether the property is exposed by serialization.
 * Drafts are mutable during construction and must not be modified after successful resolution.
 * The list-draft accessors manage runtime caches rather than property values.
 */
public interface DraftSpi extends Draft, ImmutableSpi {

    /**
     * Unloads a property according to its generated loaded-state rules.
     * For a stored property, this removes its loaded state and clears any cached list draft.
     * Unloading a view can unload its base property; computed properties can have no independently
     * controllable loaded state. Unloading is distinct from assigning a loaded {@code null} value.
     *
     * @param prop the property ID, identified by index or name
     * @throws IllegalArgumentException if the property is not recognized by the generated unload implementation
     * @throws IllegalStateException if this draft has already been resolved
     */
    void __unload(PropId prop);

    /**
     * Unloads a property by name, with the same loaded-state and cache semantics as {@link #__unload(PropId)}.
     *
     * @param prop the property name
     * @throws IllegalArgumentException if the property is not recognized by the generated unload implementation
     * @throws IllegalStateException if this draft has already been resolved
     */
    void __unload(String prop);

    /**
     * Assigns a value using the property's generated write rules, including type checks and validation.
     * Writing a stored property marks it loaded; writing an ID view updates its backing association.
     * Assignments to computed properties without generated write support can be ignored.
     * Use {@link #__unload(PropId)} to remove loaded state instead of assigning {@code null}.
     *
     * @param prop the property ID, identified by index or name
     * @param value the new value, or {@code null} when permitted by the property's write rules
     * @throws IllegalArgumentException if the property is unknown or the value violates its write rules
     * @throws ClassCastException if the value has an incompatible runtime type
     */
    void __set(PropId prop, Object value);

    /**
     * Assigns a property by name, with the same write rules as {@link #__set(PropId, Object)}.
     *
     * @param prop the property name
     * @param value the new value, or {@code null} when permitted by the property's write rules
     * @throws IllegalArgumentException if the property is unknown or the value violates its write rules
     * @throws ClassCastException if the value has an incompatible runtime type
     */
    void __set(String prop, Object value);

    /**
     * Changes a property's visibility without changing its value or loaded state.
     * Hiding a property does not unload it, and showing an unloaded property does not load it.
     *
     * @param prop the property ID, identified by index or name
     * @param show whether the property should be visible to serialization
     * @throws IllegalArgumentException if property validation rejects the ID
     * @throws IllegalStateException if this draft has already been resolved
     */
    void __show(PropId prop, boolean show);

    /**
     * Changes a property's visibility by name, with the same semantics as {@link #__show(PropId, boolean)}.
     *
     * @param prop the property name
     * @param show whether the property should be visible to serialization
     * @throws IllegalArgumentException if property validation rejects the name
     * @throws IllegalStateException if this draft has already been resolved
     */
    void __show(String prop, boolean show);

    /**
     * Returns the context that owns this draft and coordinates related object and list drafts.
     * A generated context-free builder has no owning context.
     *
     * @return the owning context, or {@code null} for a context-free builder
     */
    @Nullable
    DraftContext __draftContext();

    /**
     * Returns the cached draft for a stored list property without creating one or loading the property.
     * Index-based and named IDs address the same cache. The cache is specific to this owner and property,
     * even when multiple properties share the same base list.
     *
     * @param prop the property ID, identified by index or name
     * @return the cached list draft, or {@code null} if no draft is cached or the property has no stored list value
     *         (including an unknown property)
     */
    @Nullable
    ListDraft<?> __getListDraft(PropId prop);

    /**
     * Updates the runtime cache for a stored list property without assigning its value or changing its loaded state.
     * Index-based and named IDs address the same cache. A cached draft must correspond to this property's
     * current base list; replacement or unloading of that value invalidates the cache.
     *
     * @param prop the property ID, identified by index or name
     * @param draft the property-local list draft to cache, or {@code null} to clear the cache
     * @throws IllegalArgumentException if the property is unknown or has no stored list value
     */
    void __setListDraft(PropId prop, @Nullable ListDraft<?> draft);

    /**
     * Resolves this context-backed draft to its immutable value, resolving loaded nested drafts and applying validation.
     * Properties that were not loaded remain unloaded. An unchanged draft can reuse its original immutable value.
     * A successful result is cached, so subsequent calls return the same instance.
     *
     * <p>Runtime callers should normally use {@link DraftContext#resolveObject(Object)}, which also checks
     * whether the draft can be resolved in the calling context. After successful resolution,
     * the draft must no longer be edited.
     *
     * @return the immutable value represented by this draft
     * @throws org.babyfish.jimmer.CircularReferenceException if resolution encounters a circular draft dependency
     * @throws IllegalArgumentException if a nested draft belongs to an incompatible context
     */
    Object __resolve();

    /**
     * Reports whether this draft has a cached result from successful resolution.
     * This method does not resolve the draft and returns {@code false} while its first resolution is in progress.
     *
     * @return {@code true} if an immutable result has been cached, otherwise {@code false}
     */
    boolean __isResolved();
}
