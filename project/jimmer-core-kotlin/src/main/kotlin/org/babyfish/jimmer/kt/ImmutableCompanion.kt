package org.babyfish.jimmer.kt

/**
 * Give user another choice, like this
 *
 * <pre><code>
 * &#64;Immutable // or `@Entity`, `@Embeddable`
 * interface Book {
 *     ...omit properties...
 *
 *     companion object: ImmutableCompanion<Book>
 * }
 *
 * val book = Book {
 *      id = 1L
 *      name = "Learning GraphQL"
 * }
 * </code></pre>
 */
interface ImmutableCompanion<T>