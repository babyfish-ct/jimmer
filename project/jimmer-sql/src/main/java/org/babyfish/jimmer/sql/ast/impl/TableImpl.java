package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.sql.Column;
import org.babyfish.jimmer.meta.sql.MiddleTable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import javax.persistence.criteria.JoinType;
import java.util.HashMap;
import java.util.Map;

class TableImpl<E> implements TableImplementor<E> {

    private AbstractMutableStatementImpl statement;

    private ImmutableType immutableType;

    private TableImpl<?> parent;

    private boolean isInverse;

    private ImmutableProp joinProp;

    private JoinType joinType;

    private String alias;

    private String middleTableAlias;

    private Map<String, TableImpl<?>> childTableMap =
            new HashMap<>();

    public TableImpl(
            AbstractMutableStatementImpl statement,
            ImmutableType immutableType,
            TableImpl<?> parent,
            boolean isInverse,
            ImmutableProp joinProp,
            JoinType joinType
    ) {
        this.statement = statement;
        this.immutableType = immutableType;
        this.parent = parent;
        this.isInverse = isInverse;
        this.joinProp = joinProp;
        this.joinType = joinType;

        if ((parent == null) != (joinProp == null)) {
            throw new AssertionError("Internal bug: Bad constructor arguments for TableImpl");
        }
        if (joinProp != null) {
            if (joinProp.getStorage() instanceof MiddleTable) {
                middleTableAlias = statement.getTableAliasAllocator().allocate();
            } else if (joinProp.getStorage() == null) {
                throw new AssertionError("Internal bug: Join property has not storage");
            }
        }
        alias = statement.getTableAliasAllocator().allocate();
    }

    @Override
    public ImmutableType getImmutableType() {
        return immutableType;
    }

    public AbstractMutableStatementImpl getStatement() {
        return statement;
    }

    public TableImpl<?> getParent() {
        return parent;
    }

    public String getAlias() {
        return alias;
    }

    protected TableImpl<?> createChildTable(
            boolean isInverse,
            ImmutableProp joinProp,
            JoinType joinType
    ) {
        return new TableImpl<>(
                statement,
                isInverse ? joinProp.getDeclaringType() : joinProp.getTargetType(),
                this,
                isInverse,
                joinProp,
                joinType
        );
    }

    @Override
    public Predicate eq(Table<E> other) {
        if (TableImplementor.unwrap(other).getImmutableType() != immutableType) {
            throw new IllegalArgumentException("Cannot compare tables of different types");
        }
        String idPropName = immutableType.getIdProp().getName();
        return this.<Expression<Object>>get(idPropName).eq(other.get(idPropName));
    }

    @Override
    public NumericExpression<Long> count() {
        return count(false);
    }

