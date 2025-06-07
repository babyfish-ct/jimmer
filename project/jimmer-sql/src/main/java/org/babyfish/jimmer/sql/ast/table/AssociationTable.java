package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.table.AssociationTableProxyImpl;

import java.util.function.Function;

public interface AssociationTable<
        SE,
        ST extends Table<SE>,
        TE,
        TT extends Table<TE>
> extends TableEx<Association<SE, TE>> {

    @SuppressWarnings("unchecked")
    default ST source() {
        return (ST)join("source");
    }

    default <SID> PropExpression<SID> sourceId() {
        return getAssociatedId("source");
    }

    @SuppressWarnings("unchecked")
    default ST source(ImmutableType treatedAs) {
        return (ST)join("source", JoinType.INNER, treatedAs);
    }

    default <TID> PropExpression<TID> targetId() {
        return getAssociatedId("target");
    }

    @SuppressWarnings("unchecked")
    default TT target() {
        return (TT)join("target");
    }

    @SuppressWarnings("unchecked")
    default ST target(ImmutableType treatedAs) {
        return (ST)join("target", JoinType.INNER, treatedAs);
    }

    static <
            SE,
            ST extends Table<SE>,
            TE,
            TT extends Table<TE>
    > AssociationTable<SE, ST, TE, TT> of(Class<ST> sourceTableType, Function<ST, TT> targetTableGetter) {
        return AssociationTableProxyImpl.table(
                AssociationType.of(
                        ImmutableProps.join(sourceTableType, targetTableGetter)
                )
        );
    }
}
