package org.babyfish.jimmer.meta.sql;

public class MiddleTable implements Storage {

    private String tableName;

    private String joinColumnName;

    private String targetJoinColumnName;

    private MiddleTable inverse;

    public MiddleTable(
            String tableName,
            String joinColumnName,
            String targetJoinColumnName
    ) {
        this.tableName = tableName;
        this.joinColumnName = joinColumnName;
        this.targetJoinColumnName = targetJoinColumnName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getJoinColumnName() {
        return joinColumnName;
    }

    public String getTargetJoinColumnName() {
        return targetJoinColumnName;
    }

    public MiddleTable getInverse() {
        MiddleTable iv = inverse;
        if (iv == null) {
            iv = new MiddleTable(tableName, targetJoinColumnName, joinColumnName);
            inverse = iv;
        }
        return iv;
    }
}
