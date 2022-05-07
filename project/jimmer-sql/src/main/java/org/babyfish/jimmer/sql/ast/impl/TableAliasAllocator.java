package org.babyfish.jimmer.sql.ast.impl;

public class TableAliasAllocator {

    private int num;

    public String allocate() {
        return "tb_" + ++num;
    }
}
