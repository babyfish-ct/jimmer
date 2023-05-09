package org.babyfish.jimmer.sql.runtime;

import java.util.Objects;

public final class SqlFormatter {

    public static final SqlFormatter SIMPLE =
            new SqlFormatter(false, "", Integer.MAX_VALUE, 100);

    public static final SqlFormatter PRETTY =
            new SqlFormatter(true, "    ", 10, 100);

    private final boolean isPretty;

    private final String indent;

    private final int listParamCountInLine;

    private final int maxVariableContentLength;

    public SqlFormatter(boolean isPretty, String indent, int listParamCountInLine, int maxVariableContentLength) {
        this.isPretty = isPretty;
        this.indent = indent != null ? indent : "";
        this.listParamCountInLine = Math.max(listParamCountInLine, 1);
        this.maxVariableContentLength = Math.max(maxVariableContentLength, 10);
    }

    public boolean isPretty() {
        return isPretty;
    }

    public String getIndent() {
        return indent;
    }

    public int getListParamCountInLine() {
        return listParamCountInLine;
    }

    public int getMaxVariableContentLength() {
        return maxVariableContentLength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SqlFormatter)) return false;
        SqlFormatter formatter = (SqlFormatter) o;
        return isPretty == formatter.isPretty && listParamCountInLine == formatter.listParamCountInLine && maxVariableContentLength == formatter.maxVariableContentLength && indent.equals(formatter.indent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isPretty, indent, listParamCountInLine, maxVariableContentLength);
    }

    @Override
    public String toString() {
        return "SqlFormatter{" +
                "isPretty=" + isPretty +
                ", indent='" + indent + '\'' +
                ", listParamCountInLine=" + listParamCountInLine +
                ", maxVariableContentLength=" + maxVariableContentLength +
                '}';
    }
}
