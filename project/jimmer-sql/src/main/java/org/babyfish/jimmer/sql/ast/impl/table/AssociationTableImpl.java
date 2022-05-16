package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;

import javax.persistence.criteria.JoinType;

class AssociationTableImpl<SE, ST extends Table<SE>, TE, TT extends Table<TE>>
        extends TableImpl<Association<SE, TE>>
        implements AssociationTable<SE, ST, TE, TT> {

    public AssociationTableImpl(
            AbstractMutableStatementImpl statement,
            AssociationType associationType
    ) {
        super(statement, associationType, null, false, null, JoinType.INNER);
    }
}
