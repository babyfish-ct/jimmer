package org.babyfish.jimmer.sql.runtime;

import java.util.Objects;

public final class SqlFormatter {

    public static final SqlFormatter SINGLE_LINE =
            new SqlFormatter(false, "");

    public static final SqlFormatter MULTIPLE_LINE =
            new SqlFormatter(true, "    ");

    private final boolean multipleLines;

    private final String indent;

    public SqlFormatter(boolean multipleLines, String indent) {
        this.multipleLines = multipleLines;
        this.indent = indent;
    }

    public boolean isMultipleLines() {
        return multipleLines;
    }

    public String getIndent() {
        return indent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(multipleLines, indent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SqlFormatter)) return false;
        SqlFormatter that = (SqlFormatter) o;
        return multipleLines == that.multipleLines &&
                indent.equals(that.indent);
    }

    @Override
    public String toString() {
        return "SqlFormatter{" +
                "multipleLines=" + multipleLines +
                ", indent='" + indent + '\'' +
                '}';
    }
}
