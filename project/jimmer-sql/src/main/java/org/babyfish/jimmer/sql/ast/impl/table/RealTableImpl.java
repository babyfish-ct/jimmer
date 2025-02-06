package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.association.meta.AssociationProp;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.query.UseTableVisitor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.babyfish.jimmer.sql.runtime.TableUsedState;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

class RealTableImpl extends AbstractDataManager<RealTableImpl.Key, RealTableImpl> implements RealTable {

    private final Key key;

    private final TableImpl<?> owner;
    
    private final RealTableImpl parent;

    private final Predicate joinPredicate;
    
    private JoinType joinType;

    private String alias;

    private String middleTableAlias;

    RealTableImpl(TableImpl<?> owner) {
        this(
                new Key(null, false, null, null),
                owner,
                null
        );
    }

    private RealTableImpl(
            Key key,
            TableImpl<?> owner,
            RealTableImpl parent
    ) {
        this.key = key;
        this.owner = owner;
        this.parent = parent;
        this.joinType = owner.getJoinType();
        if (owner.weakJoinHandle != null) {
            joinPredicate = owner.weakJoinHandle.createPredicate(
                    owner.parent,
                    owner,
                    owner.statement
            );
        } else {
            joinPredicate = null;
        }
    }

    @Override
    public final TableImplementor<?> getTableImplementor() {
        return owner;
    }

    @Override
    public final RealTable getParent() {
        return parent;
    }

    @Override
    public final String getAlias() {
        return alias;
    }

    @Override
    public final String getMiddleTableAlias() {
        return middleTableAlias;
    }

    @Override
    public String getFinalAlias(
            ImmutableProp prop,
            boolean rawId,
            JSqlClientImplementor sqlClient
    ) {
        ImmutableProp joinProp = owner.joinProp;
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        if (prop.isId() && joinProp != null && !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                (rawId || TableUtils.isRawIdAllowed(owner, sqlClient))) {
            MiddleTable middleTable;
            if (joinProp.isMiddleTableDefinition()) {
                middleTable = joinProp.getStorage(strategy);
            } else {
                middleTable = null;
            }
            boolean isInverse = owner.isInverse;
            if (middleTable != null) {
                return middleTableAlias;
            }
            if (!isInverse) {
                return parent.alias;
            }
        }
        return alias;
    }

    public RealTableImpl child(JoinTypeMergeScope scope, TableImpl<?> owner) {
        Key key = new Key(scope, owner.isInverse, owner.joinProp, owner.weakJoinHandle);
        RealTableImpl child = getValue(key);
        if (child != null) {
            if (child.joinType != owner.getJoinType()) {
                child.joinType = JoinType.INNER;
            }
            return child;
        }
        child = new RealTableImpl(key, owner, this);
        putValue(key, child);
        return child;
    }

    @Override
    public void use(UseTableVisitor visitor) {
        if (joinPredicate != null) {
            ((Ast)joinPredicate).accept(visitor);
        }
        for (RealTable childTable : this) {
            childTable.use(visitor);
        }
    }

