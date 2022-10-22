package org.babyfish.jimmer.lang

/**
 * Similar to `java.util.Optional`, but different.
 *
 * The value inside the `Optional` is allowed to be null,
 * but the `Optional` itself cannot be null in principle.
 * Because of this, `Optional` itself being Null will cause Intellij to give a warning.
 *
 * Ref is slightly different,
 * not only its internal value is allowed to be null,
 * the Ref itself can also be null, to represent unknown data.
 */
class Ref<T> private constructor(
    val value: T?
) {
    companion object {

        private val EMPTY = Ref<Any>(null)

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T> of(value: T?): Ref<T> =
            if (value === null) {
                EMPTY as Ref<T>
            } else {
                Ref(value)
            }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T> empty(): Ref<T> =
            EMPTY as Ref<T>
    }
}