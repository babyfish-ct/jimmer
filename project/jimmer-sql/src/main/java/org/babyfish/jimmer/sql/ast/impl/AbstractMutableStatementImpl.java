package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasAllocator;

public class AbstractMutableStatementImpl {

    private TableAliasAllocator tableAliasAllocator;

    private SqlClient sqlClient;

    private boolean frozen;

    public AbstractMutableStatementImpl(
            TableAliasAllocator tableAliasAllocator,
            SqlClient sqlClient
    ) {
        this.tableAliasAllocator = tableAliasAllocator;
        this.sqlClient = sqlClient;
    }

    public void freeze() {
        frozen = true;
    }

    public void validateMutable() {
        if (frozen) {
            throw new IllegalStateException(
                    "Cannot mutate the statement because it has been frozen"
            );
        }
    }

    public TableAliasAllocator getTableAliasAllocator() {
        return tableAliasAllocator;
    }

    public SqlClient getSqlClient() {
        return sqlClient;
    }
}
