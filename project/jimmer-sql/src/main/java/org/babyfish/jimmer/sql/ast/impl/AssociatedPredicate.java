package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.query.MutableSubQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.RootTableResolver;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.associated.VirtualPredicate;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AssociatedPredicate extends AbstractPredicate implements VirtualPredicate {

    private static final Predicate[] EMPTY_PREDICATES = new Predicate[0];

    private final Table<?> parenTable;

    private final ImmutableProp prop;

    private final Function<Table<?>, Predicate> block;

    public AssociatedPredicate(Table<?> parenTable, ImmutableProp prop, Function<Table<?>, Predicate> block) {
        if (prop.getDeclaringType() != parenTable.getImmutableType()) {
            if (!prop.getDeclaringType().isAssignableFrom(parenTable.getImmutableType())) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                prop +
                                "\" does not belong to the current type \"" +
                                parenTable.getImmutableType() +
                                "\""
                );
            }
            prop = parenTable.getImmutableType().getProp(prop.getName());
        }
        if (!prop.isAssociation(TargetLevel.PERSISTENT)) {
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
                            parenTable.getImmutableType() +
                            "\""
            );
        }
        this.parenTable = parenTable;
        this.prop = prop;
        this.block = block;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        throw new UnsupportedOperationException("`" + getClass().getName() + "` does not support `accept`");
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        throw new UnsupportedOperationException("`" + getClass().getName() + "` does not support `renderTo`");
    }

    @Override
    public int precedence() {
        throw new UnsupportedOperationException("`" + getClass().getName() + "` does not support `precedence`");
    }

    @Override
    public TableImplementor<?> getTableImplementor(RootTableResolver resolver) {
        if (parenTable instanceof TableImplementor<?>) {
            return (TableImplementor<?>) parenTable;
        }
        return ((TableProxy<?>)parenTable).__resolve(resolver);
    }

    @Override
    public Object getSubKey() {
        return prop.getName();
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return true;
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        return ctx.resolveVirtualPredicate(this);
    }

    @Override
    public Predicate toFinalPredicate(AbstractMutableStatementImpl parent, List<VirtualPredicate> virtualPredicates, Op op) {
        MutableSubQueryImpl query;
        Table<?> table;
        if (parenTable instanceof TableImplementor<?>) {
            query = new MutableSubQueryImpl(parent, prop.getTargetType());
            table = query.getTable();
        } else {
            TableProxy<?> proxy = TableProxies.fluent(prop.getTargetType().getJavaClass());
            query = new MutableSubQueryImpl(parent, proxy);
            table = proxy;
        }
        boolean hasUserPredicates = false;
        List<Predicate> predicates;
        if (op == Op.AND) {
            predicates = new ArrayList<>(virtualPredicates.size());
            predicates.add(null);
            for (VirtualPredicate virtualPredicate : virtualPredicates) {
                Predicate predicate = ((AssociatedPredicate) virtualPredicate).block.apply(table);
                if (predicate != null) {
                    predicates.add(predicate);
                    hasUserPredicates = true;
                }
            }
            if (hasUserPredicates) {
                predicates.set(0, table.inverseGetAssociatedId(prop).eq(parenTable.getId()));
            }
        } else {
            List<Predicate> orPredicates = new ArrayList<>(virtualPredicates.size());
            for (VirtualPredicate virtualPredicate : virtualPredicates) {
                Predicate predicate = ((AssociatedPredicate) virtualPredicate).block.apply(table);
                if (predicate != null) {
                    orPredicates.add(predicate);
                    hasUserPredicates = true;
                }
            }
            if (hasUserPredicates) {
                predicates = new ArrayList<>();
                predicates.add(table.inverseGetAssociatedId(prop).eq(parenTable.getId()));
                predicates.add(Predicate.or(orPredicates.toArray(EMPTY_PREDICATES)));
            } else {
                predicates = new ArrayList<>();
                predicates.add(table.inverseGetAssociatedId(prop).eq(parenTable.getId()));
            }
        }
        if (!hasUserPredicates) {
            return null;
        }
        query.where(predicates.toArray(EMPTY_PREDICATES));
        return query.exists();
    }
}
