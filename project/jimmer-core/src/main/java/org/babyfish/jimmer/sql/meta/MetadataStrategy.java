package org.babyfish.jimmer.sql.meta;

import java.util.Objects;

public class MetadataStrategy {

    private final DatabaseNamingStrategy namingStrategy;

    private final ForeignKeyStrategy foreignKeyStrategy;

    public MetadataStrategy(
            DatabaseNamingStrategy namingStrategy,
            ForeignKeyStrategy foreignKeyStrategy
    ) {
        this.namingStrategy = namingStrategy;
        this.foreignKeyStrategy = foreignKeyStrategy;
    }

    public DatabaseNamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public ForeignKeyStrategy getForeignKeyStrategy() {
        return foreignKeyStrategy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(namingStrategy, foreignKeyStrategy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetadataStrategy)) return false;
        MetadataStrategy that = (MetadataStrategy) o;
        return namingStrategy.equals(that.namingStrategy) && foreignKeyStrategy.equals(that.foreignKeyStrategy);
    }

    @Override
    public String toString() {
        return "DatabaseMetadataStrategy{" +
                "namingStrategy=" + namingStrategy +
                ", foreignKeyStrategy=" + foreignKeyStrategy +
                '}';
    }
}
