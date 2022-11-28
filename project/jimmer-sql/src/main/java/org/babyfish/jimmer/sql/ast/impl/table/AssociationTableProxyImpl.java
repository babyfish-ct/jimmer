package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.impl.util.StaticCache;

public class AssociationTableProxyImpl<SE, ST extends Table<SE>, TE, TT extends Table<TE>>
        extends AbstractTypedTable<Association<SE, TE>> implements
        AssociationTable<SE, ST, TE, TT>, TableProxy<Association<SE, TE>> {

    private static final StaticCache<AssociationType, AssociationTableProxyImpl<?, ?, ?, ?>> CACHE =
            new StaticCache<>(AssociationTableProxyImpl::new, false);

    AssociationTableProxyImpl(AssociationType type) {
        super(type);
    }

    @SuppressWarnings("unchecked")
    public static <SE, ST extends Table<SE>, TE, TT extends Table<TE>>
    AssociationTable<SE, ST, TE, TT> table(AssociationType associationType) {
        return (AssociationTableProxyImpl<SE, ST, TE, TT>)CACHE.get(associationType);
    }

    @Override
    public ST source() {
        return TableProxies.fluent(joinOperation("source"));
    }

    @Override
    public ST source(ImmutableType treatedAs) {
        return TableProxies.fluent(joinOperation("source", JoinType.INNER, treatedAs));
    }

    @Override
    public TT target() {
        return TableProxies.fluent(joinOperation("target"));
    }

    @Override
    public ST target(ImmutableType treatedAs) {
        return TableProxies.fluent(joinOperation("target", JoinType.INNER, treatedAs));
    }

    @Override
    public TableEx<Association<SE, TE>> asTableEx() {
        return this;
    }

    @Override
    public <P extends TableProxy<Association<SE, TE>>> P __disableJoin(String reason) {
        throw new UnsupportedOperationException();
    }
}
