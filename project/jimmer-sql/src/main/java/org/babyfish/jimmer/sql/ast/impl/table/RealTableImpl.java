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
import org.babyfish.jimmer.sql.ast.impl.base.BaseSelectionMapper;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.query.UseTableVisitor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.babyfish.jimmer.sql.runtime.TableUsedState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

class RealTableImpl extends AbstractDataManager<RealTable.Key, RealTable> implements RealTable {

    private final Key key;

    private final TableLikeImplementor<?> owner;
    
    private final RealTableImpl parent;

    private final Predicate joinPredicate;

    private BaseTableOwner baseTableOwner;

    private boolean baseTableOwnerResolved;
    
    private JoinType joinType;

    private String alias;

    private String middleTableAlias;

    RealTableImpl(TableLikeImplementor<?> owner) {
        this(
                new Key(null, false, null, null),
                owner,
                null
        );
    }

    private RealTableImpl(
            Key key,
            TableLikeImplementor<?> owner,
            RealTableImpl parent
    ) {
        this.key = key;
        this.owner = owner;
        this.parent = parent;
        if (owner instanceof TableImpl<?>) {
            TableImpl<?> tableImpl = (TableImpl<?>) owner;
            this.joinType = tableImpl.getJoinType();
            if (tableImpl.weakJoinHandle != null) {
                joinPredicate = tableImpl.weakJoinHandle.createPredicate(
                        tableImpl.parent,
                        tableImpl,
                        tableImpl.statement
                );
            } else {
                joinPredicate = null;
            }
        } else {
            this.joinType = JoinType.INNER;
            this.joinPredicate = null;
        }
    }

    @Override
    public final TableLikeImplementor<?> getTableLikeImplementor() {
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
        if (!(owner instanceof TableImpl<?>)) {
            return alias;
        }
        TableImpl<?> tableImpl = (TableImpl<?>) owner;
        ImmutableProp joinProp = tableImpl.joinProp;
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        if (prop.isId() && joinProp != null && !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                (rawId || TableUtils.isRawIdAllowed(tableImpl, sqlClient))) {
            MiddleTable middleTable;
            if (joinProp.isMiddleTableDefinition()) {
                middleTable = joinProp.getStorage(strategy);
            } else {
                middleTable = null;
            }
            boolean isInverse = tableImpl.isInverse;
            if (middleTable != null) {
                return middleTableAlias;
            }
            if (!isInverse) {
                return parent.alias;
            }
        }
        return alias;
    }

    @Override
    @Nullable
    public BaseTableOwner getBaseTableOwner() {
        if (!baseTableOwnerResolved) {
            baseTableOwner = createBaseTableOwner();
            baseTableOwnerResolved = true;
        }
        return baseTableOwner;
    }

    private BaseTableOwner createBaseTableOwner() {
        if (parent != null) {
            return parent.getBaseTableOwner();
        }
        return BaseTableOwner.of(owner);
    }

    public RealTableImpl child(JoinTypeMergeScope scope, TableImpl<?> owner) {
        Key key = new Key(scope, owner.isInverse, owner.joinProp, owner.weakJoinHandle);
        RealTableImpl child = (RealTableImpl) getValue(key);
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

    public RealTable getChild(Key key) {
        return getValue(key);
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
        if (owner == null) {
            throw new IllegalStateException("Internal bug: renderJoinAsFrom can only be called base on joined tables");
        }
        if (!(owner instanceof TableImpl<?>)) {

        } else {
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
                    for (RealTable childTable : this) {
                        childTable.renderTo(builder);
                    }
                }
            }
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        TableLikeImplementor<?> owner = this.owner;
        SqlBuilder sqlBuilder = builder.assertSimple();
        TableUsedState usedState = sqlBuilder.getAstContext().getTableUsedState(this);
        if (owner.getParent() == null || usedState != TableUsedState.NONE) {
            renderSelf(sqlBuilder, TableImplementor.RenderMode.NORMAL);
            for (RealTable childTable : this) {
                childTable.renderTo(sqlBuilder);
            }
        }
    }

    private void renderSelf(SqlBuilder builder, TableImplementor.RenderMode mode) {
        TableLikeImplementor<?> owner = this.owner;
        if (owner instanceof TableImplementor<?>) {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) owner;
            AbstractMutableStatementImpl statement = tableImplementor.getStatement();
            Predicate filterPredicate;
            if (tableImplementor.isInverse()) {
                renderInverseJoin(builder, mode);
                filterPredicate = statement.getFilterPredicate(tableImplementor, builder.getAstContext());
            } else if (tableImplementor.getJoinProp() != null || tableImplementor.getWeakJoinHandle() != null) {
                renderJoin(builder, mode);
                filterPredicate = statement.getFilterPredicate(tableImplementor, builder.getAstContext());
            } else {
                builder
                        .from()
                        .sql(tableImplementor.getImmutableType().getTableName(builder.getAstContext().getSqlClient().getMetadataStrategy()))
                        .sql(" ")
                        .sql(alias);
                filterPredicate = null;
            }
            if (filterPredicate != null) {
                builder.sql(" and ");
                ((Ast) filterPredicate).renderTo(builder);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void renderJoin(SqlBuilder builder, TableImplementor.RenderMode mode) {

        if (!(owner instanceof TableImplementor<?>)) {
            return;
        }
        TableImpl<?> owner = (TableImpl<?>) this.owner;
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

        TableImpl<?> owner = (TableImpl<?>)this.owner;
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
        TableImpl<?> owner = (TableImpl<?>) this.owner;
        if (builder.getAstContext().getTableUsedState(this) != TableUsedState.NONE) {
            ImmutableType immutableType = owner.getImmutableType();
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
        TableImpl<?> owner = (TableImpl<?>) this.owner;
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
        TableImpl<?> owner = (TableImpl<?>) this.owner;
        BaseSelectionMapper mapper =
                builder instanceof SqlBuilder ?
                        ((SqlBuilder)builder).getAstContext().getBaseSelectionMapper(owner.getBaseTableOwner()) :
                null;
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
                        builder.definition(
                                withPrefix ? middleTableAlias : null,
                                middleTable.getColumnDefinition(),
                                asBlock,
                                mapper
                        );
                    } else {
                        builder.definition(
                                withPrefix ? middleTableAlias : null,
                                middleTable.getTargetColumnDefinition(),
                                asBlock,
                                mapper
                        );
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
                    builder.definition(
                            withPrefix ? parent.alias : null,
                            joinProp.getStorage(strategy),
                            asBlock,
                            mapper
                    );
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
            builder.definition(
                    withPrefix ? alias : null,
                    definition,
                    asBlock,
                    mapper
            );
        }
    }

    @Override
    public String toString() {
        return "RealTable{" +
                "owner=" + owner +
                ", key=" + key +
                ", joinType=" + joinType +
                '}';
    }

    @Override
    public final void allocateAliases() {
        TableLikeImplementor<?> owner = this.owner;
        if (alias == null) {
            AbstractMutableStatementImpl statement = owner.getStatement();
            StatementContext ctx = statement.getContext();
            ImmutableProp joinProp = owner.getJoinProp();
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

        for (RealTable childTable : this) {
            childTable.allocateAliases();
        }
    }
}
