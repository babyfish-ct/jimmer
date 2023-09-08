package org.babyfish.jimmer.sql.runtime;

import java.util.List;
import java.util.Objects;

public final class SqlFormatter {

    public static final SqlFormatter SIMPLE =
            new SqlFormatter();

    public static final SqlFormatter PRETTY =
            new SqlFormatter("    ", 10, PrettySqlAppender.DEFAULT_MAX_VARIABLE_LENGTH);

    public static final SqlFormatter INLINE_PRETTY =
            new SqlFormatter("    ", 10, -1);

    private final PrettySqlAppender prettySqlAppender;

    private final String indent;

    private final int listParamCountInLine;

    protected SqlFormatter() {
        this.indent = "";
        this.listParamCountInLine = 10;
        this.prettySqlAppender = null;
    }

    protected SqlFormatter(
            String indent,
            int listParamCountInLine,
            int maxVariableContentLength
    ) {
        this.indent = indent != null ? indent : "";
        this.listParamCountInLine = Math.max(listParamCountInLine, 1);
        if (maxVariableContentLength == -1) {
            this.prettySqlAppender = PrettySqlAppender.inline();
        } else {
            this.prettySqlAppender = PrettySqlAppender.comment(Math.max(maxVariableContentLength, 10));
        }
    }

    public static SqlFormatter pretty(String indent, int listParamCountInLine, int maxVariableContentLength) {
        return new SqlFormatter(indent, listParamCountInLine, Math.max(maxVariableContentLength, 10));
    }

    public static SqlFormatter inlinePretty(String indent, int listParamCountInLine) {
        return new SqlFormatter(indent, listParamCountInLine, -1);
    }

    public boolean isPretty() {
        return prettySqlAppender != null;
    }

    public String getIndent() {
        return indent;
    }

    public int getListParamCountInLine() {
        return listParamCountInLine;
    }

    public void append(StringBuilder builder, String sql, List<Object> variables, List<Integer> variablePositions) {
        if (prettySqlAppender == null) {
            builder.append(sql);
        }
        prettySqlAppender.append(builder, sql, variables, variablePositions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SqlFormatter that = (SqlFormatter) o;

        if (listParamCountInLine != that.listParamCountInLine) return false;
        if (!Objects.equals(prettySqlAppender, that.prettySqlAppender)) {
            return false;
        }
        return indent.equals(that.indent);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(prettySqlAppender);
        result = 31 * result + indent.hashCode();
        result = 31 * result + listParamCountInLine;
        return result;
    }

    @Override
    public String toString() {
        return "SqlFormatter{" +
                "prettySqlAppender=" + prettySqlAppender +
                ", indent='" + indent + '\'' +
                ", listParamCountInLine=" + listParamCountInLine +
                '}';
    }
}
