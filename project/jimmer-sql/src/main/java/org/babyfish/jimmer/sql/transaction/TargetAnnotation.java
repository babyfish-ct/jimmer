package org.babyfish.jimmer.sql.transaction;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Jimmer supports compile-time transaction management AOP implementation.
 * If a class or its methods are annotated with {@link Tx}, Jimmer's
 * APT/KSP will automatically generate a derived class for it.
 *
 * <p>This annotation includes a parameter of another annotation type,
 * and that annotation type represented by this parameter should support
 * parameterless usage. This method allows specifying an annotation for
 * the automatically generated derived class, rather than the current class.
 * For example:</p>
 *
 * <p>User writes code like this:</p>
 * <pre>{@code
 * @TargetAnnotation(Component.class)
 * @Tx(Propgation.REQUIRED)
 * public class BookRepository {
 *
 *     final JSqlClient sqlClient;
 *
 *     public BookRepository(JSqlClient sqlClient) {
 *         this.sqlClient = sqlClient;
 *     }
 *
 *     public List<Book> findBooks() {
 *         ... ...
 *     }
 *
 *     @Tx(Propgation.MANDATORY)
 *     public void saveBook(BookInput input) {
 *         ... ...
 *     }
 * }
 * }</pre>
 *
 * <p>After compilation, a derived class will be generated automatically:</p>
 * <pre>{@code
 * @Component
 * public class BookRepositoryTx extends BookRepository {
 *
 *     public BookRepository(JSqlClient sqlClient) {
 *         super(sqlClient);
 *     }
 *
 *     @Override
 *     public List<Book> findBooks() {
 *         return sqlClient.transaction(Propagation.REQUIRED, con -> {
 *             return super.findBooks();
 *         });
 *     }
 *
 *     @Override
 *     public void saveBook(BookInput input) {
 *         sqlClient.transaction(Propagation.MANDATORY, con -> {
 *             super.saveBook(input);
 *             return null;
 *         });
 *     }
 * }
 * }</pre>
 *
 * <p>As we can see, the automatically generated derived class
 * will be annotated by {@code @Component}, but the current class
 * remains unannotated.</p>
 */
@Target(ElementType.TYPE)
public @interface TargetAnnotation {
    Class<? extends Annotation> value();
}
