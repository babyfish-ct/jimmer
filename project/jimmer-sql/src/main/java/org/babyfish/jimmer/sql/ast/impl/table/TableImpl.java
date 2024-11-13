package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.association.meta.AssociationProp;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.DtoMetadata;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Function;

class TableImpl<E> implements TableImplementor<E> {

    private final MergedNode mergedNode;

    private final ImmutableType immutableType;

    private final TableImpl<?> parent;

    private final boolean isInverse;

    private final ImmutableProp joinProp;

    private final WeakJoinHandle weakJoinHandle;

    private final JoinType originalJoinType;

    public TableImpl(
            MergedNode mergedNode,
            ImmutableType immutableType,
            TableImpl<?> parent,
            boolean isInverse,
            ImmutableProp joinProp,
            WeakJoinHandle weakJoinHandle,
            JoinType joinType
    ) {
        if (mergedNode == null) {
            throw new AssertionError("Internal bug: Bad constructor arguments for TableImpl");
        }
        if (parent != null && immutableType instanceof AssociationType) {
            throw new AssertionError("Internal bug: Bad constructor arguments for TableImpl");
        }
        if ((parent == null) != (joinProp == null && weakJoinHandle == null)) {
            throw new AssertionError("Internal bug: Bad constructor arguments for TableImpl");
        }
        if (parent != null && (joinProp == null) == (weakJoinHandle == null)) {
            throw new AssertionError("Internal bug: Bad constructor arguments for TableImpl");
        }
        if (weakJoinHandle != null && isInverse) {
            throw new AssertionError("Internal bug: Bad constructor arguments for TableImpl");
        }

        this.mergedNode = mergedNode;
        this.immutableType = immutableType;
        this.parent = parent;
        this.isInverse = isInverse;
        this.joinProp = joinProp;
        this.weakJoinHandle = weakJoinHandle;
        this.originalJoinType = joinType;
    }

    @Override
    public ImmutableType getImmutableType() {
        return immutableType;
    }

    @Override
    public AbstractMutableStatementImpl getStatement() {
        return mergedNode.statement;
    }

    @Override
    public TableImplementor<?> getParent() {
        return parent;
    }

    @Override
    public boolean isInverse() {
        return isInverse;
    }

