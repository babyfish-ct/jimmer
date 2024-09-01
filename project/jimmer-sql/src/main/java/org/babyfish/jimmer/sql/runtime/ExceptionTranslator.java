package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Exception translator for database exception,
 * no matter it is low-level JDBC exception,
 * such as {@link java.sql.SQLException};
 * or a relatively advanced Jimmer exception,
 * such as @{@link SaveException.NotUnique}
 *
 * <p>There are 3 ways to setup exception translators.</p>
 *
 * <ol>
 *     <li>Add it to save command, like this <pre>{@code
 *     sqlClient.getEntities()
 *          .saveCommand(...entity...)
 *          .addExceptionTranslator(
 *              new ExceptionTranslator<SaveException.NotUnique>() {
 *                  ...
 *              }
 *          )
 *     }</pre><p>This configuration has a higher priority. If the
 *     same exception type is configured twice by this method and
 *     the global configuration, the configuration of this method
 *     takes precedence.</p></li>
 *     <li>Global configuration without spring-starter, like this <pre>{@code
 *     JSqlClient sqlClient = JSqlClient
 *          .newBuilder()
 *          .addExceptionTranslator(
 *              new ExceptionTranslator<SaveException.NotUnique>() {
 *                  ...
 *              }
 *          )
 *     }</pre></li>
 *     <li>Global configuration by jimmer-spring-starter,
 *     by {@code @Component} of spring framework, such as <pre>{@code
 *     @Component // Let spring know your translator
 *     public NotUniqueExceptionTranslator
 *         implements ExceptionTranslator<SaveException.NotUnique> {
 *         ...
 *     }
 *     }</pre></li>
 * </ol>
 * @param <E> The translated exception type.
 *           <p>Note, this generic type must be specified by any class
 *           implements this interface, otherwise, exception will be raised</p>
 */
public interface ExceptionTranslator<E extends Exception> {

    @Nullable
    default Exception translate(@NotNull E exception, @NotNull Args args) {
        return exception;
    }

    interface Args {
        JSqlClientImplementor sqlClient();
        String sql();
        ExecutionPurpose purpose();
        ExecutorContext ctx();
    }

    static ExceptionTranslator<Exception> of(Collection<ExceptionTranslator<?>> translators) {
        return RuntimeExceptionTranslator.of(translators);
    }
}
