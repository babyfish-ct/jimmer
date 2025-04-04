package org.babyfish.jimmer.sql.transaction;

import org.babyfish.jimmer.sql.runtime.ConnectionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;

/**
 * A connection manager with transaction management mechanism,
 * whether using an IOC framework or not.
 *
 * <ul>
 *     <li>If an IOC framework is used, its implementation
 *     should be an encapsulation of the transaction management
 *     within the IOC framework. Taking {@code jimmer-spring-starter}
 *     as an example, it is the {@code SpringConnectionManager}
 *     which will be created and enabled automatically.</li>
 *
 *     <li>If no IOC framework is used, the class
 *     {@link AbstractTxConnectionManager} is the
 *     lightweight implementation provided by jimmer,
 *     please specify the connection manager of sqlClient by
 *     {@link ConnectionManager#simpleConnectionManager(DataSource)}</li>
 * </ul>
 */
public interface TxConnectionManager extends ConnectionManager {

    default <R> R execute(Function<Connection, R> block) {
        return executeTransaction(Propagation.SUPPORTS, block);
    }

    default <R> R executeTransaction(Function<Connection, R> block) {
        return executeTransaction(Propagation.REQUIRED, block);
    }

    <R> R executeTransaction(Propagation propagation, Function<Connection, R> block);
}
