package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.Collection;
import java.util.Map;

public interface Entities {

    Executable<Map<String, Integer>> deleteCommand(
            Class<?> entityType,
            Object id
    );

    Executable<Map<String, Integer>> batchDeleteCommand(
            Class<?> entityType,
            Collection<?> ids
    );

    Executable<Map<String, Integer>> batchDeleteCommand(
            Class<?> entityType,
            Object ... ids
    );
}
