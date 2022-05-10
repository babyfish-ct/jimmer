package org.babyfish.jimmer.sql.ast.impl.table;

public class TableAliasAllocator {

    private int num;

    public String allocate() {
        return "tb_" + ++num;
    }
}
