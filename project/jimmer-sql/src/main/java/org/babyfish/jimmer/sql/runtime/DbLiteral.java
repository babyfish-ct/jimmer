package org.babyfish.jimmer.sql.runtime;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public interface DbLiteral {

    Class<?> getType();

    default void render(StringBuilder builder) {
        builder.append('?');
    }

    void renderValue(StringBuilder builder);

    void renderToComment(StringBuilder builder);

    void setParameter(PreparedStatement stmt, int index, int jdbcType) throws SQLException;

    class DbNull implements DbLiteral {

        private final Class<?> type;

        public DbNull(Class<?> type) {
            this.type = type;
        }

        public Class<?> getType() {
            return type;
        }

        @Override
        public void renderValue(StringBuilder builder) {
            builder.append("null");
        }

        @Override
        public void renderToComment(StringBuilder builder) {
            builder.append("<null: ").append(type.getSimpleName()).append('>');
        }

        @Override
        public void setParameter(PreparedStatement stmt, int index, int jdbcType) throws SQLException {
            stmt.setNull(index, jdbcType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DbNull dbNull = (DbNull) o;
            return type.equals(dbNull.type);
        }

        @Override
        public String toString() {
            return "DbNull{" +
                    "type=" + type +
                    '}';
        }
    }

    class JsonWithSuffix implements DbLiteral {

        private final Object value;

        private final String suffix;

        public JsonWithSuffix(Object value, String suffix) {
            this.value = value;
            this.suffix = suffix;
        }

        @Override
        public Class<?> getType() {
            return value.getClass();
        }

        @Override
        public void render(StringBuilder builder) {
            builder.append("? ").append(suffix);
        }

        @Override
        public void renderValue(StringBuilder builder) {
            builder.append('"').append(value.toString().replace("'", "''")).append(' ').append(suffix);
        }

        @Override
        public void renderToComment(StringBuilder builder) {
            builder.append(value).append(' ').append(suffix);
        }

        @Override
        public void setParameter(PreparedStatement stmt, int index, int jdbcType) throws SQLException {
            stmt.setString(index, value.toString());
        }

        @Override
        public int hashCode() {
            int result = value.hashCode();
            result = 31 * result + suffix.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JsonWithSuffix that = (JsonWithSuffix) o;

            if (!value.equals(that.value)) return false;
            return suffix.equals(that.suffix);
        }

        @Override
        public String toString() {
            return "JsonWithSuffix{" +
                    "value=" + value +
                    ", suffix='" + suffix + '\'' +
                    '}';
        }
    }
}


