package org.babyfish.jimmer.sql.ddl;

/**
 * @author honhimW
 */

public class ConstraintNamingStrategy {

    public String determineUniqueKeyName(String tableName, String[] columnNames) {
        return defaultPattern("uk", tableName, columnNames);
    }

    public String determineIndexName(String tableName, String[] columnNames) {
        return defaultPattern("idx", tableName, columnNames);
    }

    public String determineForeignKeyName(String tableName, String[] columnNames) {
        return defaultPattern("fk", tableName, columnNames);
    }

    protected String defaultPattern(String prefix, String tableName, String[] columnNames) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append('_').append(tableName);
        for (String columnName : columnNames) {
            sb.append('_').append(columnName);
        }
        return sb.toString();
    }

}
