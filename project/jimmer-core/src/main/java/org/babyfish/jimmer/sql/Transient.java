package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;
import org.babyfish.jimmer.Formula;

import java.lang.annotation.*;

/**
 * <p>Example-1: Transient property</p>
 *
 * <pre>{@code
 * public class BookStore {
 *
 *     ...omit other properties...
 *
 *     @Transient
 *     String userData();
 *
 * }}</pre>
 *
 * <p>Example-2: Complex calculation property whose return type is scalar type</p>
 *
 * <pre>{@code
 * public class BookStore {
 *
 *     ...omit other properties...
 *
 *     @Id
 *     long id();
 *
 *     @Transient(BookStoreAvgPriceResolver.class)
 *     BigDecimal avgPrice();
 * }
 *
 * class BookStoreAvgPriceResolver implements TransientResolver<Long, BigDecimal>{
 *     ...omit code...
 * }}</pre>
 *
 * <p>Example-3: Complex calculation property whose return type is entity type</p>
 *
 * <pre>{@code
 * public class BookStore {
 *
 *     ...omit other properties...
 *
 *     @Id
 *     long id();
 *
 *     @Transient(BookStoreMostPopularAuthorResolver.class)
 *     Author mostPopularAuthor();
 * }
 *
 * class BookStoreMostPopularAuthorResolver implements TransientResolver<Long, Long>{
 *     ...omit code...
 * }}</pre>
 *
 * <p>Example-4: Complex calculation property whose return type is list of entity type</p>
 *
 * <pre>{@code
 * public class BookStore {
 *
 *     ...omit other properties...
 *
 *     @Id
 *     long id();
 *
 *     @Transient(BookStoreNewestBooksResolver.class)
 *     List<Book> newestBooks();
 * }
 *
 * class BookStoreNewestBooksResolver implements TransientResolver<Long, List<Long>>{
 *     ...omit code...
 * }}</pre>
 *
 * @see Formula
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface Transient {

    /**
     * <p>
     *     If this argument is not specified, the decorated property is a transient property,
     *     that means you can only get and set(by draft) it and ORM will ignore it.
     * </p>
     *
     * <p>
     *     Otherwise, the decorated property is a complex calculation property
     *     that means you can add it into ObjectFetcher and jimmer will load it in query.
     * </p>
     *
     * <p>
     *     `@Transient` with `value` means complex calculation means complex calculation property,
     *     You can also view {@link Formula} which can be used as simple calcluation property
     * </p>
     *
     *
     * @return
     * <p>
     *     A class implements
     *     `org.babyfish.jimmer.sql.TransientResolver` of jimmer-sql or
     *     `org.babyfish.jimmer.sql.kt.KTransientResolver` of jimmer-sql-kotlin.
     * <p>
     *
     * <p>The first type argument of `TransientResolver` should the `ID` of declaring entity type</p>
     * <ul>
     *     <li>
     *         If the current calculation property returns simple type,
     *         the second type argument of `TransientResolver` should be return type of current property
     *     </li>
     *     <li>
     *         If the current calculation property returns entity type,
     *         the second type argument of `TransientResolver` should be `ID` of target entity type
     *     </li>
     *     <li>
     *         If the current calculation property returns a list whose element type is entity type,
     *         the second type argument of `TransientResolver` should be
     *         a list whose element type is `ID` of target entity type
     *     </li>
     * </ul>
     */
    Class<?> value() default void.class;
}
