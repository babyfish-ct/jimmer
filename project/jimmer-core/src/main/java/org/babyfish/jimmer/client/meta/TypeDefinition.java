package org.babyfish.jimmer.client.meta;

import java.util.List;
import java.util.Map;

public interface TypeDefinition {

    TypeName getTypeName();

    boolean isImmutable();

    Map<String, Prop> getPropMap();

    List<TypeRef> getSuperTypes();

    Doc getDoc();

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

    static boolean isGenerationRequired(TypeName typeName) {
        String text = typeName.toString();
        switch (text) {
            case "boolean":
            case "char":
            case "byte":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
            case "java.lang.Object":
            case "java.io.Closeable":
            case "java.lang.AutoCloseable":
            case "java.lang.Enum":
            case "java.lang.Class":
            case "java.math.BigDecimal":
            case "java.math.BigInteger":
            case "java.util.Iterable":
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
                return false;
            default:
                return !text.startsWith("<");
        }
    }
}
