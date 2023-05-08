package org.babyfish.jimmer.sql.runtime;

import java.util.Objects;

public final class SqlFormatter {

    public static final SqlFormatter SINGLE_LINE =
            new SqlFormatter(
                    false,
                    "",
                    Integer.MAX_VALUE
            );

    public static final SqlFormatter MULTIPLE_LINE =
            new SqlFormatter(
                    true,
                    "    ",
                    8
            );

    private final boolean multipleLines;

    private final String indent;

    private final int maxValueCountInLine;

    public SqlFormatter(boolean multipleLines, String indent, int maxValueCountInLine) {
        this.multipleLines = multipleLines;
        this.indent = indent;
        this.maxValueCountInLine = maxValueCountInLine;
    }

    public boolean isMultipleLines() {
        return multipleLines;
    }

    public String getIndent() {
        return indent;
    }

    public int getMaxValueCountInLine() {
        return maxValueCountInLine;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                multipleLines,
                indent,
                maxValueCountInLine
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SqlFormatter)) return false;
        SqlFormatter that = (SqlFormatter) o;
        return multipleLines == that.multipleLines &&
                maxValueCountInLine == that.maxValueCountInLine &&
                indent.equals(that.indent);
    }

    @Override
    public String toString() {
        return "SqlFormatter{" +
                "multipleLines=" + multipleLines +
                ", indent='" + indent + '\'' +
                ", maxParameterCountInOneLine=" + maxValueCountInLine +
                '}';
    }
}
