package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.babyfish.jimmer.sql.runtime.TableUsedState;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class MergedNode extends AbstractDataManager<String, MergedNode> {

    final AbstractMutableStatementImpl statement;

    final String alias;

    final String middleTableAlias;

    private TableImpl<?> innerChild;

    private TableImpl<?> leftChild;

    private TableImpl<?> rightChild;

    private TableImpl<?> fullChild;

    private final TableImplementors tableImplementors = new TableImplementors();

    public MergedNode(AbstractMutableStatementImpl statement, ImmutableProp joinProp) {
        this.statement = statement;
        StatementContext ctx = statement.getContext();
        if (joinProp != null) {
            if (joinProp.isMiddleTableDefinition()) {
                middleTableAlias = statement.getContext().allocateTableAlias();
            } else if (joinProp.getSqlTemplate() == null && !joinProp.hasStorage()) {
                throw new AssertionError("Internal bug: Join property has not storage");
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

    TableImplementor<?> table(
            String joinName,
            AbstractMutableStatementImpl statement,
            ImmutableProp joinProp,
            JoinType joinType,
            Function<MergedNode, TableImpl<?>> supplier
    ) {
        MergedNode childNode = getValue(joinName);
        if (childNode == null) {
            childNode = new MergedNode(statement, joinProp);
            putValue(joinName, childNode);
        }
        return childNode.table(joinType, supplier);
    }

    TableImplementor<?> table(JoinType joinType, Function<MergedNode, TableImpl<?>> supplier) {
        switch (joinType) {
            case LEFT:
                if (leftChild != null) {
                    return leftChild;
                }
                return leftChild = supplier.apply(this);
            case RIGHT:
                if (rightChild != null) {
                    return rightChild;
                }
                return rightChild = supplier.apply(this);
            case FULL:
                if (fullChild != null) {
                    return fullChild;
                }
                return fullChild = supplier.apply(this);
            default:
                if (innerChild != null) {
                    return innerChild;
                }
                return innerChild = supplier.apply(this);
        }
    }

    public JoinType getMergedJoinType(AstContext ctx) {

        TableImpl<?> i = innerChild;
        if (i != null && ctx.getTableUsedState(i) != TableUsedState.NONE) {
            return JoinType.INNER;
        }

        TableImpl<?> l = leftChild;
        TableImpl<?> r = rightChild;
        TableImpl<?> f = fullChild;
        JoinType mergedJoinType = null;
        if (l != null && ctx.getTableUsedState(l) != TableUsedState.NONE) {
            mergedJoinType = JoinType.LEFT;
        }
        if (r != null && ctx.getTableUsedState(r) != TableUsedState.NONE) {
            if (mergedJoinType != null) {
                return JoinType.INNER;
            }
            mergedJoinType = JoinType.RIGHT;
        }
        if (f != null && ctx.getTableUsedState(f) != TableUsedState.NONE) {
            if (mergedJoinType != null) {
                return JoinType.INNER;
            }
            mergedJoinType = JoinType.FULL;
        }
        return mergedJoinType;
    }

    public void accept(AstVisitor visitor) {
        if (innerChild != null) {
            innerChild.accept(visitor);
        } else if (leftChild != null) {
            leftChild.accept(visitor);
        } else if (rightChild != null) {
            rightChild.accept(visitor);
        } else {
            fullChild.accept(visitor);
        }
    }

    public void renderTo(SqlBuilder builder) {
        AstContext ctx = builder.getAstContext();
        TableImpl<?> i = innerChild;
        TableImpl<?> l = leftChild;
        TableImpl<?> r = rightChild;
        TableImpl<?> f = fullChild;
        if (i != null && ctx.getTableUsedState(i) == TableUsedState.USED) {
            i.renderTo(builder);
        } else if (l != null && ctx.getTableUsedState(l) == TableUsedState.USED) {
            l.renderTo(builder);
        } else if (r != null && ctx.getTableUsedState(r) == TableUsedState.USED) {
            r.renderTo(builder);
        } else if (f != null && ctx.getTableUsedState(f) == TableUsedState.USED) {
            f.renderTo(builder);
        } else if (i != null && ctx.getTableUsedState(i) != TableUsedState.NONE) {
            i.renderTo(builder);
        } else if (l != null && ctx.getTableUsedState(l) != TableUsedState.NONE) {
            l.renderTo(builder);
        } else if (r != null && ctx.getTableUsedState(r) != TableUsedState.NONE) {
            r.renderTo(builder);
        } else if (f != null && ctx.getTableUsedState(f) != TableUsedState.NONE) {
            f.renderTo(builder);
        }
    }

    public void renderJoinAsFrom(SqlBuilder builder, TableImplementor.RenderMode mode) {
        AstContext ctx = builder.getAstContext();
        TableImpl<?> i = innerChild;
        TableImpl<?> l = leftChild;
        TableImpl<?> r = rightChild;
        TableImpl<?> f = fullChild;
        if (i != null && ctx.getTableUsedState(i) != TableUsedState.NONE) {
            i.renderJoinAsFrom(builder, mode);
        } else if (l != null && ctx.getTableUsedState(l) != TableUsedState.NONE) {
            l.renderJoinAsFrom(builder, mode);
        } else if (r != null && ctx.getTableUsedState(r) != TableUsedState.NONE) {
            r.renderJoinAsFrom(builder, mode);
        } else if (ctx.getTableUsedState(f) != TableUsedState.NONE) {
            f.renderJoinAsFrom(builder, mode);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MergedTable{");
        if (innerChild != null) {
            builder.append(",").append("innerChild=").append(innerChild);
        }
        if (leftChild != null) {
            builder.append(",").append("leftChild=").append(leftChild);
        }
        if (rightChild != null) {
            builder.append(",").append("rightChild=").append(rightChild);
        }
        if (fullChild != null) {
            builder.append(",").append("fullChild=").append(fullChild);
        }
        builder.append("}");
        return builder.toString();
    }

    @NotNull
    public Iterable<TableImplementor<?>> tableImplementors() {
        return tableImplementors;
    }

    private class TableImplementors implements Iterable<TableImplementor<?>> {

        @NotNull
        @Override
        public Iterator<TableImplementor<?>> iterator() {
            return new Itr();
        }
    }

    private class Itr implements Iterator<TableImplementor<?>> {

        private int step;

        public Itr() {
            if (innerChild != null) {
                step = 0;
            } else if (leftChild != null) {
                step = 1;
            } else if (rightChild != null) {
                step = 2;
            } else if (fullChild != null) {
                step = 3;
            } else {
                step = 4;
            }
        }

        @Override
        public boolean hasNext() {
            return step < 4;
        }

        @Override
        public TableImplementor<?> next() {
            int step = this.step;
            TableImplementor<?> ti = tableImplementor(step);
            if (ti == null) {
                throw new NoSuchElementException();
            }
            while (step < 4) {
                if (tableImplementor(++step) != null) {
                    break;
                }
            }
            this.step = step;
            return ti;
        }

        private TableImplementor<?> tableImplementor(int step) {
            switch (step) {
                case 0:
                    return innerChild;
                case 1:
                    return leftChild;
                case 2:
                    return rightChild;
                case 3:
                    return fullChild;
                default:
                    return null;
            }
        }
    }
}
