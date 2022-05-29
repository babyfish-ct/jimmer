package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.association.Association;

public interface AssociationTableEx<
        SE,
        ST extends TableEx<SE>,
        TE,
        TT extends TableEx<TE>
> extends
        TableEx<Association<SE, TE>>,
        AssociationTable<SE,ST, TE, TT> {
}
