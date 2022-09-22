package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.UseTableVisitor;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasAllocator;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableWrappers;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public class MutableDeleteImpl
        extends AbstractMutableStatementImpl
        implements MutableDelete, Executable<Integer> {

    private MutableRootQueryImpl<TableEx<?>> deleteQuery;

    public MutableDeleteImpl(JSqlClient sqlClient, ImmutableType immutableType) {
        super(new TableAliasAllocator(), sqlClient);
        deleteQuery = new MutableRootQueryImpl<>(
                this.getTableAliasAllocator(),
                sqlClient,
                immutableType
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Table<?>> T getTable() {
        return deleteQuery.getTable();
    }

    @Override
    public MutableDelete where(Predicate... predicates) {
        deleteQuery.where(predicates);
        return this;
    }

    @Override
    public Integer execute() {
        return getSqlClient()
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @Override
    public Integer execute(Connection con) {
        if (con != null) {
            return executeImpl(con);
        }
        return getSqlClient()
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @SuppressWarnings("unchecked")
    private Integer executeImpl(Connection con) {
        JSqlClient sqlClient = getSqlClient();
        TableImplementor<?> table = TableWrappers.unwrap(deleteQuery.getTable());
        if (table.getChildren().isEmpty()) {
            SqlBuilder builder = new SqlBuilder(sqlClient);
            Ast ast = (Ast) deleteQuery.getPredicate();
            if (ast != null) {
                ast.accept(new UseTableVisitor(builder));
            }
            renderDirectly(builder);
            Tuple2<String, List<Object>> sqlResult = builder.build();
            return sqlClient.getExecutor().execute(
                    con,
                    sqlResult.get_1(),
                    sqlResult.get_2(),
                    null,
                    PreparedStatement::executeUpdate
            );
        }
        List<Object> ids = deleteQuery
                .select((Expression<Object>)table.get(table.getImmutableType().getIdProp().getName()))
                .distinct()
                .execute(con);
        if (ids.isEmpty()) {
            return 0;
        }
        return getSqlClient().getEntities().batchDeleteCommand(
                table.getImmutableType().getJavaClass(),
                ids
        ).execute(con).getTotalAffectedRowCount();
    }

    private void renderDirectly(SqlBuilder builder) {
        TableImplementor<?> table = TableWrappers.unwrap(deleteQuery.getTable());
        builder.sql("delete");
        if (getSqlClient().getDialect().needDeletedAlias()) {
            builder.sql(" ");
            builder.sql(table.getAlias());
        }
        builder.sql(" from ");
        builder.sql(table.getImmutableType().getTableName());
        builder.sql(" as ");
        builder.sql(table.getAlias());
        Predicate predicate = deleteQuery.getPredicate();
        if (predicate != null) {
            builder.sql(" where ");
            ((Ast)predicate).renderTo(builder);
        }
    }
}