    @Override
    public NumericExpression<Long> count(boolean distinct) {
        return this.get(immutableType.getIdProp().getName()).count(distinct);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XE extends Expression<?>> XE get(String prop) {
        ImmutableProp immutableProp = immutableType.getProps().get(prop);
        if (immutableProp == null || !immutableProp.isScalar()) {
            throw new IllegalArgumentException("'" + prop + "' is not scalar property");
        }
        return (XE)PropExpression.of(this, immutableProp);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop) {
        return join(prop, JoinType.INNER);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType) {
        ImmutableProp immutableProp = immutableType.getProps().get(prop);
        if (immutableProp == null || !immutableProp.isAssociation()) {
            throw new IllegalArgumentException("'" + prop + "' is not association property");
        }
        return (XT)join0(false, immutableProp, joinType);
    }

    @Override
    public <XE, XT extends Table<XE>> XT inverseJoin(Class<XE> targetType, String backProp) {
        return inverseJoin(targetType, backProp, JoinType.INNER);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XE, XT extends Table<XE>> XT inverseJoin(Class<XE> targetType, String backProp, JoinType joinType) {
        ImmutableType immutableTargetType = ImmutableType.tryGet(targetType);
        if (immutableTargetType == null) {
            throw new IllegalArgumentException("'" + targetType.getName() + "' is not entity type");
        }
        ImmutableProp immutableBackProp = immutableTargetType.getProps().get(backProp);
        if (immutableBackProp == null || immutableBackProp.getTargetType() != immutableType) {
            throw new IllegalArgumentException("'" + backProp + "' is not back association property");
        }
        return (XT)join0(true, immutableBackProp, joinType);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoinByTable(
            Class<XT> targetTableType,
            String backProp
    ) {
        return inverseJoinByTable(targetTableType, backProp, joinType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XT extends Table<?>> XT inverseJoinByTable(
            Class<XT> targetTableType,
            String backProp,
            JoinType joinType
    ) {
        ImmutableType immutableType = ImmutableType.tryGet(targetTableType);
        if (immutableType == null) {
            throw new IllegalArgumentException(
                    "Cannot get immutable type from table type \"" +
                            targetTableType.getName() +
                            "\""
            );
        }
        return (XT)inverseJoin(immutableType.getJavaClass(), backProp, joinType);
    }

    private Table<?> join0(
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

        statement.validateMutable();

        String joinName;
        if (!isInverse) {
            joinName = prop.getName();
        } else if (prop.getOpposite() != null) {
            joinName = prop.getOpposite().getName();
        } else {
            joinName = "inverse(" + prop + ")";
        }

        if (prop.getMappedBy() != null) {
            return join1(joinName, !isInverse, prop.getMappedBy(), joinType);
        }
        return join1(joinName, isInverse, prop, joinType);
    }

    private Table<?> join1(
            String joinName,
            boolean isInverse,
            ImmutableProp prop,
            JoinType joinType
    ) {
        TableImpl<?> existing = childTableMap.get(joinName);
        if (existing != null) {
            if (existing.joinType != joinType) {
                existing.joinType = JoinType.INNER;
            }
            return TableWrappers.wrap(existing);
        }
        TableImpl<?> newTable = createChildTable(
                isInverse,
                prop,
                joinType
        );
        childTableMap.put(joinName, newTable);
        return TableWrappers.wrap(newTable);
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visitTableReference(this, null);
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        renderSelf(builder, RenderMode.NORMAL);
        if (parent == null || builder.isTableUsed(this)) {
            for (TableImpl<?> childTable : childTableMap.values()) {
                childTable.renderTo(builder);
            }
        }
    }

    private void renderSelf(SqlBuilder sqlBuilder, RenderMode mode) {
        if (isInverse) {
            renderInverseJoin(sqlBuilder, mode);
        } else if (joinProp != null) {
            renderJoin(sqlBuilder, mode);
        } else {
            sqlBuilder
                    .sql(" from ")
                    .sql(immutableType.getTableName())
                    .sql(" as ")
                    .sql(alias);
        }
    }

    private void renderJoin(SqlBuilder sqlBuilder, RenderMode mode) {

        TableImpl<?> parent = this.parent;
        JoinType joinType = this.joinType;
        MiddleTable middleTable = null;
        if (joinProp.getStorage() instanceof MiddleTable) {
            middleTable = (MiddleTable) joinProp.getStorage();
        }

        if (middleTable != null) {
            renderJoinImpl(
                    sqlBuilder,
                    joinType,
                    parent.alias,
                    ((Column)parent.immutableType.getIdProp().getStorage()).getName(),
                    middleTable.getTableName(),
                    middleTableAlias,
                    middleTable.getJoinColumnName(),
                    mode
            );
            if (sqlBuilder.isTableUsed(this) && (
                    mode == RenderMode.NORMAL ||
                            mode == RenderMode.DEEPER_JOIN_ONLY)
            ) {
                renderJoinImpl(
                        sqlBuilder,
                        joinType,
                        middleTableAlias,
                        middleTable.getTargetJoinColumnName(),
                        immutableType.getTableName(),
                        alias,
                        ((Column)immutableType.getIdProp().getStorage()).getName(),
                        RenderMode.NORMAL
                );
            }
        } else if (sqlBuilder.isTableUsed(this)) {
            renderJoinImpl(
                    sqlBuilder,
                    joinType,
                    parent.alias,
                    ((Column)joinProp.getStorage()).getName(),
                    immutableType.getTableName(),
                    alias,
                    ((Column)parent.immutableType.getIdProp().getStorage()).getName(),
                    mode
            );
        }
    }

    private void renderInverseJoin(SqlBuilder sqlBuilder, RenderMode mode) {

        TableImpl<?> parent = this.parent;
        JoinType joinType = this.joinType;
        MiddleTable middleTable = null;
        if (joinProp.getStorage() instanceof MiddleTable) {
            middleTable = (MiddleTable) joinProp.getStorage();
        }

        if (middleTable != null) {
            renderJoinImpl(
                    sqlBuilder,
                    joinType,
                    parent.alias,
                    ((Column)parent.immutableType.getIdProp().getStorage()).getName(),
                    middleTable.getTableName(),
                    middleTableAlias,
                    middleTable.getTargetJoinColumnName(),
                    mode
            );
            if (sqlBuilder.isTableUsed(this) && (
                    mode == RenderMode.NORMAL ||
                            mode == RenderMode.DEEPER_JOIN_ONLY)
            ) {
                renderJoinImpl(
                        sqlBuilder,
                        joinType,
                        middleTableAlias,
                        middleTable.getJoinColumnName(),
                        immutableType.getTableName(),
                        alias,
                        ((Column)immutableType.getIdProp().getStorage()).getName(),
                        RenderMode.NORMAL
                );
            }
        } else { // One-to-many join cannot be optimized by "used"
            renderJoinImpl(
                    sqlBuilder,
                    joinType,
                    parent.alias,
                    ((Column)parent.immutableType.getIdProp().getStorage()).getName(),
                    immutableType.getTableName(),
                    alias,
                    ((Column)joinProp.getStorage()).getName(),
                    mode
            );
        }
    }

    private void renderJoinImpl(
            SqlBuilder sqlBuilder,
            JoinType joinType,
            String previousAlias,
            String previousColumnName,
            String newTableName,
            String newAlias,
            String newColumnName,
            RenderMode mode
    ) {
        if (mode != RenderMode.NORMAL && joinType != JoinType.INNER) {
            throw new AssertionError("Internal bug: outer join cannot be accepted by abnormal render mode");
        }
        switch (mode) {
            case NORMAL:
                sqlBuilder
                        .sql(" ")
                        .sql(joinType.name().toLowerCase())
                        .sql(" join ")
                        .sql(newTableName)
                        .sql(" as ")
                        .sql(newAlias)
                        .sql(" on ");
                break;
            case FROM_ONLY:
                sqlBuilder
                        .sql(newTableName)
                        .sql(" as ")
                        .sql(newAlias);
                break;
        }
        if (mode == RenderMode.NORMAL || mode == RenderMode.WHERE_ONLY) {
            sqlBuilder
                    .sql(previousAlias)
                    .sql(".")
                    .sql(previousColumnName)
                    .sql(" = ")
                    .sql(newAlias)
                    .sql(".")
                    .sql(newColumnName);
        }
    }

    private enum RenderMode {
        NORMAL,
        FROM_ONLY,
        WHERE_ONLY,
        DEEPER_JOIN_ONLY;
    }

    void renderSelection(ImmutableProp prop, SqlBuilder builder) {
        if (prop.isId() && joinProp != null) {
            MiddleTable middleTable;
            if (joinProp.getStorage() instanceof MiddleTable) {
                middleTable = (MiddleTable) joinProp.getStorage();
            } else {
                middleTable = null;
            }
            boolean isInverse = this.isInverse;
            if (middleTable != null) {
                builder.sql(middleTableAlias).sql(".");
                if (isInverse) {
                    builder.sql(middleTable.getJoinColumnName());
                } else {
                    builder.sql(middleTable.getTargetJoinColumnName());
                }
                return;
            }
            if (!isInverse) {
                builder
                        .sql(parent.alias)
                        .sql(".")
                        .sql(((Column)joinProp.getStorage()).getName());
                return;
            }
        }
        builder.sql(alias).sql(".").sql(((Column)prop.getStorage()).getName());
    }

    @Override
    public String toString() {
        String text;
        if (joinProp == null) {
            text = immutableType.getJavaClass().getSimpleName();
        } else if (isInverse) {
            ImmutableProp opposite = joinProp.getOpposite();
            if (opposite != null) {
                text = parent.toString() + '.' + opposite;
            } else {
                text = "← " + parent + '.' + joinProp.getName();
            }
        } else {
            return parent.toString() + '.' + joinProp.getName();
        }
        if (joinType == JoinType.INNER) {
            return text;
        }
        return text + '(' + joinType.name().toLowerCase() + ')';
    }

    Destructive getDestructive() {
        if (joinProp == null) {
            return Destructive.NONE;
        }
        ImmutableProp prop;
        if (isInverse) {
            prop = joinProp.getOpposite();
            if (prop == null) {
                return Destructive.BREAK_REPEATABILITY;
            }
        } else {
            prop = joinProp;
        }
        if (prop.isEntityList()) {
            return Destructive.BREAK_REPEATABILITY;
        }
        if (prop.isNullable() && joinType != JoinType.LEFT) {
            return Destructive.BREAK_ROW_COUNT;
        }
        return Destructive.NONE;
    }

    enum Destructive {
        NONE, // Left join for nullable reference, Left/Inner join for non-null reference
        BREAK_ROW_COUNT, // inner join for nullable-reference
        BREAK_REPEATABILITY // Any join for Collection
    }
}