    @Override
    public void renderJoinAsFrom(SqlBuilder builder, TableImplementor.RenderMode mode) {
        TableImpl<?> owner = this.owner;
        if (owner == null) {
            throw new IllegalStateException("Internal bug: renderJoinAsFrom can only be called base on joined tables");
        }
        if (mode == TableImplementor.RenderMode.NORMAL) {
            throw new IllegalStateException("Internal bug: renderJoinAsFrom does not accept render mode ALL");
        }
        TableUsedState usedState = builder.getAstContext().getTableUsedState(this);
        if (usedState != TableUsedState.NONE) {
            if (mode == TableImplementor.RenderMode.FROM_ONLY || mode == TableImplementor.RenderMode.WHERE_ONLY) {
                builder.separator();
            }
            renderSelf(builder, mode);
            if (mode == TableImplementor.RenderMode.DEEPER_JOIN_ONLY) {
                for (RealTableImpl childTable : this) {
                    childTable.renderTo(builder);
                }
            }
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        TableImpl<?> owner = this.owner;
        SqlBuilder sqlBuilder = builder.assertSimple();
        TableUsedState usedState = sqlBuilder.getAstContext().getTableUsedState(this);
        if (owner.parent == null || usedState != TableUsedState.NONE) {
            renderSelf(sqlBuilder, TableImplementor.RenderMode.NORMAL);
            for (RealTableImpl childTable : this) {
                childTable.renderTo(sqlBuilder);
            }
        }
    }

    private void renderSelf(SqlBuilder builder, TableImplementor.RenderMode mode) {
        TableImpl<?> owner = this.owner;
        AbstractMutableStatementImpl statement = owner.statement;
        Predicate filterPredicate;
        if (owner.isInverse) {
            renderInverseJoin(builder, mode);
            filterPredicate = statement.getFilterPredicate(owner, builder.getAstContext());
        } else if (owner.joinProp != null || owner.weakJoinHandle != null) {
            renderJoin(builder, mode);
            filterPredicate = statement.getFilterPredicate(owner, builder.getAstContext());
        } else {
            builder
                    .from()
                    .sql(owner.immutableType.getTableName(builder.getAstContext().getSqlClient().getMetadataStrategy()))
                    .sql(" ")
                    .sql(alias);
            filterPredicate = null;
        }
        if (filterPredicate != null) {
            builder.sql(" and ");
            ((Ast)filterPredicate).renderTo(builder);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderJoin(SqlBuilder builder, TableImplementor.RenderMode mode) {

        TableImpl<?> owner = this.owner;
        MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();

        if (owner.weakJoinHandle != null) {
            if (builder.getAstContext().getTableUsedState(this) != TableUsedState.NONE) {
                builder
                        .join(joinType)
                        .sql(owner.immutableType.getTableName(strategy))
                        .sql(" ")
                        .sql(alias)
                        .on();
                if (joinPredicate == null) {
                    builder.sql("1 = 1");
                } else {
                    ((Ast)joinPredicate).renderTo(builder);
                }
            }
            return;
        }

        ImmutableProp joinProp = owner.joinProp;
        ImmutableType immutableType = owner.immutableType;
        if (joinProp.getSqlTemplate() instanceof JoinTemplate) {
            renderJoinBySql(builder, (JoinTemplate) joinProp.getSqlTemplate(), mode);
            return;
        }

        if (joinProp instanceof AssociationProp) {
            if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED) {
                renderJoinImpl(
                        builder,
                        joinType,
                        parent.alias,
                        joinProp.getStorage(strategy),
                        immutableType.getTableName(strategy),
                        alias,
                        immutableType.getIdProp().getStorage(strategy),
                        mode
                );
                renderMiddleTableFilters(
                        ((AssociationProp)joinProp).getDeclaringType().getMiddleTable(strategy),
                        parent.alias,
                        builder
                );
            }
            return;
        }

        MiddleTable middleTable = null;
        if (joinProp.isMiddleTableDefinition()) {
            middleTable = joinProp.getStorage(strategy);
        }

        if (middleTable != null) {
            renderJoinImpl(
                    builder,
                    joinType,
                    parent.alias,
                    owner.parent.immutableType.getIdProp().getStorage(strategy),
                    middleTable.getTableName(),
                    middleTableAlias,
                    middleTable.getColumnDefinition(),
                    mode
            );
            renderMiddleTableFilters(
                    middleTable,
                    middleTableAlias,
                    builder
            );
            if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED && (
                    mode == TableImplementor.RenderMode.NORMAL ||
                            mode == TableImplementor.RenderMode.DEEPER_JOIN_ONLY)
            ) {
                renderJoinImpl(
                        builder,
                        joinType,
                        middleTableAlias,
                        middleTable.getTargetColumnDefinition(),
                        immutableType.getTableName(strategy),
                        alias,
                        immutableType.getIdProp().getStorage(strategy),
                        TableImplementor.RenderMode.NORMAL
                );
            }
        } else if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED) {
            renderJoinImpl(
                    builder,
                    joinType,
                    parent.alias,
                    joinProp.getStorage(strategy),
                    immutableType.getTableName(strategy),
                    alias,
                    immutableType.getIdProp().getStorage(strategy),
                    mode
            );
        }
    }

