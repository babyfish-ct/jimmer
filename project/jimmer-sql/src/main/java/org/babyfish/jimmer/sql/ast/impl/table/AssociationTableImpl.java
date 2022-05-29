package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.AssociationTableEx;
import org.babyfish.jimmer.sql.ast.table.TableEx;

import javax.persistence.criteria.JoinType;

class AssociationTableImpl<SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>>
        extends TableImpl<Association<SE, TE>>
        implements AssociationTableEx<SE, ST, TE, TT> {

    public AssociationTableImpl(
            AbstractMutableStatementImpl statement,
            AssociationType associationType
    ) {
        super(statement, associationType, null, false, null, JoinType.INNER);
    }
}
