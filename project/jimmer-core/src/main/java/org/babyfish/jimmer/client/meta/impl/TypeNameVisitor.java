package org.babyfish.jimmer.client.meta.impl;

public interface TypeNameVisitor {

    void visitTypeName(String typeName);

    static boolean isPrimitive(String typeName) {
        switch (typeName) {
            case "boolean":
            case "char":
            case "byte":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
                return true;
            default:
                return false;
        }
    }

    static boolean isSupportedByClient(String typeName) {
        switch (typeName) {
            case "boolean":
            case "char":
            case "byte":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "java.math.BigDecimal":
            case "java.math.BigInteger":
            case "java.util.Collection":
            case "java.util.List":
            case "java.util.Set":
            case "java.util.SortedSet":
            case "java.util.NavigableSet":
            case "java.util.SequencedSet":
            case "java.util.Map":
            case "java.util.SortedMap":
            case "java.util.NavigableMap":
            case "java.util.SequencedMap":
            case "java.lang.BigDecimal":
            case "java.lang.BigInteger":
            case "java.lang.String":
            case "java.util.UUID":
            case "java.util.Date":
            case "java.sql.Date":
            case "java.sql.Time":
            case "java.sql.Timestamp":
            case "java.time.LocalDate":
            case "java.time.LocalDateTime":
            case "java.time.OffsetDateTime":
            case "java.time.ZonedDateTime":
                return true;
            default:
                return false;
        }
    }
}
