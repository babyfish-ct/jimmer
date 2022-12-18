package org.babyfish.jimmer.spring.repository.support;

import org.babyfish.jimmer.spring.repository.SpringConnectionManager;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Utils {

    private Utils() {}

    public static <E> Collection<E> toCollection(Iterable<E> iterable) {
        if (iterable instanceof Collection<?>) {
            return (Collection<E>) iterable;
        }
        if (iterable == null) {
            return Collections.emptyList();
        }
        List<E> list = new ArrayList<>();
        for (E e : iterable) {
            list.add(e);
        }
        return list;
    }

    public static void validateSqlClient(JSqlClient sqlClient) {
        if (!(sqlClient.getConnectionManager() instanceof SpringConnectionManager)) {
            throw new IllegalArgumentException(
                    "The connection manager of sql client must be instance of \"" +
                            SpringConnectionManager.class.getName() +
                            "\""
            );
        }
        ConnectionManager slaveConnectionManager = sqlClient.getSlaveConnectionManager(false);
        if (slaveConnectionManager != null && !(slaveConnectionManager instanceof SpringConnectionManager)) {
            throw new IllegalArgumentException(
                    "The slave connection manager of sql client must be null or instance of \"" +
                            SpringConnectionManager.class.getName() +
                            "\""
            );
        }
        if (sqlClient.getEntityManager() == null) {
            throw new IllegalArgumentException(
                    "The entity manager of of sql client must be specified, " +
                            "please specify it by the constant in the generated source code."
            );
        }
    }
}
