package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

public interface SubQueryProvider {

    MutableSubQuery createSubQuery(TableProxy<?> table);

    <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>>
    MutableSubQuery createAssociationSubQuery(AssociationTable<SE, ST, TE, TT> table);
}
