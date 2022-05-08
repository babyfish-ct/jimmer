package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.SqlClient;

class AbstractMutableStatementImpl {

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

    void freeze() {
        frozen = true;
    }

    void validateMutable() {
        if (frozen) {
            throw new IllegalStateException(
                    "Cannot mutate the statement because it has been frozen"
            );
        }
    }

    TableAliasAllocator getTableAliasAllocator() {
        return tableAliasAllocator;
    }

    SqlClient getSqlClient() {
        return sqlClient;
    }
}
