package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.association.meta.AssociationProp;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryExportSelection;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.query.TableUsageVisitor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
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

    private JoinType joinType;

    private Aliases aliases;

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
        if (owner instanceof BaseTableImplementor) {
            BaseTableImplementor baseTableImpl = (BaseTableImplementor) owner;
            this.joinType = baseTableImpl.getJoinType();
            if (owner.getWeakJoinHandle() != null) {
                TableLikeImplementor<?> sourceImplementor = baseTableImpl.getParent();
                TableLike<?> sourceLike = sourceImplementor instanceof BaseTableImplementor ?
                        ((BaseTableImplementor) sourceImplementor).toSymbol() :
                        TableUtils.disableJoin(
                                TableProxies.wrap((Table<?>) sourceImplementor),
                                "For the weak join operation from a " +
                                        "regular table to a base table, the strong join " +
                                        "is not allowed on the regular table side (source side)."
                        );
                joinPredicate = owner.getWeakJoinHandle().createPredicate(
                        sourceLike,
                        baseTableImpl.toSymbol(),
                        owner.getStatement()
                );
            } else {
                joinPredicate = null;
            }
        } else {
            TableImpl<?> tableImpl = (TableImpl<?>) owner;
            this.joinType = tableImpl.getJoinType();
            if (owner.getWeakJoinHandle() != null) {
                joinPredicate = owner.getWeakJoinHandle().createPredicate(
                        owner.getParent(),
                        owner,
                        owner.getStatement()
                );
            } else {
                joinPredicate = null;
            }
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
    public Key getKey() {
        return key;
    }

    @Override
    public RealTable child(Key key) {
        RealTableImpl child = (RealTableImpl) getValue(key);
        if (child != null) {
            if (child.joinType != owner.getJoinType()) {
                child.joinType = JoinType.INNER;
            }
            return child;
        }
        child = new RealTableImpl(key, owner, this);
        putValue(key, child, RealTableImpl::lessThan);
        return child;
    }

    public RealTableImpl child(JoinTypeMergeScope scope, TableImpl<?> owner) {
        return child(scope, owner, null);
    }

    public RealTableImpl child(
            JoinTypeMergeScope scope,
            TableImpl<?> owner,
            @Nullable JoinType requiredJoinType
    ) {
        Key key = new Key(scope, owner.isInverse, owner.joinProp, owner.weakJoinHandle);
        JoinType joinType = requiredJoinType != null ? requiredJoinType : owner.getJoinType();
        RealTableImpl child = (RealTableImpl) getValue(key);
        if (child != null) {
            if (child.joinType != joinType) {
                child.joinType = JoinType.INNER;
            }
            return child;
        }
        child = new RealTableImpl(key, owner, this);
        child.joinType = joinType;
        putValue(key, child, RealTableImpl::lessThan);
        return child;
    }

    public RealTableImpl child(JoinTypeMergeScope scope, BaseTableImplementor owner) {
        Key key = new Key(scope, false, null, owner.getWeakJoinHandle());
        RealTableImpl child = (RealTableImpl) getValue(key);
        if (child != null) {
            if (child.joinType != owner.getJoinType()) {
                child.joinType = JoinType.INNER;
            }
            return child;
        }
        child = new RealTableImpl(key, owner, this);
        putValue(key, child, RealTableImpl::lessThan);
        return child;
    }

    private static boolean lessThan(RealTable a, RealTable b) {
        return ((RealTableImpl)a).owner.getOrder() < ((RealTableImpl)b).owner.getOrder();
    }

    @Override
    public final String getAlias() {
        return aliases().value;
    }

    @Override
    public final String getMiddleTableAlias() {
        return aliases().middleValue;
    }

    @Override
    public String getFinalAlias(
            ImmutableProp prop,
            boolean rawId,
            JSqlClientImplementor sqlClient
    ) {
        if (!(owner instanceof TableImpl<?>)) {
            return aliases().value;
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
                return aliases().middleValue;
            }
            if (!isInverse) {
                return parent.aliases().value;
            }
        }
        return aliases().value;
    }

    @Override
    @Nullable
    public BaseTableOwner getBaseTableOwner() {
        if (owner instanceof TableImplementor<?>) {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) owner;
            return tableImplementor.getBaseTableOwner();
        }
        return null;
    }

    @Override
    public void use(TableUsageVisitor visitor) {
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
        if (mode == TableImplementor.RenderMode.NORMAL) {
            throw new IllegalStateException("Internal bug: renderJoinAsFrom does not accept render mode ALL");
        }
        TableUsedState usedState = builder.getAstContext().getTableUsedState(this);
        if (usedState != TableUsedState.NONE) {
            if (mode == TableImplementor.RenderMode.FROM_ONLY || mode == TableImplementor.RenderMode.WHERE_ONLY) {
                builder.separator();
            }
            renderSelf(builder, mode, false);
            if (mode == TableImplementor.RenderMode.DEEPER_JOIN_ONLY) {
                for (RealTable childTable : this) {
                    childTable.renderTo(builder, false);
                }
            }
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder, boolean cte) {
        TableLikeImplementor<?> owner = this.owner;
        SqlBuilder sqlBuilder = builder.assertSimple();
        TableUsedState usedState = sqlBuilder.getAstContext().getTableUsedState(this);
        if (owner.getParent() == null || usedState != TableUsedState.NONE) {
            if (owner instanceof BaseTableImplementor) {
                AstContext astContext = builder.assertSimple().getAstContext();
                astContext.pushRenderedBaseTable(this);
                renderSelf(sqlBuilder, TableImplementor.RenderMode.NORMAL, cte);
                astContext.popRenderedBaseTable();
            } else {
                renderSelf(sqlBuilder, TableImplementor.RenderMode.NORMAL, cte);
            }
            if (!cte) {
                for (RealTable childTable : this) {
                    BaseTableOwner childOwner = childTable.getBaseTableOwner();
                    if (childOwner != null && childOwner.getBaseTable().isRecursiveCte()) {
                        continue;
                    }
                    childTable.renderTo(sqlBuilder, false);
                }
            }
        }
    }

    private void renderSelf(SqlBuilder builder, TableImplementor.RenderMode mode, boolean cte) {
        TableLikeImplementor<?> owner = this.owner;
        if (owner instanceof BaseTableImplementor) {
            if (!cte && parent != null) {
                builder.join(joinType);
            }
            renderBaseTableCore(builder, cte);
        } else if (owner instanceof TableImplementor<?>) {
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
                        .sql(aliases().value);
                filterPredicate = null;
            }
            if (filterPredicate != null) {
                builder.sql(" and ");
                ((Ast) filterPredicate).renderTo(builder);
            }
        }
    }

    private void renderBaseTableCore(SqlBuilder builder, boolean cte) {
        AstContext ctx = builder.getAstContext();
        BaseTableImpl baseTableImpl = (BaseTableImpl) owner;
        boolean aliasOnly = !cte && ((BaseTableImplementor) owner).isCte();
        boolean withScope = !cte && parent != null &&
                parent.owner instanceof TableImplementor<?> &&
                ((BaseTableImplementor)owner).getRecursive() == null;
        if (withScope) {
            builder.enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
        }
        if (aliasOnly) {
            builder.sql(aliases().value);
        } else {
            builder.enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
            baseTableImpl.renderBaseQueryCore(builder);
            builder.leave();
            if (!baseTableImpl.isCte()) {
                builder.sql(" ").sql(aliases().value);
            }
        }
        for (Selection<?> selection : baseTableImpl.getSelections()) {
            if (!(selection instanceof Table<?>)) {
                continue;
            }
            TableImplementor<?> tableImplementor = TableProxies.resolve((Table<?>) selection, ctx);
            BaseTableOwner baseTableOwner = tableImplementor.getBaseTableOwner();
            if (baseTableOwner == null || baseTableOwner.getBaseTable() != baseTableImpl.toSymbol()) {
                continue;
            }
            RealTable realTable = tableImplementor.realTable(ctx);
            if (!cte) {
                ctx.pushRenderedBaseTable(null);
                for (RealTable childTable : realTable) {
                    childTable.renderTo(builder, false);
                }
                ctx.popRenderedBaseTable();
            }
        }
        if (withScope) {
            builder.leave();
        }
        if (owner.getParent() == null || cte) {
            return;
        }
        ctx.pushRenderedBaseTable(null);
        builder.on();
        if (joinPredicate == null) {
            builder.sql("1 = 1");
        } else {
            ((Ast)joinPredicate).renderTo(builder);
        }
        ctx.popRenderedBaseTable();
    }

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
                        .sql(aliases().value)
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
                        parent.aliases().value,
                        parent,
                        joinProp.getStorage(strategy),
                        immutableType.getTableName(strategy),
                        aliases().value,
                        immutableType.getIdProp().getStorage(strategy),
                        mode
                );
                renderMiddleTableFilters(
                        ((AssociationProp)joinProp).getDeclaringType().getMiddleTable(strategy),
                        parent.aliases().value,
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
                    parent.aliases().value,
                    parent,
                    owner.parent.immutableType.getIdProp().getStorage(strategy),
                    middleTable.getTableName(),
                    aliases().middleValue,
                    middleTable.getColumnDefinition(),
                    mode
            );
            renderMiddleTableFilters(
                    middleTable,
                    aliases().middleValue,
                    builder
            );
            if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED && (
                    mode == TableImplementor.RenderMode.NORMAL ||
                            mode == TableImplementor.RenderMode.DEEPER_JOIN_ONLY)
            ) {
                renderJoinImpl(
                        builder,
                        joinType,
                        aliases().middleValue,
                        this,
                        middleTable.getTargetColumnDefinition(),
                        immutableType.getTableName(strategy),
                        aliases().value,
                        immutableType.getIdProp().getStorage(strategy),
                        TableImplementor.RenderMode.NORMAL
                );
            }
        } else if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED) {
            renderJoinImpl(
                    builder,
                    joinType,
                    parent.aliases().value,
                    parent,
                    joinProp.getStorage(strategy),
                    immutableType.getTableName(strategy),
                    aliases().value,
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
                    parent.aliases().value,
                    parent,
                    owner.parent.immutableType.getIdProp().getStorage(strategy),
                    middleTable.getTableName(),
                    aliases().middleValue,
                    middleTable.getTargetColumnDefinition(),
                    mode
            );
            renderMiddleTableFilters(
                    middleTable,
                    aliases().middleValue,
                    builder
            );
            if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED && (
                    mode == TableImplementor.RenderMode.NORMAL ||
                            mode == TableImplementor.RenderMode.DEEPER_JOIN_ONLY)
            ) {
                renderJoinImpl(
                        builder,
                        joinType,
                        aliases().middleValue,
                        this,
                        middleTable.getColumnDefinition(),
                        immutableType.getTableName(strategy),
                        aliases().value,
                        immutableType.getIdProp().getStorage(strategy),
                        TableImplementor.RenderMode.NORMAL
                );
            }
        } else { // One-to-many join cannot be optimized by "used"
            renderJoinImpl(
                    builder,
                    joinType,
                    parent.aliases().value,
                    parent,
                    owner.parent.immutableType.getIdProp().getStorage(strategy),
                    immutableType.getTableName(strategy),
                    aliases().value,
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
                            .sql(aliases().value)
                            .on();
                    break;
                case FROM_ONLY:
                    builder
                            .sql(immutableType.getTableName(strategy))
                            .sql(" ")
                            .sql(aliases().value);
                    break;
            }
            if (mode == TableImplementor.RenderMode.NORMAL || mode == TableImplementor.RenderMode.WHERE_ONLY) {
                if (owner.isInverse) {
                    builder.sql(joinTemplate.toSql(aliases().value, parent.aliases().value));
                } else {
                    builder.sql(joinTemplate.toSql(parent.aliases().value, aliases().value));
                }
            }
        }
    }

    private void renderJoinImpl(
            SqlBuilder builder,
            JoinType joinType,
            String previousAlias,
            RealTable previousTable,
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
            BaseQueryExportSelection exportSelection = null;
            if (owner instanceof TableImplementor<?>) {
                TableImplementor<?> tableImplementor = (TableImplementor<?>) owner;
                BaseTableOwner baseTableOwner = tableImplementor.getBaseTableOwner();
                if (baseTableOwner != null && builder.getQueryRenderContext() != null) {
                    exportSelection = builder.getQueryRenderContext().getBaseQueryExportSelection(baseTableOwner);
                }
            }
            int size = previousDefinition.size();
            builder.enter(SqlBuilder.ScopeType.AND);
            for (int i = 0; i < size; i++) {
                builder.separator();
                if (exportSelection != null) {
                    int index = exportSelection.columnIndex(previousTable, previousDefinition.name(i), false);
                    builder
                            .sql(exportSelection.getAlias())
                            .sql(".c")
                            .sql(Integer.toString(index));
                } else {
                    builder
                            .sql(previousAlias)
                            .sql(".")
                            .sql(previousDefinition.name(i));
                }
                builder
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
            String middleAlias,
            SqlBuilder builder
    ) {
        TableImpl<?> owner = (TableImpl<?>) this.owner;
        ImmutableProp joinProp = owner.joinProp;
        LogicalDeletedInfo deletedInfo = middleTable.getLogicalDeletedInfo();
        JSqlClient sqlClient = builder.getAstContext().getSqlClient();
        if (deletedInfo != null &&
                sqlClient.getFilters().getBehavior(joinProp) != LogicalDeletedBehavior.IGNORED) {
            builder.sql(" and ");
            JoinTableFilters.render(sqlClient.getFilters().getBehavior(joinProp), deletedInfo, middleAlias, builder);
        }
        JoinTableFilterInfo filterInfo = middleTable.getFilterInfo();
        if (filterInfo != null) {
            builder.sql(" and ");
            JoinTableFilters.render(filterInfo, middleAlias, builder);
        }
    }

    public void renderSelection(
            ImmutableProp prop,
            boolean rawId,
            AbstractSqlBuilder<?> builder,
            ColumnDefinition optionalDefinition,
            boolean withPrefix,
            Function<Integer, String> asBlock,
            boolean idViewAllowed
    ) {
        TableImpl<?> owner = (TableImpl<?>) this.owner;
        BaseQueryExportSelection exportSelection =
                builder instanceof SqlBuilder && ((SqlBuilder) builder).getQueryRenderContext() != null ?
                        ((SqlBuilder)builder).getQueryRenderContext().getBaseQueryExportSelection(owner.getBaseTableOwner()) :
                        null;
        ImmutableProp joinProp = owner.joinProp;
        MetadataStrategy strategy = builder.sqlClient().getMetadataStrategy();
        if (exportSelection != null &&
                !exportSelection.isRootTable(this) &&
                !(prop.isId() &&
                        joinProp != null &&
                        !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                        (rawId || idViewAllowed && TableUtils.isRawIdAllowed(owner, builder.sqlClient())) &&
                        !owner.isInverse &&
                        parent != null &&
                        exportSelection.containsTable(parent))) {
            exportSelection = null;
        }
        if (prop.isId() && joinProp != null && !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                (rawId || idViewAllowed && TableUtils.isRawIdAllowed(owner, builder.sqlClient()))) {
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
                                withPrefix ? aliases().middleValue : null,
                                middleTable.getColumnDefinition(),
                                false,
                                asBlock,
                                null
                        );
                    } else {
                        builder.definition(
                                withPrefix ? aliases().middleValue : null,
                                middleTable.getTargetColumnDefinition(),
                                false,
                                asBlock,
                                null
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
                            builder.sql(aliases().middleValue).sql(".");
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
                            withPrefix ? parent : null,
                            joinProp.getStorage(strategy),
                            true,
                            asBlock,
                            exportSelection
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
                            builder.sql(parent.aliases().value).sql(".");
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
            builder.sql(((FormulaTemplate)template).toSql(aliases().value));
            if (asBlock != null) {
                builder.sql(" ").sql(asBlock.apply(0));
            }
        } else {
            ColumnDefinition definition = optionalDefinition != null ?
                    optionalDefinition :
                    prop.getStorage(strategy);
            builder.definition(
                    withPrefix ? this : null,
                    definition,
                    false,
                    asBlock,
                    exportSelection
            );
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RealTable{")
                .append("identity=").append(System.identityHashCode(this))
                .append(", owner=").append(owner);
        if (!"".equals(key.joinName) && key.weakJoinHandle != null) {
            builder.append(", key=").append(key);
        }
        if (joinType != JoinType.INNER) {
            builder.append(", joinType=").append(joinType);
        }
        builder.append('}');
        return builder.toString();
    }

    @Override
    public final void applyAliasesIfNecessary(TableAliasScope scope) {
        if (aliases != null && aliases.scope == scope) {
            return;
        }
        if (parent != null) {
            parent.applyAliasesIfNecessary(scope);
        }
        if (aliases == null || aliases.scope != scope) {
            applyAlias(scope);
        }
    }

    @Override
    public final void applyAliases(TableAliases tableAliases, TableAliasScope scope) {
        TableAliases.Alias alias = tableAliases.get(this);
        if (alias != null) {
            scope.reserveTableAlias(alias.value);
            scope.reserveTableAlias(alias.middleValue);
            this.aliases = new Aliases(alias.value, alias.middleValue, scope);
        } else if (aliases != null && aliases.scope != scope) {
            this.aliases = null;
        }
        for (RealTable childTable : this) {
            childTable.applyAliases(tableAliases, scope);
        }
    }

    private void applyAlias(TableAliasScope scope) {
        TableLikeImplementor<?> owner = this.owner;
        if (owner instanceof BaseTableImplementor) {
            BaseTableImplementor baseTableImplementor = (BaseTableImplementor) owner;
            BaseTableImplementor recursive = baseTableImplementor.getRecursive();
            if (recursive != null) {
                RealTableImpl recursiveTable = (RealTableImpl) recursive.realTable(key.scope);
                recursiveTable.applyAliasesIfNecessary(scope);
                this.aliases = recursiveTable.aliases;
                return;
            }
        }
        ImmutableProp joinProp = owner instanceof TableImplementor<?> ?
                ((TableImplementor<?>)owner).getJoinProp() :
                null;
        String middleAlias;
        if (joinProp != null && joinProp.isMiddleTableDefinition()) {
            middleAlias = scope.allocateTableAlias(owner);
        } else {
            middleAlias = null;
        }
        this.aliases = new Aliases(scope.allocateTableAlias(owner), middleAlias, scope);
    }

    private Aliases aliases() {
        Aliases aliases = this.aliases;
        if (aliases == null) {
            if (parent != null) {
                applyAliasesIfNecessary(parent.aliases().scope);
                aliases = this.aliases;
                if (aliases != null) {
                    return aliases;
                }
            }
            throw new IllegalStateException(
                    "Table aliases have not been allocated for " + this
            );
        }
        return aliases;
    }

    private static class Aliases {

        final String value;

        final String middleValue;

        final TableAliasScope scope;

        Aliases(String value, String middleValue, TableAliasScope scope) {
            this.value = value;
            this.middleValue = middleValue;
            this.scope = scope;
        }

        @Override
        public String toString() {
            return "Aliases{" +
                    "value='" + value + '\'' +
                    ", middleValue='" + middleValue + '\'' +
                    '}';
        }
    }
}
