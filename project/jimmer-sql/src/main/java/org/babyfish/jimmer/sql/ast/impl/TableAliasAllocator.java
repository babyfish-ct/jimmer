package org.babyfish.jimmer.sql.ast.impl;

class TableAliasAllocator {

    private int num;

    public String allocate() {
        return "tb_" + ++num;
    }
}