    @Override
    public boolean isEmpty(java.util.function.Predicate<TableImplementor<?>> filter) {
        if (mergedNode.isEmpty()) {
            return true;
        }
        if (filter == null) {
            return false;
        }
        for (MergedNode childNode : mergedNode) {
            for (TableImplementor<?> childTableImplementor : childNode.tableImplementors()) {
                if (filter.test(childTableImplementor)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isRemote() {
        return joinProp != null && joinProp.isRemote();
    }

    @Override
    public ImmutableProp getJoinProp() {
        return joinProp;
    }

    @Override
    public WeakJoinHandle getWeakJoinHandle() {
        return weakJoinHandle;
    }

    @Override
    public JoinType getJoinType() {
        return originalJoinType;
    }

    @Override
    public String getAlias() {
        return mergedNode.alias;
    }

    @Nullable
    @Override
    public String getMiddleTableAlias() {
        return mergedNode.middleTableAlias;
    }

    @Override
    public Iterator<MergedNode> iterator() {
        return mergedNode.iterator();
    }

    @Override
    public Predicate eq(Table<E> other) {
        if (other.getImmutableType() != immutableType) {
            throw new IllegalArgumentException("Cannot compare tables of different types");
        }
        ImmutableProp idProp = immutableType.getIdProp();
        return this.get(idProp).eq(other.get(idProp));
    }

    @Override
    public Predicate eq(Example<E> example) {
        return ((ExampleImpl<E>)example).toPredicate(this);
    }

    @Override
    public Predicate eq(E example) {
        return eq(Example.of(example));
    }

    @Override
    public Predicate eq(View<E> view) {
        return eq(Example.of(view));
    }

    @Override
    public Predicate isNull() {
        String idPropName = immutableType.getIdProp().getName();
        return this.get(idPropName).isNull();
    }

    @Override
    public Predicate isNotNull() {
        String idPropName = immutableType.getIdProp().getName();
        return this.get(idPropName).isNotNull();
    }

    @Override
    public NumericExpression<Long> count() {
        return count(false);
    }

    @Override
    public NumericExpression<Long> count(boolean distinct) {
        if (immutableType instanceof AssociationType) {
            return this.get(((AssociationType)immutableType).getSourceProp().getName()).count();
        }
        return this.get(immutableType.getIdProp().getName()).count(distinct);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> PropExpression<X> get(String prop) {
        return get(immutableType.getProp(prop));
    }

    @Override
    public <X> PropExpression<X> get(ImmutableProp prop) {
        return get(prop, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> PropExpression<X> get(ImmutableProp prop, boolean rawId) {
        if (isRemote() && immutableType.getIdProp() != prop) {
            throw new IllegalArgumentException(
                    "The current table is remote so that only the id property \"" +
                            immutableType.getIdProp() +
                            "\" can be accessed"
            );
        }
        if (prop.getDeclaringType() != immutableType) {
            if (!prop.getDeclaringType().isAssignableFrom(immutableType)) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                prop +
                                "\" does not belong to the current type \"" +
                                immutableType +
                                "\""
                );
            }
            prop = immutableType.getProp(prop.getName());
        }
        ImmutableProp idViewBaseProp = prop.getIdViewBaseProp();
        if (idViewBaseProp != null && idViewBaseProp.isReference(TargetLevel.ENTITY)) {
            return joinImplementor(idViewBaseProp.getName(), idViewBaseProp.isNullable() ? JoinType.LEFT : JoinType.INNER)
                    .get(idViewBaseProp.getTargetType().getIdProp(), true);
        }
        return (PropExpression<X>) PropExpressionImpl.of(this, prop, rawId);
    }

    @Override
    public <X> PropExpression<X> getId() {
        return get(immutableType.getIdProp());
    }

    @Override
    public <X> PropExpression<X> getAssociatedId(String prop) {
        ImmutableProp immutableProp = immutableType.getProp(prop);
        return getAssociatedId(immutableProp);
    }

    @Override
    public <X> PropExpression<X> getAssociatedId(ImmutableProp prop) {
        TableImplementor<?> joinedTable = joinImplementor(prop, prop.isNullable() ? JoinType.LEFT : JoinType.INNER);
        return joinedTable.get(joinedTable.getImmutableType().getIdProp(), true);
    }

    @Override
    public <XT extends Table<?>> XT join(ImmutableProp prop) {
        return TableProxies.wrap(joinImplementor(prop));
    }

    @Override
    public <XT extends Table<?>> XT join(String prop) {
        return TableProxies.wrap(joinImplementor(prop));
    }

    @Override
    public <XT extends Table<?>> XT join(ImmutableProp prop, JoinType joinType) {
        return TableProxies.wrap(joinImplementor(prop, joinType));
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType) {
        return TableProxies.wrap(joinImplementor(prop, joinType));
    }

    @Override
    public <XT extends Table<?>> XT join(ImmutableProp prop, JoinType joinType, ImmutableType treatedAs) {
        return TableProxies.wrap(joinImplementor(prop, joinType, treatedAs));
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType, ImmutableType treatedAs) {
        return TableProxies.wrap(joinImplementor(prop, joinType, treatedAs));
    }

    @Override
    public <X> PropExpression<X> inverseGetAssociatedId(ImmutableProp prop) {
        ImmutableProp oppositeProp = prop.getOpposite();
        TableImplementor<?> joinedTable = inverseJoinImplementor(
                prop,
                oppositeProp != null && oppositeProp.isNullable() ? JoinType.LEFT : JoinType.INNER
        );
        return joinedTable.get(joinedTable.getImmutableType().getIdProp(), true);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop) {
        return TableProxies.wrap(inverseJoinImplementor(prop));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop, JoinType joinType) {
        return TableProxies.wrap(inverseJoinImplementor(prop, joinType));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop) {
        return TableProxies.wrap(inverseJoinImplementor(prop));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop, JoinType joinType) {
        return TableProxies.wrap(inverseJoinImplementor(prop, joinType));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock
    ) {
        return inverseJoin(ImmutableProps.join(targetTableType, backPropBlock));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock,
            JoinType joinType
    ) {
        return inverseJoin(ImmutableProps.join(targetTableType, backPropBlock), joinType);
    }

    @Override
    public <X> TableImplementor<X> joinImplementor(String prop) {
        return joinImplementor(immutableType.getProp(prop), JoinType.INNER, null);
    }

    @Override
    public <X> TableImplementor<X> joinImplementor(ImmutableProp prop) {
        return joinImplementor(prop, JoinType.INNER, null);
    }

    @Override
    public <X> TableImplementor<X> joinImplementor(String prop, JoinType joinType) {
        return joinImplementor(immutableType.getProp(prop), joinType, null);
    }

    @Override
    public <X> TableImplementor<X> joinImplementor(ImmutableProp prop, JoinType joinType) {
        return joinImplementor(prop, joinType, null);
    }

    @Override
    public <X> TableImplementor<X> joinImplementor(String prop, JoinType joinType, ImmutableType treatedAs) {
        return joinImplementor(immutableType.getProp(prop), joinType, treatedAs);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> TableImplementor<X> joinImplementor(ImmutableProp prop, JoinType joinType, ImmutableType treatedAs) {
        if (prop.getDeclaringType() != immutableType) {
            if (!prop.getDeclaringType().isAssignableFrom(immutableType)) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                prop +
                                "\" does not belong to the current type \"" +
                                immutableType +
                                "\""
                );
            }
            prop = immutableType.getProp(prop.getName());
        }
        ImmutableProp manyToManyViewBaseProp = prop.getManyToManyViewBaseProp();
        if (manyToManyViewBaseProp != null) {
            return (TableImplementor<X>)
                    ((TableImpl<Object>)join0(false, manyToManyViewBaseProp, joinType))
                    .join0(false, prop.getManyToManyViewBaseDeeperProp(), joinType);
        }
        if (!prop.isAssociation(TargetLevel.ENTITY)) {
            if (isRemote()) {
                throw new IllegalStateException(
                        "The current table is remote so that join is not supported"
                );
            }
            if (prop.isTransient()) {
                throw new IllegalArgumentException(
                        "\"" + prop + "\" cannot be transient"
                );
            }
            if (prop.isRemote() && prop.getMappedBy() != null) {
                throw new IllegalArgumentException(
                        "\"" + prop + "\" cannot be remote and reversed(with `mappedBy`)"
                );
            }
            throw new IllegalArgumentException(
                    "\"" +
                            prop +
                            "\" is not association property of \"" +
                            this.immutableType +
                            "\""
            );
        }
        return (TableImplementor<X>) join0(false, prop, joinType);
    }

    @Override
    public <X> TableImplementor<X>  inverseJoinImplementor(ImmutableProp prop) {
        return inverseJoinImplementor(prop, JoinType.INNER);
    }

    @Override
    public <X> TableImplementor<X>  inverseJoinImplementor(TypedProp.Association<?, ?> prop) {
        return inverseJoinImplementor(prop.unwrap(), JoinType.INNER);
    }

    @Override
    public <X> TableImplementor<X>  inverseJoinImplementor(TypedProp.Association<?, ?> prop, JoinType joinType) {
        return inverseJoinImplementor(prop.unwrap(), joinType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> TableImplementor<X>  inverseJoinImplementor(ImmutableProp backProp, JoinType joinType) {
        if (backProp.getTargetType() != immutableType) {
            throw new IllegalArgumentException("'" + backProp + "' is not back association property");
        }
        if (!backProp.getDeclaringType().isEntity()) {
            throw new IllegalArgumentException("'" + backProp + "' is not declared in entity");
        }
        ImmutableProp manyToManyViewBaseProp = backProp.getManyToManyViewBaseProp();
        if (manyToManyViewBaseProp != null) {
            return (TableImplementor<X>)
                    ((TableImpl<?>)join0(true, backProp.getManyToManyViewBaseDeeperProp(), joinType))
                    .join0(true, manyToManyViewBaseProp, joinType);
        }
        return (TableImplementor<X>) join0(true, backProp, joinType);
    }

    private TableImplementor<?> join0(
            boolean isInverse,
            ImmutableProp prop,
            JoinType joinType
    ) {
        if (prop.isTransient()) {
            throw new ExecutionException(
                    "Cannot join to '" +
                            prop.getName() +
                            "' because it's transient association"
            );
        }
        if (isInverse && prop instanceof AssociationProp) {
            throw new ExecutionException(
                    "Cannot join to '" + prop + "' by inverse mode because it's property of association entity"
            );
        }

        // TODO:
        // statement.validateMutable();

        String joinName;
        if (!isInverse) {
            joinName = prop.getName();
        } else if (prop.getOpposite() != null) {
            joinName = prop.getOpposite().getName();
        } else {
            joinName = "inverse(" + prop + ")";
        }

        if (prop.getMappedBy() != null) {
            return join1(
                    joinName,
                    !isInverse,
                    prop.getMappedBy(),
                    joinType
            );
        }
        return join1(joinName, isInverse, prop, joinType);
    }

    private TableImplementor<?> join1(
            String joinName,
            boolean isInverse,
            ImmutableProp prop,
            JoinType joinType
    ) {
        return mergedNode.table(joinName, mergedNode.statement, prop, joinType, node ->
                new TableImpl<>(
                        node,
                        isInverse ? prop.getDeclaringType() : prop.getTargetType(),
                        this,
                        isInverse,
                        prop,
                        null,
                        joinType
                )
        );
    }

    @Override
    public <X> TableImplementor<X> weakJoinImplementor(Class<? extends WeakJoin<?, ?>> weakJoinType, JoinType joinType) {
        return weakJoinImplementor(WeakJoinHandle.of(weakJoinType), joinType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> TableImplementor<X> weakJoinImplementor(WeakJoinHandle handle, JoinType joinType) {
        String joinName = "weak(" + handle.getWeakJoinType().getName() + ")";
        return (TableImplementor<X>) mergedNode.table(joinName, mergedNode.statement, null, joinType, node ->
                new TableImpl<>(
                        node,
                        handle.getTargetType(),
                        this,
                        isInverse,
                        null,
                        handle,
                        joinType
                )
        );
    }

    @Override
    public Selection<E> fetch(Fetcher<E> fetcher) {
        if (fetcher == null) {
            return this;
        }
        if (immutableType != fetcher.getImmutableType()) {
            throw new IllegalArgumentException(
                    "Illegal fetcher type, the entity type of current table is \"" +
                            this +
                            "\" but the fetcher type is \"" +
                            fetcher.getImmutableType() +
                            "\""
            );
        }
        return new FetcherSelectionImpl<>(this, fetcher);
    }

    @Override
    public <V extends View<E>> Selection<V> fetch(Class<V> viewType) {
        if (viewType == null) {
            throw new IllegalArgumentException("The argument `staticType` cannot be null");
        }
        DtoMetadata<E, V> metadata = DtoMetadata.of(viewType);
        Fetcher<E> fetcher = metadata.getFetcher();
        if (immutableType != fetcher.getImmutableType()) {
            throw new IllegalArgumentException(
                    "Illegal fetcher type, the entity type of current table is \"" +
                            this +
                            "\" but the static type is based on \"" +
                            fetcher.getImmutableType() +
                            "\""
            );
        }
        return new FetcherSelectionImpl<>(this, fetcher, metadata.getConverter());
    }

    @Override
    public TableEx<E> asTableEx() {
        return TableProxies.wrap(this);
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        visitor.visitTableReference(this, null, false);
    }

    @Override
    public void renderJoinAsFrom(SqlBuilder builder, RenderMode mode) {
        if (parent == null) {
            throw new IllegalStateException("Internal bug: renderJoinAsFrom can only be called base on joined tables");
        }
        if (mode == RenderMode.NORMAL) {
            throw new IllegalStateException("Internal bug: renderJoinAsFrom does not accept render mode ALL");
        }
        TableUsedState usedState = builder.getAstContext().getTableUsedState(this);
        if (usedState != TableUsedState.NONE) {
            if (mode == RenderMode.FROM_ONLY || mode == RenderMode.WHERE_ONLY) {
                builder.separator();
            }
            renderSelf(builder, mode);
            if (mode == RenderMode.DEEPER_JOIN_ONLY) {
                for (MergedNode childNode : mergedNode) {
                    childNode.renderTo(builder);
                }
            }
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        SqlBuilder sqlBuilder = builder.assertSimple();
        TableUsedState usedState = sqlBuilder.getAstContext().getTableUsedState(this);
        if (parent == null || usedState != TableUsedState.NONE) {
            renderSelf(sqlBuilder, RenderMode.NORMAL);
            for (MergedNode childNode : mergedNode) {
                childNode.renderTo(sqlBuilder);
            }
        }
    }

    private void renderSelf(SqlBuilder builder, RenderMode mode) {
        AbstractMutableStatementImpl statement = mergedNode.statement;
        Predicate filterPredicate;
        if (isInverse) {
            renderInverseJoin(builder, mode);
            filterPredicate = statement.getFilterPredicate(this, builder.getAstContext());
        } else if (joinProp != null || weakJoinHandle != null) {
            renderJoin(builder, mode);
            filterPredicate = statement.getFilterPredicate(this, builder.getAstContext());
        } else {
            builder
                    .from()
                    .sql(immutableType.getTableName(builder.getAstContext().getSqlClient().getMetadataStrategy()))
                    .sql(" ")
                    .sql(mergedNode.alias);
            filterPredicate = null;
        }
        if (filterPredicate != null) {
            builder.sql(" and ");
            ((Ast)filterPredicate).renderTo(builder);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderJoin(SqlBuilder builder, RenderMode mode) {

        MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();

        if (weakJoinHandle != null) {
            if (builder.getAstContext().getTableUsedState(this) != TableUsedState.NONE) {
                Predicate predicate = weakJoinHandle.createPredicate(
                        parent,
                        this,
                        builder.getAstContext().getStatement()
                );
                builder
                        .join(mergedNode.getMergedJoinType(builder.getAstContext()))
                        .sql(immutableType.getTableName(strategy))
                        .sql(" ")
                        .sql(mergedNode.alias)
                        .on();
                if (predicate == null) {
                    builder.sql("1 = 1");
                } else {
                    ((Ast)predicate).renderTo(builder);
                }
            }
            return;
        }

        if (joinProp.getSqlTemplate() instanceof JoinTemplate) {
            renderJoinBySql(builder, (JoinTemplate) joinProp.getSqlTemplate(), mode);
            return;
        }

        if (joinProp instanceof AssociationProp) {
            if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED) {
                renderJoinImpl(
                        builder,
                        mergedNode.getMergedJoinType(builder.getAstContext()),
                        parent.getAlias(),
                        joinProp.getStorage(strategy),
                        immutableType.getTableName(strategy),
                        mergedNode.alias,
                        immutableType.getIdProp().getStorage(strategy),
                        mode
                );
                renderMiddleTableFilters(
                        ((AssociationProp)joinProp).getDeclaringType().getMiddleTable(strategy),
                        parent.getAlias(),
                        builder
                );
            }
            return;
        }

        TableImpl<?> parent = this.parent;
        JoinType joinType = mergedNode.getMergedJoinType(builder.getAstContext());
        MiddleTable middleTable = null;
        if (joinProp.isMiddleTableDefinition()) {
            middleTable = joinProp.getStorage(strategy);
        }

        if (middleTable != null) {
            renderJoinImpl(
                    builder,
                    joinType,
                    parent.getAlias(),
                    parent.immutableType.getIdProp().getStorage(strategy),
                    middleTable.getTableName(),
                    mergedNode.middleTableAlias,
                    middleTable.getColumnDefinition(),
                    mode
            );
            renderMiddleTableFilters(
                    middleTable,
                    mergedNode.middleTableAlias,
                    builder
            );
            if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED && (
                    mode == RenderMode.NORMAL ||
                            mode == RenderMode.DEEPER_JOIN_ONLY)
            ) {
                renderJoinImpl(
                        builder,
                        joinType,
                        mergedNode.middleTableAlias,
                        middleTable.getTargetColumnDefinition(),
                        immutableType.getTableName(strategy),
                        mergedNode.alias,
                        immutableType.getIdProp().getStorage(strategy),
                        RenderMode.NORMAL
                );
            }
        } else if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED) {
            renderJoinImpl(
                    builder,
                    joinType,
                    parent.getAlias(),
                    joinProp.getStorage(strategy),
                    immutableType.getTableName(strategy),
                    mergedNode.alias,
                    immutableType.getIdProp().getStorage(strategy),
                    mode
            );
        }
    }

    private void renderInverseJoin(SqlBuilder builder, RenderMode mode) {

        MetadataStrategy strategy = builder.sqlClient().getMetadataStrategy();
        TableImpl<?> parent = this.parent;
        JoinType joinType = mergedNode.getMergedJoinType(builder.getAstContext());

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
                    parent.getAlias(),
                    parent.immutableType.getIdProp().getStorage(strategy),
                    middleTable.getTableName(),
                    mergedNode.middleTableAlias,
                    middleTable.getTargetColumnDefinition(),
                    mode
            );
            renderMiddleTableFilters(
                    middleTable,
                    mergedNode.middleTableAlias,
                    builder
            );
            if (builder.getAstContext().getTableUsedState(this) == TableUsedState.USED && (
                    mode == RenderMode.NORMAL ||
                            mode == RenderMode.DEEPER_JOIN_ONLY)
            ) {
                renderJoinImpl(
                        builder,
                        joinType,
                        mergedNode.middleTableAlias,
                        middleTable.getColumnDefinition(),
                        immutableType.getTableName(strategy),
                        mergedNode.alias,
                        immutableType.getIdProp().getStorage(strategy),
                        RenderMode.NORMAL
                );
            }
        } else { // One-to-many join cannot be optimized by "used"
            renderJoinImpl(
                    builder,
                    joinType,
                    parent.getAlias(),
                    parent.immutableType.getIdProp().getStorage(strategy),
                    immutableType.getTableName(strategy),
                    mergedNode.alias,
                    joinProp.getStorage(strategy),
                    mode
            );
        }
    }

    private void renderJoinBySql(
            SqlBuilder builder,
            JoinTemplate joinTemplate,
            RenderMode mode
    ) {
        if (builder.getAstContext().getTableUsedState(this) != TableUsedState.NONE) {
            MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();
            switch (mode) {
                case NORMAL:
                    builder
                            .join(mergedNode.getMergedJoinType(builder.getAstContext()))
                            .sql(immutableType.getTableName(strategy))
                            .sql(" ")
                            .sql(mergedNode.alias)
                            .on();
                    break;
                case FROM_ONLY:
                    builder
                            .sql(immutableType.getTableName(strategy))
                            .sql(" ")
                            .sql(mergedNode.alias);
                    break;
            }
            if (mode == RenderMode.NORMAL || mode == RenderMode.WHERE_ONLY) {
                if (isInverse) {
                    builder.sql(joinTemplate.toSql(mergedNode.alias, parent.getAlias()));
                } else {
                    builder.sql(joinTemplate.toSql(parent.getAlias(), mergedNode.alias));
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
            RenderMode mode
    ) {
        if (mode != RenderMode.NORMAL && joinType != JoinType.INNER) {
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
        if (mode == RenderMode.NORMAL || mode == RenderMode.WHERE_ONLY) {
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

    @Override
    public void renderSelection(
            ImmutableProp prop,
            boolean rawId,
            SqlBuilder builder,
            ColumnDefinition optionalDefinition,
            boolean withPrefix,
            Function<Integer, String> asBlock
    ) {
        MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();
        if (prop.isId() && joinProp != null && !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                (rawId || TableUtils.isRawIdAllowed(this, builder.getAstContext().getSqlClient()))) {
            MiddleTable middleTable;
            if (joinProp.isMiddleTableDefinition()) {
                middleTable = joinProp.getStorage(strategy);
            } else {
                middleTable = null;
            }
            boolean isInverse = this.isInverse;
            if (middleTable != null) {
                if (optionalDefinition == null) {
                    if (isInverse) {
                        builder.definition(withPrefix ? mergedNode.middleTableAlias : null, middleTable.getColumnDefinition(), asBlock);
                    } else {
                        builder.definition(withPrefix ? mergedNode.middleTableAlias : null, middleTable.getTargetColumnDefinition(), asBlock);
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
                            builder.sql(mergedNode.middleTableAlias).sql(".");
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
                    builder.definition(withPrefix ? parent.getAlias() : null, joinProp.getStorage(strategy), asBlock);
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
                            builder.sql(parent.getAlias()).sql(".");
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
            builder.sql(((FormulaTemplate)template).toSql(mergedNode.alias));
            if (asBlock != null) {
                builder.sql(" ").sql(asBlock.apply(0));
            }
        } else {
            ColumnDefinition definition = optionalDefinition != null ?
                    optionalDefinition :
                    prop.getStorage(strategy);
            builder.definition(withPrefix ? mergedNode.alias : null, definition, asBlock);
        }
    }

    @Override
    public String getFinalAlias(
            ImmutableProp prop,
            boolean rawId,
            JSqlClientImplementor sqlClient
    ) {
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        if (prop.isId() && joinProp != null && !(joinProp.getSqlTemplate() instanceof JoinTemplate) &&
                (rawId || TableUtils.isRawIdAllowed(this, sqlClient))) {
            MiddleTable middleTable;
            if (joinProp.isMiddleTableDefinition()) {
                middleTable = joinProp.getStorage(strategy);
            } else {
                middleTable = null;
            }
            boolean isInverse = this.isInverse;
            if (middleTable != null) {
                return mergedNode.middleTableAlias;
            }
            if (!isInverse) {
                return parent.getAlias();
            }
        }
        return mergedNode.alias;
    }

    @Override
    public String toString() {
        String text;
        if (joinProp == null) {
            text = immutableType.getJavaClass().getSimpleName();
        } else if (isInverse) {
            ImmutableProp opposite = joinProp.getOpposite();
            if (opposite != null) {
                text = parent.toString() + '.' + opposite.getName();
            } else {
                text = parent + "[← " + joinProp + ']';
            }
        } else {
            return parent.toString() + '.' + joinProp.getName();
        }
        JoinType joinType = originalJoinType;
        if (joinType == JoinType.INNER) {
            return text;
        }
        return text + '(' + joinType.name().toLowerCase() + ')';
    }

    @Override
    public TableRowCountDestructive getDestructive() {
        if (joinProp == null) {
            return TableRowCountDestructive.NONE;
        }
        ImmutableProp prop;
        if (isInverse) {
            prop = joinProp.getOpposite();
            if (prop == null) {
                return TableRowCountDestructive.BREAK_REPEATABILITY;
            }
        } else {
            prop = joinProp;
        }
        if (prop.isReferenceList(TargetLevel.PERSISTENT)) {
            return TableRowCountDestructive.BREAK_REPEATABILITY;
        }
        if (prop.isNullable() && originalJoinType != JoinType.LEFT) {
            return TableRowCountDestructive.BREAK_ROW_COUNT;
        }
        return TableRowCountDestructive.NONE;
    }

    @Override
    public <XT extends Table<?>> Predicate exists(String prop, Function<XT, Predicate> block) {
        ImmutableProp joinProp = immutableType.getProps().get(prop);
        if (joinProp == null) {
            throw new IllegalArgumentException(
                    "Illegal property name \"" +
                            prop +
                            "\", there is no such property \"" +
                            immutableType +
                            "\""
            );
        }
        return exists(joinProp, block);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XT extends Table<?>> Predicate exists(ImmutableProp prop, Function<XT, Predicate> block) {
        return new AssociatedPredicate(
                this,
                prop,
                (Function<Table<?>, Predicate>) block
        );
    }

    @Override
    public boolean hasVirtualPredicate() {
        return false;
    }

    @Override
    public Ast resolveVirtualPredicate(AstContext ctx) {
        return this;
    }
}