    private void renderInverseJoin(SqlBuilder builder, TableImplementor.RenderMode mode) {

        MetadataStrategy strategy = builder.sqlClient().getMetadataStrategy();
        ImmutableType immutableType = owner.immutableType;
        ImmutableProp joinProp = owner.joinProp;

        if (joinProp.getSqlTemplate() instanceof JoinTemplate) {
            renderJoinBySql(builder, (JoinTemplate) joinProp.getSqlTemplate(), mode);
            return;
        }

        MiddleTable middleTable = null;
        if (joinProp.isMiddleTableDefinition()) {
            middleTable = joinProp.getStorage(strategy);
        }

        if (middleTable != null) {
            renderJoinImpl(
                    builder,
                    joinType,
                    parent.alias,
                    owner.parent.immutableType.getIdProp().getStorage(strategy),
                    middleTable.getTableName(),
                    middleTableAlias,
                    middleTable.getTargetColumnDefinition(),
                    mode
            );
            renderMiddleTableFilters(
                    middleTable,
                    middleTableAlias,
                    builder
            );
            if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED && (
                    mode == TableImplementor.RenderMode.NORMAL ||
                            mode == TableImplementor.RenderMode.DEEPER_JOIN_ONLY)
            ) {
                renderJoinImpl(
                        builder,
                        joinType,
                        middleTableAlias,
                        middleTable.getColumnDefinition(),
                        immutableType.getTableName(strategy),
                        alias,
                        immutableType.getIdProp().getStorage(strategy),
                        TableImplementor.RenderMode.NORMAL
                );
            }
        } else { // One-to-many join cannot be optimized by "used"
            renderJoinImpl(
                    builder,
                    joinType,
                    parent.alias,
                    owner.parent.immutableType.getIdProp().getStorage(strategy),
                    immutableType.getTableName(strategy),
                    alias,
                    joinProp.getStorage(strategy),
                    mode
            );
        }
    }

    private void renderJoinBySql(
            SqlBuilder builder,
            JoinTemplate joinTemplate,
            TableImplementor.RenderMode mode
    ) {
        if (builder.getAstContext().getTableUsedState(this) != TableUsedState.NONE) {
            ImmutableType immutableType = owner.immutableType;
            MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();
            switch (mode) {
                case NORMAL:
                    builder
                            .join(joinType)
                            .sql(immutableType.getTableName(strategy))
                            .sql(" ")
                            .sql(alias)
                            .on();
                    break;
                case FROM_ONLY:
                    builder
                            .sql(immutableType.getTableName(strategy))
                            .sql(" ")
                            .sql(alias);
                    break;
            }
            if (mode == TableImplementor.RenderMode.NORMAL || mode == TableImplementor.RenderMode.WHERE_ONLY) {
                if (owner.isInverse) {
                    builder.sql(joinTemplate.toSql(alias, parent.alias));
                } else {
                    builder.sql(joinTemplate.toSql(parent.alias, alias));
                }
            }
        }
    }

    private void renderJoinImpl(
            SqlBuilder builder,
            JoinType joinType,
            String previousAlias,
            ColumnDefinition previousDefinition,
            String newTableName,
            String newAlias,
            ColumnDefinition newDefinition,
            TableImplementor.RenderMode mode
    ) {
        if (mode != TableImplementor.RenderMode.NORMAL && joinType != JoinType.INNER) {
            throw new AssertionError("Internal bug: outer join cannot be accepted by abnormal render mode");
        }
        switch (mode) {
            case NORMAL:
                builder
                        .join(joinType)
                        .sql(newTableName)
                        .sql(" ")
                        .sql(newAlias)
                        .on();
                break;
            case FROM_ONLY:
                builder
                        .sql(newTableName)
                        .sql(" ")
                        .sql(newAlias);
                break;
        }
        if (mode == TableImplementor.RenderMode.NORMAL || mode == TableImplementor.RenderMode.WHERE_ONLY) {
            int size = previousDefinition.size();
            builder.enter(SqlBuilder.ScopeType.AND);
            for (int i = 0; i < size; i++) {
                builder.separator();
                builder
                        .sql(previousAlias)
                        .sql(".")
                        .sql(previousDefinition.name(i))
                        .sql(" = ")
                        .sql(newAlias)
                        .sql(".")
                        .sql(newDefinition.name(i));
            }
            builder.leave();

        }
    }

    private void renderMiddleTableFilters(
            MiddleTable middleTable,
            String middleTableAlias,
            SqlBuilder builder
    ) {
        ImmutableProp joinProp = owner.joinProp;
        LogicalDeletedInfo deletedInfo = middleTable.getLogicalDeletedInfo();
        JSqlClient sqlClient = builder.getAstContext().getSqlClient();
        if (deletedInfo != null &&
                sqlClient.getFilters().getBehavior(joinProp) != LogicalDeletedBehavior.IGNORED) {
            builder.sql(" and ");
            JoinTableFilters.render(sqlClient.getFilters().getBehavior(joinProp), deletedInfo, middleTableAlias, builder);
        }
        JoinTableFilterInfo filterInfo = middleTable.getFilterInfo();
        if (filterInfo != null) {
            builder.sql(" and ");
            JoinTableFilters.render(filterInfo, middleTableAlias, builder);
        }
    }

    public void renderSelection(
            ImmutableProp prop,
            boolean rawId,
            AbstractSqlBuilder<?> builder,
            ColumnDefinition optionalDefinition,
            boolean withPrefix,
            Function<Integer, String> asBlock
    ) {
        ImmutableProp joinProp = owner.joinProp;
        MetadataStrategy strategy = builder.sqlClient().getMetadataStrategy();
        if (prop.isId() && joinProp != null && !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                (rawId || TableUtils.isRawIdAllowed(owner, builder.sqlClient()))) {
            MiddleTable middleTable;
            if (joinProp.isMiddleTableDefinition()) {
                middleTable = joinProp.getStorage(strategy);
            } else {
                middleTable = null;
            }
            boolean isInverse = owner.isInverse;
            if (middleTable != null) {
                if (optionalDefinition == null) {
                    if (isInverse) {
                        builder.definition(withPrefix ? middleTableAlias : null, middleTable.getColumnDefinition(), asBlock);
                    } else {
                        builder.definition(withPrefix ? middleTableAlias : null, middleTable.getTargetColumnDefinition(), asBlock);
                    }
                } else {
                    ColumnDefinition fullDefinition = prop.getStorage(strategy);
                    ColumnDefinition parentDefinition = isInverse ?
                            middleTable.getColumnDefinition() :
                            middleTable.getTargetColumnDefinition();
                    int size = optionalDefinition.size();
                    for (int i = 0; i < size; i++) {
                        if (i != 0) {
                            builder.sql(", ");
                        }
                        int index = fullDefinition.index(optionalDefinition.name(i));
                        String parentColumnName = parentDefinition.name(index);
                        if (withPrefix) {
                            builder.sql(middleTableAlias).sql(".");
                        }
                        builder.sql(parentColumnName);
                        if (asBlock != null) {
                            builder.sql(" ").sql(asBlock.apply(i));
                        }
                    }
                }
                return;
            }
            if (!isInverse) {
                if (optionalDefinition == null) {
                    builder.definition(withPrefix ? parent.alias : null, joinProp.getStorage(strategy), asBlock);
                } else {
                    ColumnDefinition fullDefinition = prop.getStorage(strategy);
                    ColumnDefinition parentDefinition = joinProp.getStorage(strategy);
                    int size = optionalDefinition.size();
                    for (int i = 0; i < size; i++) {
                        if (i != 0) {
                            builder.sql(", ");
                        }
                        int index = fullDefinition.index(optionalDefinition.name(i));
                        String parentColumnName = parentDefinition.name(index);
                        if (withPrefix) {
                            builder.sql(parent.alias).sql(".");
                        }
                        builder.sql(parentColumnName);
                        if (asBlock != null) {
                            builder.sql(" ").sql(asBlock.apply(i));
                        }
                    }
                }
                return;
            }
        }
        SqlTemplate template = prop.getSqlTemplate();
        if (template instanceof FormulaTemplate) {
            builder.sql(((FormulaTemplate)template).toSql(alias));
            if (asBlock != null) {
                builder.sql(" ").sql(asBlock.apply(0));
            }
        } else {
            ColumnDefinition definition = optionalDefinition != null ?
                    optionalDefinition :
                    prop.getStorage(strategy);
            builder.definition(withPrefix ? alias : null, definition, asBlock);
        }
    }

    @Override
    public String toString() {
        return "RealTable{" +
                "key=" + key +
                ", joinType=" + joinType +
                '}';
    }

    static class Key {

        final JoinTypeMergeScope scope;

        final WeakJoinHandle weakJoinHandle;

        final String joinName;

        Key(
                JoinTypeMergeScope scope,
                boolean inverse,
                ImmutableProp joinProp,
                WeakJoinHandle weakJoinHandle
        ) {
            this.scope = scope;
            this.weakJoinHandle = weakJoinHandle;
            String joinName;
            if (joinProp == null) {
                joinName = "";
            } else if (inverse) {
                ImmutableProp opposite = joinProp.getOpposite();
                if (opposite != null) {
                    joinName = opposite.getName();
                } else {
                    joinName = "←" + joinProp.getName();
                }
            } else {
                joinName = joinProp.getName();
            }
            this.joinName = joinName;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(scope);
            result = 31 * result + joinName.hashCode();
            result = 31 * result + (weakJoinHandle != null ? weakJoinHandle.getWeakJoinType().hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Key other = (Key) o;
            if (scope != other.scope) {
                return false;
            }
            if (!joinName.equals(other.joinName)) {
                return false;
            }
            return (weakJoinHandle != null ? weakJoinHandle.getWeakJoinType() : null) ==
                    (other.weakJoinHandle != null ? other.weakJoinHandle.getWeakJoinType() : null);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "scope=" + scope +
                    ", joinName=" + joinName +
                    ", weakJoinHandle=" + weakJoinHandle +
                    "}";
        }
    }

    @Override
    public final void allocateAliases() {

        if (alias == null) {
            AbstractMutableStatementImpl statement = owner.statement;
            StatementContext ctx = statement.getContext();
            ImmutableProp joinProp = owner.joinProp;
            if (joinProp != null) {
                if (joinProp.isMiddleTableDefinition()) {
                    middleTableAlias = statement.getContext().allocateTableAlias();
                } else if (joinProp.getSqlTemplate() == null && !joinProp.hasStorage()) {
                    //throw new AssertionError("Internal bug: Join property has not storage");
                    middleTableAlias = null;
                } else {
                    middleTableAlias = null;
                }
            } else {
                middleTableAlias = null;
            }
            String alias = ctx.allocateTableAlias();
            final JSqlClientImplementor sqlClient = statement.getSqlClient();
            if (alias.equals("tb_1_") && sqlClient != null &&
                    (!sqlClient.getDialect().isUpdateAliasSupported() && ctx.getPurpose().toString().startsWith("UPDATE") ||
                            (!sqlClient.getDialect().isDeleteAliasSupported() && ctx.getPurpose().toString().startsWith("DELETE")))
            ) {
                alias = statement.getType().getTableName(sqlClient.getMetadataStrategy());
            }
            this.alias = alias;
        }

        for (RealTableImpl childTable : this) {
            childTable.allocateAliases();
        }
    }
}
