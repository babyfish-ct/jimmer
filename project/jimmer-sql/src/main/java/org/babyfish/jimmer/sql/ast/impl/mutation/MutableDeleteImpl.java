package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.UseTableVisitor;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MutableDeleteImpl
        extends AbstractMutableStatementImpl
        implements MutableDelete {

    private MutableRootQueryImpl<TableEx<?>> deleteQuery;

    public MutableDeleteImpl(JSqlClient sqlClient, ImmutableType immutableType) {
        super(sqlClient, immutableType);
        deleteQuery = new MutableRootQueryImpl<>(
                new StatementContext(ExecutionPurpose.QUERY, false),
                sqlClient,
                immutableType
        );
    }

    public MutableDeleteImpl(JSqlClient sqlClient, TableProxy<?> table) {
        super(sqlClient, table);
        deleteQuery = new MutableRootQueryImpl<>(
                new StatementContext(ExecutionPurpose.QUERY, false),
                sqlClient,
                table
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Table<?>> T getTable() {
        return deleteQuery.getTable();
    }

    @Override
    public TableImplementor<?> getTableImplementor() {
        return deleteQuery.getTableImplementor();
    }

    @Override
    public AbstractMutableStatementImpl getParent() {
        return null;
    }

    @Override
    public StatementContext getContext() {
        return deleteQuery.getContext();
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
        freeze();

        JSqlClient sqlClient = getSqlClient();
        TableImplementor<?> table = getTableImplementor();

        AstContext astContext = new AstContext(sqlClient);
        astContext.pushStatement(deleteQuery);
        try {
            Predicate predicate = deleteQuery.getPredicate();
            if (predicate != null) {
                ((Ast) predicate).accept(new UseTableVisitor(astContext));
            }
        } finally {
            astContext.popStatement();
        }

        boolean binLogOnly = sqlClient.getTriggerType() == TriggerType.BINLOG_ONLY;
        if (table.isEmpty() && binLogOnly) {
            SqlBuilder builder = new SqlBuilder(astContext);
            astContext.pushStatement(this);
            try {
                renderDirectly(builder);
                Tuple2<String, List<Object>> sqlResult = builder.build();
                return sqlClient.getExecutor().execute(
                        con,
                        sqlResult.get_1(),
                        sqlResult.get_2(),
                        getPurpose(),
                        null,
                        PreparedStatement::executeUpdate
                );
            } finally {
                astContext.popStatement();
            }
        }
        List<Object> ids;
        MutationCache cache;
        if (binLogOnly) {
            ids = deleteQuery
                    .select((Expression<Object>) table.get(table.getImmutableType().getIdProp().getName()))
                    .distinct()
                    .execute(con);
            cache = null;
        } else {
            List<ImmutableSpi> rows = (List<ImmutableSpi>) deleteQuery
                    .select(table)
                    .execute(con);
            int idPropId = table.getImmutableType().getIdProp().getId();
            ids = new ArrayList<>(rows.size());
            cache = new MutationCache(sqlClient);
            for (ImmutableSpi row : rows) {
                cache.save(row, false);
                ids.add(row.__get(idPropId));
            }
        }
        if (ids.isEmpty()) {
            return 0;
        }
        Deleter deleter = new Deleter(
                new DeleteCommandImpl.Data(sqlClient),
                con,
                cache,
                binLogOnly ? null : new MutationTrigger(),
                new HashMap<>()
        );
        deleter.addPreHandleInput(table.getImmutableType(), ids);
        return deleter.execute(true).getTotalAffectedRowCount();
    }

    private void renderDirectly(SqlBuilder builder) {
        TableImplementor<?> table = getTableImplementor();
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
            ((Ast) predicate).renderTo(builder);
        }
    }
}
