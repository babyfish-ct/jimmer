package org.babyfish.jimmer.sql.runtime;

import java.util.Objects;

public final class SqlFormatter {

    public static final SqlFormatter SINGLE_LINE =
            new SqlFormatter(
                    false,
                    "",
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE
            );

    public static final SqlFormatter MULTIPLE_LINE =
            new SqlFormatter(
                    true,
                    "    ",
                    3,
                    8
            );

    private final boolean multipleLines;

    private final String indent;

    private final int maxColumnCountInOneLine;

    private final int maxParameterCountInOneLine;

    public SqlFormatter(boolean multipleLines, String indent, int maxColumnCountInOneLine, int maxParameterCountInOneLine) {
        this.multipleLines = multipleLines;
        this.indent = indent;
        this.maxColumnCountInOneLine = maxColumnCountInOneLine;
        this.maxParameterCountInOneLine = maxParameterCountInOneLine;
    }

    public boolean isMultipleLines() {
        return multipleLines;
    }

    public String getIndent() {
        return indent;
    }

    public int getMaxColumnCountInOneLine() {
        return maxColumnCountInOneLine;
    }

    public int getMaxParameterCountInOneLine() {
        return maxParameterCountInOneLine;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                multipleLines,
                indent,
                maxColumnCountInOneLine,
                maxParameterCountInOneLine
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SqlFormatter)) return false;
        SqlFormatter that = (SqlFormatter) o;
        return multipleLines == that.multipleLines &&
                maxColumnCountInOneLine == that.maxColumnCountInOneLine &&
                maxParameterCountInOneLine == that.maxParameterCountInOneLine &&
                indent.equals(that.indent);
    }

    @Override
    public String toString() {
        return "SqlFormatter{" +
                "multipleLines=" + multipleLines +
                ", indent='" + indent + '\'' +
                ", maxColumnCountInOneLine=" + maxColumnCountInOneLine +
                ", maxParameterCountInOneLine=" + maxParameterCountInOneLine +
                '}';
    }
}
