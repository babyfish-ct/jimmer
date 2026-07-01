package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.association.meta.AssociationProp;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryRead;
import org.babyfish.jimmer.sql.ast.impl.base.BaseQueryReadSupport;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.query.QueryRenderContext;
import org.babyfish.jimmer.sql.ast.impl.query.TableUsageVisitor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.meta.*;
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

    private final TableAliasKey aliasKey;

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
        this.aliasKey = TableAliasKey.create(this);
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
        Key key = new Key(
                scope,
                owner.isInverse,
                owner.joinProp,
                owner.weakJoinHandle,
                owner.isTreated() ? owner.immutableType : null
        );
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

    @Override
    public boolean isOptimizableBridgeTo(RealTable child, AstContext ctx) {
        if (parent == null ||
                !(owner instanceof TableImplementor<?>) ||
                !(child.getTableLikeImplementor() instanceof TableImplementor<?>)) {
            return false;
        }
        TableImplementor<?> bridgeImplementor = (TableImplementor<?>) owner;
        TableImplementor<?> childImplementor = (TableImplementor<?>) child.getTableLikeImplementor();
        ImmutableProp bridgeProp = bridgeImplementor.getJoinProp();
        ImmutableProp childProp = childImplementor.getJoinProp();
        return bridgeImplementor.getWeakJoinHandle() == null &&
                childImplementor.getWeakJoinHandle() == null &&
                !bridgeImplementor.isInverse() &&
                childImplementor.isInverse() &&
                bridgeImplementor.getJoinType() == JoinType.INNER &&
                childImplementor.getJoinType() == JoinType.INNER &&
                bridgeProp != null &&
                bridgeProp.isMappedId() &&
                childProp == bridgeProp &&
                bridgeImplementor.getStatement().getFilterPredicate(bridgeImplementor, ctx) == null;
    }

    @Override
    public boolean isMappedIdColumnSource(AstContext ctx) {
        if (parent == null || !(owner instanceof TableImpl<?>)) {
            return false;
        }
        TableImpl<?> tableOwner = (TableImpl<?>) owner;
        ImmutableProp joinProp = tableOwner.joinProp;
        return joinProp != null &&
                joinProp.isMappedId() &&
                !tableOwner.isInverse &&
                tableOwner.weakJoinHandle == null &&
                !joinProp.isMiddleTableDefinition() &&
                !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                tableOwner.getStatement().getFilterPredicate(tableOwner, ctx) == null;
    }

    @Override
    public TableAliasKey getAliasKey() {
        return aliasKey;
    }

    private static boolean lessThan(RealTable a, RealTable b) {
        return ((RealTableImpl) a).owner.getOrder() < ((RealTableImpl) b).owner.getOrder();
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
            ((Ast) joinPredicate).accept(visitor);
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
            if (tableImplementor.isTreated()) {
                renderTreatedJoin(builder, tableImplementor, mode);
                filterPredicate = null;
            } else if (tableImplementor.isInverse()) {
                renderInverseJoin(builder, mode);
                filterPredicate = statement.getFilterPredicate(tableImplementor, builder.getAstContext());
            } else if (tableImplementor.getJoinProp() != null || tableImplementor.getWeakJoinHandle() != null) {
                renderJoin(builder, mode);
                filterPredicate = statement.getFilterPredicate(tableImplementor, builder.getAstContext());
            } else if (tableImplementor.isJoinedTypeBranchRoot()) {
                renderJoinedTypeBranchRoot(builder, tableImplementor, mode);
                filterPredicate = null;
            } else {
                builder
                        .from()
                        .sql(tableImplementor.getImmutableType().getTableName(builder.getAstContext().getSqlClient().getMetadataStrategy()))
                        .sql(" ")
                        .sqlAlias(this);
                filterPredicate = null;
            }
            if (filterPredicate != null) {
                builder.sql(" and ");
                ((Ast) filterPredicate).renderTo(builder);
            }
        }
    }

    private void renderJoinedTypeBranchRoot(
            SqlBuilder builder,
            TableImplementor<?> tableImplementor,
            TableImplementor.RenderMode mode
    ) {
        if (mode != TableImplementor.RenderMode.NORMAL && mode != TableImplementor.RenderMode.FROM_ONLY) {
            throw new AssertionError("Internal bug: Illegal render mode for joined type branch root table");
        }
        MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();
        ImmutableType type = tableImplementor.getImmutableType();
        ImmutableType rootType = type.getInheritanceInfo().getRootType();
        String rootAlias = builder.alias(this);
        if (mode == TableImplementor.RenderMode.NORMAL) {
            builder
                    .from()
                    .sql(rootType.getTableName(strategy))
                    .sql(" ")
                    .sql(rootAlias);
        } else {
            builder
                    .sql(rootType.getTableName(strategy))
                    .sql(" ")
                    .sql(rootAlias);
        }
        QueryRenderContext queryRenderContext = builder.getQueryRenderContext();
        if (queryRenderContext != null && !queryRenderContext.isJoinedTypeBranchTableRequired(tableImplementor)) {
            return;
        }
        renderJoinImpl(
                builder,
                JoinType.INNER,
                rootAlias,
                this,
                rootType.getIdProp().getStorage(strategy),
                type.getTableName(strategy),
                joinedTypeBranchAlias(builder),
                type.getIdProp().getStorage(strategy),
                TableImplementor.RenderMode.NORMAL
        );
    }

    private void renderTreatedJoin(
            SqlBuilder builder,
            TableImplementor<?> tableImplementor,
            TableImplementor.RenderMode mode
    ) {
        if (mode != TableImplementor.RenderMode.NORMAL &&
                mode != TableImplementor.RenderMode.FROM_ONLY &&
                mode != TableImplementor.RenderMode.WHERE_ONLY) {
            throw new AssertionError("Internal bug: Illegal render mode for treated table");
        }
        MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();
        ImmutableType type = tableImplementor.getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        ImmutableType rootType = inheritanceInfo.getRootType();
        TableImplementor<?> parentImplementor = tableImplementor.getParent();
        String alias = builder.alias(this);
        if (inheritanceInfo.getStrategy() == InheritanceType.JOINED) {
            renderJoinImpl(
                    builder,
                    joinType,
                    builder.alias(parent),
                    parent,
                    parentImplementor.getImmutableType().getIdProp().getStorage(strategy),
                    type.getTableName(strategy),
                    alias,
                    type.getIdProp().getStorage(strategy),
                    mode
            );
            if (mode == TableImplementor.RenderMode.NORMAL || mode == TableImplementor.RenderMode.WHERE_ONLY) {
                builder.sql(" and ");
                renderTreatedDiscriminator(builder, inheritanceInfo, type);
            }
            return;
        }
        renderJoinImpl(
                builder,
                joinType,
                builder.alias(parent),
                parent,
                parentImplementor.getImmutableType().getIdProp().getStorage(strategy),
                rootType.getTableName(strategy),
                alias,
                rootType.getIdProp().getStorage(strategy),
                mode
        );
        if (mode == TableImplementor.RenderMode.NORMAL || mode == TableImplementor.RenderMode.WHERE_ONLY) {
            builder.sql(" and ");
            ((Ast) tableImplementor.discriminatorPredicate(type)).renderTo(builder);
        }
    }

    private void renderTreatedDiscriminator(
            SqlBuilder builder,
            InheritanceInfo inheritanceInfo,
            ImmutableType treatedType
    ) {
        ImmutableProp discriminatorProp = inheritanceInfo.getDiscriminatorProp();
        DiscriminatorPredicate.render(
                builder,
                inheritanceRootTable(inheritanceInfo),
                ((SingleColumn) discriminatorProp.getStorage(
                        builder.getAstContext().getSqlClient().getMetadataStrategy()
                )).getName(),
                discriminatorProp,
                DiscriminatorPredicate.values(inheritanceInfo, treatedType)
        );
    }

    private RealTable inheritanceRootTable(InheritanceInfo inheritanceInfo) {
        ImmutableType rootType = inheritanceInfo.getRootType();
        RealTableImpl table = this;
        while (table != null) {
            if (table.owner instanceof TableImplementor<?> &&
                    ((TableImplementor<?>) table.owner).getImmutableType() == rootType) {
                return table;
            }
            table = table.parent;
        }
        throw new AssertionError(
                "Internal bug: Cannot find inheritance root table \"" +
                        rootType +
                        "\" for treated table \"" +
                        owner +
                        "\""
        );
    }

    private String joinedTypeBranchAlias(SqlBuilder builder) {
        return TableImplementor.joinedTypeBranchAlias(builder, (TableImplementor<?>) owner);
    }

    private void renderBaseTableCore(SqlBuilder builder, boolean cte) {
        AstContext ctx = builder.getAstContext();
        BaseTableImpl baseTableImpl = (BaseTableImpl) owner;
        boolean aliasOnly = !cte && ((BaseTableImplementor) owner).isCte();
        boolean withScope = !cte && parent != null &&
                parent.owner instanceof TableImplementor<?> &&
                ((BaseTableImplementor) owner).getRecursive() == null;
        if (withScope) {
            builder.enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
        }
        if (aliasOnly) {
            builder.sqlAlias(this);
        } else {
            builder.enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
            baseTableImpl.renderBaseQueryCore(builder);
            builder.leave();
            if (!baseTableImpl.isCte()) {
                builder.sql(" ").sqlAlias(this);
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
            ((Ast) joinPredicate).renderTo(builder);
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
                        .sqlAlias(this)
                        .on();
                if (joinPredicate == null) {
                    builder.sql("1 = 1");
                } else {
                    ((Ast) joinPredicate).renderTo(builder);
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
                        parentAlias(builder, joinProp),
                        parent,
                        joinProp.getStorage(strategy),
                        immutableType.getTableName(strategy),
                        builder.alias(this),
                        immutableType.getIdProp().getStorage(strategy),
                        mode
                );
                renderMiddleTableFilters(
                        ((AssociationProp) joinProp).getDeclaringType().getMiddleTable(strategy),
                        builder.alias(parent),
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
                    parentAlias(builder, joinProp),
                    parent,
                    owner.parent.immutableType.getIdProp().getStorage(strategy),
                    middleTable.getTableName(),
                    builder.middleTableAlias(this),
                    middleTable.getColumnDefinition(),
                    mode
            );
            renderMiddleTableFilters(
                    middleTable,
                    builder.middleTableAlias(this),
                    builder
            );
            if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED && (
                    mode == TableImplementor.RenderMode.NORMAL ||
                            mode == TableImplementor.RenderMode.DEEPER_JOIN_ONLY)
            ) {
                renderJoinImpl(
                        builder,
                        joinType,
                        builder.middleTableAlias(this),
                        this,
                        middleTable.getTargetColumnDefinition(),
                        immutableType.getTableName(strategy),
                        builder.alias(this),
                        immutableType.getIdProp().getStorage(strategy),
                        TableImplementor.RenderMode.NORMAL
                );
            }
        } else if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED) {
            renderJoinImpl(
                    builder,
                    joinType,
                    parentAlias(builder, joinProp),
                    parent,
                    joinProp.getStorage(strategy),
                    immutableType.getTableName(strategy),
                    builder.alias(this),
                    immutableType.getIdProp().getStorage(strategy),
                    mode
            );
        }
    }

    private void renderInverseJoin(SqlBuilder builder, TableImplementor.RenderMode mode) {

        TableImpl<?> owner = (TableImpl<?>) this.owner;
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
                    builder.alias(parent),
                    parent,
                    owner.parent.immutableType.getIdProp().getStorage(strategy),
                    middleTable.getTableName(),
                    builder.middleTableAlias(this),
                    middleTable.getTargetColumnDefinition(),
                    mode
            );
            renderMiddleTableFilters(
                    middleTable,
                    builder.middleTableAlias(this),
                    builder
            );
            if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED && (
                    mode == TableImplementor.RenderMode.NORMAL ||
                            mode == TableImplementor.RenderMode.DEEPER_JOIN_ONLY)
            ) {
                renderJoinImpl(
                        builder,
                        joinType,
                        builder.middleTableAlias(this),
                        this,
                        middleTable.getColumnDefinition(),
                        immutableType.getTableName(strategy),
                        builder.alias(this),
                        immutableType.getIdProp().getStorage(strategy),
                        TableImplementor.RenderMode.NORMAL
                );
            }
        } else if (isMappedIdBridgeJoin(builder)) {
            RealTableImpl bridge = parent;
            TableImpl<?> bridgeOwner = (TableImpl<?>) bridge.owner;
            renderJoinImpl(
                    builder,
                    joinType,
                    builder.alias(bridge.parent),
                    bridge.parent,
                    bridgeOwner.joinProp.getStorage(strategy),
                    immutableType.getTableName(strategy),
                    builder.alias(this),
                    joinProp.getStorage(strategy),
                    mode
            );
        } else { // One-to-many join cannot be optimized by "used"
            renderJoinImpl(
                    builder,
                    joinType,
                    builder.alias(parent),
                    parent,
                    owner.parent.immutableType.getIdProp().getStorage(strategy),
                    immutableType.getTableName(strategy),
                    builder.alias(this),
                    joinProp.getStorage(strategy),
                    mode
            );
        }
    }

    private boolean isMappedIdBridgeJoin(SqlBuilder builder) {
        if (parent == null || parent.parent == null) {
            return false;
        }
        if (builder.getAstContext().getTableUsedState(parent) != TableUsedState.ID_ONLY) {
            return false;
        }
        return parent.isOptimizableBridgeTo(this, builder.getAstContext());
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
                            .sqlAlias(this)
                            .on();
                    break;
                case FROM_ONLY:
                    builder
                            .sql(immutableType.getTableName(strategy))
                            .sql(" ")
                            .sqlAlias(this);
                    break;
            }
            if (mode == TableImplementor.RenderMode.NORMAL || mode == TableImplementor.RenderMode.WHERE_ONLY) {
                if (owner.isInverse) {
                    builder.sql(joinTemplate.toSql(builder.alias(this), builder.alias(parent)));
                } else {
                    builder.sql(joinTemplate.toSql(parentAlias(builder, owner.joinProp), builder.alias(this)));
                }
            }
        }
    }

    private String parentAlias(SqlBuilder builder, @Nullable ImmutableProp joinProp) {
        if (parent != null && parent.owner instanceof TableImplementor<?> && joinProp != null) {
            TableImplementor<?> parentImplementor = (TableImplementor<?>) parent.owner;
            String rootAlias = builder.getAstContext().getJoinedTypeBranchUpdateRootAlias(parentImplementor);
            if (rootAlias != null && parentImplementor.isRootTableProp(joinProp)) {
                return rootAlias;
            }
        }
        return builder.alias(parent);
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
            BaseQueryReadSupport readSupport = null;
            BaseTableOwner baseTableOwner = null;
            QueryRenderContext queryRenderContext = builder.getQueryRenderContext();
            if (owner instanceof TableImplementor<?>) {
                TableImplementor<?> tableImplementor = (TableImplementor<?>) owner;
                baseTableOwner = tableImplementor.getBaseTableOwner();
                if (baseTableOwner != null && queryRenderContext != null) {
                    readSupport = queryRenderContext.getBaseQueryReadSupport();
                }
            }
            int size = previousDefinition.size();
            builder.enter(SqlBuilder.ScopeType.AND);
            for (int i = 0; i < size; i++) {
                builder.separator();
                BaseQueryRead read = readSupport != null ?
                        readSupport.column(baseTableOwner, previousTable, previousDefinition.name(i), false) :
                        null;
                if (read != null) {
                    renderBaseQueryRead(builder, read, 0);
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
        QueryRenderContext queryRenderContext = builder.getQueryRenderContext();
        BaseTableOwner baseTableOwner = owner.getBaseTableOwner();
        BaseQueryReadSupport readSupport =
                queryRenderContext != null &&
                        queryRenderContext.getBaseQueryReadSupport().canReadSelection(
                                baseTableOwner,
                                this,
                                prop,
                                owner.joinProp,
                                rawId,
                                idViewAllowed,
                                builder.sqlClient()
                        ) ?
                        queryRenderContext.getBaseQueryReadSupport() :
                        null;
        ImmutableProp joinProp = owner.joinProp;
        MetadataStrategy strategy = builder.sqlClient().getMetadataStrategy();
        if (joinProp == null && owner.isJoinedTypeBranchRoot()) {
            renderJoinedTypeBranchSelection(
                    prop,
                    builder,
                    optionalDefinition,
                    withPrefix,
                    asBlock,
                    strategy
            );
            return;
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
                    String middleAlias = withPrefix ? builder.assertSimple().middleTableAlias(this) : null;
                    if (isInverse) {
                        builder.definition(
                                middleAlias,
                                middleTable.getColumnDefinition(),
                                asBlock
                        );
                    } else {
                        builder.definition(
                                middleAlias,
                                middleTable.getTargetColumnDefinition(),
                                asBlock
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
                            builder.sql(builder.assertSimple().middleTableAlias(this)).sql(".");
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
                    renderDefinition(
                            builder,
                            (RealTableImpl) parent,
                            joinProp.getStorage(strategy),
                            true,
                            withPrefix,
                            asBlock,
                            readSupport,
                            baseTableOwner
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
                        renderColumn(
                                builder,
                                (RealTableImpl) parent,
                                parentColumnName,
                                true,
                                withPrefix,
                                readSupport,
                                baseTableOwner
                        );
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
            builder.sql(((FormulaTemplate) template).toSql(builder.assertSimple().alias(this)));
            if (asBlock != null) {
                builder.sql(" ").sql(asBlock.apply(0));
            }
        } else {
            ColumnDefinition definition = optionalDefinition != null ?
                    optionalDefinition :
                    prop.getStorage(strategy);
            renderDefinition(
                    builder,
                    this,
                    definition,
                    false,
                    withPrefix,
                    asBlock,
                    readSupport,
                    baseTableOwner
            );
        }
    }

    private void renderJoinedTypeBranchSelection(
            ImmutableProp prop,
            AbstractSqlBuilder<?> builder,
            ColumnDefinition optionalDefinition,
            boolean withPrefix,
            Function<Integer, String> asBlock,
            MetadataStrategy strategy
    ) {
        TableImpl<?> owner = (TableImpl<?>) this.owner;
        boolean rootProp = owner.isRootTableProp(prop);
        SqlTemplate template = prop.getSqlTemplate();
        boolean updateTarget = builder.getAstContext().isJoinedTypeBranchUpdateTarget(owner);
        String updateRootAlias = builder.getAstContext().getJoinedTypeBranchUpdateRootAlias(owner);
        if (updateTarget && !owner.isTreated()) {
            if (rootProp && updateRootAlias == null) {
                throw new AssertionError(
                        "Internal bug: Root alias is not prepared for joined type branch update"
                );
            }
            renderJoinedTypeBranchUpdateSelection(
                    prop,
                    builder,
                    optionalDefinition,
                    withPrefix,
                    asBlock,
                    strategy,
                    rootProp,
                    updateRootAlias
            );
            return;
        }
        if (owner.isTreated()) {
            RealTableImpl sourceTable = rootProp ? (RealTableImpl) parent : this;
            if (template instanceof FormulaTemplate) {
                builder.sql(((FormulaTemplate) template).toSql(builder.assertSimple().alias(sourceTable)));
                if (asBlock != null) {
                    builder.sql(" ").sql(asBlock.apply(0));
                }
                return;
            }
            renderDefinition(
                    builder,
                    sourceTable,
                    optionalDefinition != null ? optionalDefinition : prop.getStorage(strategy),
                    false,
                    withPrefix,
                    asBlock,
                    null,
                    null
            );
            return;
        }
        if (template instanceof FormulaTemplate) {
            String alias = rootProp ?
                    builder.assertSimple().alias(this) :
                    joinedTypeBranchAlias(builder.assertSimple());
            builder.sql(((FormulaTemplate) template).toSql(alias));
            if (asBlock != null) {
                builder.sql(" ").sql(asBlock.apply(0));
            }
            return;
        }
        ColumnDefinition definition = optionalDefinition != null ?
                optionalDefinition :
                prop.getStorage(strategy);
        if (rootProp) {
            renderDefinition(
                    builder,
                    this,
                    definition,
                    false,
                    withPrefix,
                    asBlock,
                    null,
                    null
            );
        } else {
            renderJoinedTypeBranchDefinition(
                    builder,
                    definition,
                    withPrefix,
                    asBlock
            );
        }
    }

    private void renderJoinedTypeBranchUpdateSelection(
            ImmutableProp prop,
            AbstractSqlBuilder<?> builder,
            ColumnDefinition optionalDefinition,
            boolean withPrefix,
            Function<Integer, String> asBlock,
            MetadataStrategy strategy,
            boolean rootProp,
            String rootAlias
    ) {
        SqlTemplate template = prop.getSqlTemplate();
        String alias = rootProp ? rootAlias : builder.assertSimple().alias(this);
        if (template instanceof FormulaTemplate) {
            builder.sql(((FormulaTemplate) template).toSql(alias));
            if (asBlock != null) {
                builder.sql(" ").sql(asBlock.apply(0));
            }
            return;
        }
        ColumnDefinition definition = optionalDefinition != null ?
                optionalDefinition :
                prop.getStorage(strategy);
        renderDefinition(builder, alias, definition, withPrefix, asBlock);
    }

    private void renderDefinition(
            AbstractSqlBuilder<?> builder,
            String alias,
            ColumnDefinition definition,
            boolean withPrefix,
            Function<Integer, String> asBlock
    ) {
        int size = definition.size();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                builder.sql(", ");
            }
            if (withPrefix) {
                builder.sql(alias).sql(".");
            }
            builder.sql(definition.name(i));
            if (asBlock != null) {
                builder.sql(" ").sql(asBlock.apply(i));
            }
        }
    }

    private void renderJoinedTypeBranchDefinition(
            AbstractSqlBuilder<?> builder,
            ColumnDefinition definition,
            boolean withPrefix,
            Function<Integer, String> asBlock
    ) {
        int size = definition.size();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                builder.sql(", ");
            }
            if (withPrefix) {
                builder.sql(joinedTypeBranchAlias(builder.assertSimple())).sql(".");
            }
            builder.sql(definition.name(i));
            if (asBlock != null) {
                builder.sql(" ").sql(asBlock.apply(i));
            }
        }
    }

    @Override
    public void renderColumn(
            AbstractSqlBuilder<?> builder,
            String columnName,
            boolean foreignKeyInBaseQuery,
            BaseQueryReadSupport readSupport,
            BaseTableOwner baseTableOwner
    ) {
        renderColumn(builder, this, columnName, foreignKeyInBaseQuery, true, readSupport, baseTableOwner);
    }

    @Override
    public void renderDefinition(
            AbstractSqlBuilder<?> builder,
            ColumnDefinition definition,
            boolean foreignKeyInBaseQuery,
            BaseQueryReadSupport readSupport,
            BaseTableOwner baseTableOwner
    ) {
        renderDefinition(builder, this, definition, foreignKeyInBaseQuery, true, null, readSupport, baseTableOwner);
    }

    private void renderDefinition(
            AbstractSqlBuilder<?> builder,
            RealTableImpl table,
            ColumnDefinition definition,
            boolean foreignKeyInBaseQuery,
            boolean withPrefix,
            Function<Integer, String> asBlock,
            BaseQueryReadSupport readSupport,
            BaseTableOwner baseTableOwner
    ) {
        int size = definition.size();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                builder.sql(", ");
            }
            renderColumn(
                    builder,
                    table,
                    definition.name(i),
                    foreignKeyInBaseQuery,
                    withPrefix,
                    readSupport,
                    baseTableOwner
            );
            if (asBlock != null) {
                builder.sql(" ").sql(asBlock.apply(i));
            }
        }
    }

    private void renderColumn(
            AbstractSqlBuilder<?> builder,
            RealTableImpl table,
            String columnName,
            boolean foreignKeyInBaseQuery,
            boolean withPrefix,
            BaseQueryReadSupport readSupport,
            BaseTableOwner baseTableOwner
    ) {
        ColumnMapping mapping = mappedIdParentColumn(builder, table, columnName);
        if (mapping != null) {
            renderColumn(
                    builder,
                    mapping.table,
                    mapping.columnName,
                    foreignKeyInBaseQuery,
                    withPrefix,
                    readSupport,
                    baseTableOwner
            );
            return;
        }
        BaseQueryRead read = withPrefix && readSupport != null ?
                readSupport.column(baseTableOwner, table, columnName, foreignKeyInBaseQuery) :
                null;
        if (read != null) {
            renderBaseQueryRead(builder, read, 0);
        } else {
            if (withPrefix) {
                builder.sql(builder.assertSimple().alias(table)).sql(".");
            }
            builder.sql(columnName);
        }
    }

    private void renderBaseQueryRead(AbstractSqlBuilder<?> builder, BaseQueryRead read, int index) {
        builder
                .sql(builder.assertSimple().alias(read.getRealBaseTable()))
                .sql(".c")
                .sql(Integer.toString(read.index(index)));
    }

    @Nullable
    private ColumnMapping mappedIdParentColumn(
            AbstractSqlBuilder<?> builder,
            RealTableImpl table,
            String columnName
    ) {
        if (table == null ||
                table.parent == null ||
                builder.getAstContext().getTableUsedState(table) == TableUsedState.USED ||
                !table.isMappedIdColumnSource(builder.getAstContext())) {
            return null;
        }
        TableImpl<?> tableOwner = (TableImpl<?>) table.owner;
        ImmutableProp joinProp = tableOwner.joinProp;
        MetadataStrategy strategy = builder.sqlClient().getMetadataStrategy();
        ColumnDefinition idDefinition = tableOwner.immutableType.getIdProp().getStorage(strategy);
        int index = idDefinition.index(columnName);
        if (index == -1) {
            return null;
        }
        ColumnDefinition parentDefinition = joinProp.getStorage(strategy);
        return new ColumnMapping((RealTableImpl) table.parent, parentDefinition.name(index));
    }

    private static class ColumnMapping {

        final RealTableImpl table;

        final String columnName;

        ColumnMapping(RealTableImpl table, String columnName) {
            this.table = table;
            this.columnName = columnName;
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

}
