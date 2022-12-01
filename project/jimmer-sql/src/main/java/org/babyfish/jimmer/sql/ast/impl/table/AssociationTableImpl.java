package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.table.TableEx;

class AssociationTableImpl<SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>>
        extends TableImpl<Association<SE, TE>>
        implements TableEx<Association<SE, TE>>, org.babyfish.jimmer.sql.ast.table.AssociationTable<SE, ST, TE, TT> {

    public AssociationTableImpl(
            AbstractMutableStatementImpl statement,
            AssociationType associationType
    ) {
        super(statement, associationType, null, false, null, null, JoinType.INNER);
    }
}
