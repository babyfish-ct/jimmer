package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasAllocator;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
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

    public MutableDeleteImpl(SqlClient sqlClient, ImmutableType immutableType) {
        super(new TableAliasAllocator(), sqlClient);
        deleteQuery = new MutableRootQueryImpl<>(sqlClient, immutableType);
    }

    @SuppressWarnings("unchecked")
    public <T extends Table<?>> T getTable() {
        return (T)deleteQuery.getTable();
    }

    @Override
    public MutableDelete where(Predicate... predicates) {
        deleteQuery.where(predicates);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Integer execute(Connection con) {
        SqlClient sqlClient = getSqlClient();
        TableImplementor<?> table = TableImplementor.unwrap(deleteQuery.getTable());
        if (table.getChildren().isEmpty()) {
            SqlBuilder builder = new SqlBuilder(sqlClient);
            renderDirectly(builder);
            Tuple2<String, List<Object>> sqlResult = builder.build();
            return sqlClient.getExecutor().execute(con, sqlResult._1(), sqlResult._2(), PreparedStatement::executeUpdate);
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
        TableImplementor<?> table = TableImplementor.unwrap(deleteQuery.getTable());
        builder.sql("delete from ");
        builder.sql(table.getImmutableType().getTableName());
        builder.sql(" as ");
        builder.sql(table.getAlias());
        String separator = " where ";
        for (Predicate predicate : deleteQuery.getPredicates()) {
            builder.sql(separator);
            separator = " and ";
            ((Ast) predicate).renderTo(builder);
        }
    }
}
