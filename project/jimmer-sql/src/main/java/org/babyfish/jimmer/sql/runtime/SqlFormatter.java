package org.babyfish.jimmer.sql.runtime;

import java.util.Objects;

public final class SqlFormatter {

    public static final SqlFormatter SIMPLE =
            new SqlFormatter(false, "");

    public static final SqlFormatter PRETTY =
            new SqlFormatter(true, "    ");

    private final boolean isPretty;

    private final String indent;

    public SqlFormatter(boolean isPretty, String indent) {
        this.isPretty = isPretty;
        this.indent = indent;
    }

    public boolean isPretty() {
        return isPretty;
    }

    public String getIndent() {
        return indent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isPretty, indent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SqlFormatter)) return false;
        SqlFormatter that = (SqlFormatter) o;
        return isPretty == that.isPretty &&
                indent.equals(that.indent);
    }

    @Override
    public String toString() {
        return "SqlFormatter{" +
                "isPretty=" + isPretty +
                ", indent='" + indent + '\'' +
                '}';
    }
}
