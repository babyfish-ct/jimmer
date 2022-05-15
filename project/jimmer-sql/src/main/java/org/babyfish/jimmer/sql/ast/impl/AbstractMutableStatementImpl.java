package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasAllocator;

import java.util.Objects;

public class AbstractMutableStatementImpl {

    private TableAliasAllocator tableAliasAllocator;

    private SqlClient sqlClient;

    private boolean frozen;

    public AbstractMutableStatementImpl(
            TableAliasAllocator tableAliasAllocator,
            SqlClient sqlClient
    ) {
        this.tableAliasAllocator = tableAliasAllocator;
        if (!(this instanceof Fake)) {
            Objects.requireNonNull(sqlClient, "sqlClient cannot be null");
            this.sqlClient = sqlClient;
        }
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
        SqlClient client = sqlClient;
        if (client == null) {
            throw new UnsupportedOperationException(
                    "getSqlClient() is not supported by " + Fake.class.getName()
            );
        }
        return client;
    }

    public static AbstractMutableStatementImpl fake() {
        return new Fake();
    }

    private static class Fake extends AbstractMutableStatementImpl {

        private Fake() {
            super(new TableAliasAllocator(), null);
        }
    }
}
