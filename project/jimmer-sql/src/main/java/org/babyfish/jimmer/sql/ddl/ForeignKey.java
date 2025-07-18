package org.babyfish.jimmer.sql.ddl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

/**
 * @author honhimW
 */

public class ForeignKey {

    public final org.babyfish.jimmer.sql.ddl.annotations.ForeignKey foreignKey;

    public final ImmutableProp joinColumn;

    public final ImmutableType table;

    public final ImmutableType referencedTable;

    public ForeignKey(org.babyfish.jimmer.sql.ddl.annotations.ForeignKey foreignKey, ImmutableProp joinColumn, ImmutableType table, ImmutableType referencedTable) {
        this.foreignKey = foreignKey;
        this.joinColumn = joinColumn;
        this.table = table;
        this.referencedTable = referencedTable;
    }
}
