package org.babyfish.jimmer.sql.meta;

public class MetadataStrategy {

    private final DatabaseNamingStrategy namingStrategy;

    private final ForeignKeyStrategy foreignKeyStrategy;

    private final SqlTypeStrategy sqlTypeStrategy;

    private final ScalarTypeStrategy scalarTypeStrategy;

    public MetadataStrategy(
            DatabaseNamingStrategy namingStrategy,
            ForeignKeyStrategy foreignKeyStrategy,
            SqlTypeStrategy sqlTypeStrategy,
            ScalarTypeStrategy scalarTypeStrategy
    ) {
        this.namingStrategy = namingStrategy;
        this.foreignKeyStrategy = foreignKeyStrategy;
        this.sqlTypeStrategy = sqlTypeStrategy;
        this.scalarTypeStrategy = scalarTypeStrategy;
    }

    public DatabaseNamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public ForeignKeyStrategy getForeignKeyStrategy() {
        return foreignKeyStrategy;
    }

    public SqlTypeStrategy getSqlTypeStrategy() {
        return sqlTypeStrategy;
    }

    public ScalarTypeStrategy getScalarTypeStrategy() {
        return scalarTypeStrategy;
    }

    @Override
    public int hashCode() {
        int result = namingStrategy.hashCode();
        result = 31 * result + foreignKeyStrategy.hashCode();
        result = 31 * result + sqlTypeStrategy.hashCode();
        result = 31 * result + scalarTypeStrategy.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetadataStrategy that = (MetadataStrategy) o;

        if (!namingStrategy.equals(that.namingStrategy)) return false;
        if (foreignKeyStrategy != that.foreignKeyStrategy) return false;
        if (!sqlTypeStrategy.equals(that.sqlTypeStrategy)) return false;
        return scalarTypeStrategy.equals(that.scalarTypeStrategy);
    }

    @Override
    public String toString() {
        return "MetadataStrategy{" +
                "namingStrategy=" + namingStrategy +
                ", foreignKeyStrategy=" + foreignKeyStrategy +
                ", sqlTypeStrategy=" + sqlTypeStrategy +
                ", scalarTypeStrategy=" + scalarTypeStrategy +
                '}';
    }
}
