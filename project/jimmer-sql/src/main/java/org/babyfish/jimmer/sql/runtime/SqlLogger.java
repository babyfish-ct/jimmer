package org.babyfish.jimmer.sql.runtime;

import java.util.List;

public abstract class SqlLogger {

    public abstract void append(
            StringBuilder builder,
            String sql,
            List<Object> variables,
            List<Integer> variablePositions,
            int maxVariableContentLength
    );

    private static class Comment extends SqlLogger {

        @Override
        public void append(
                StringBuilder builder,
                String sql,
                List<Object> variables,
                List<Integer> variablePositions,
                int maxVariableContentLength
        ) {
            int cloneFrom = 0;
            int paramIndex = 0;
            for (int index : variablePositions) {
                builder.append(sql, cloneFrom, index);
                cloneFrom = index;
                builder.append(" /* ");
                appendVariable(builder, variables.get(paramIndex++), maxVariableContentLength);
                builder.append(" */");
            }
            int len = sql.length();
            if (cloneFrom < len) {
                builder.append(sql, cloneFrom, len);
            }
        }

        private static void appendVariable(
                StringBuilder builder,
                Object variable,
                int maxVariableContentLength
        ) {
            if (maxVariableContentLength < 10) {
                maxVariableContentLength = 10;
            }
            if (variable instanceof DbNull) {
                builder.append("<null: ").append(((DbNull) variable).getType().getSimpleName()).append('>');
            } else if (variable instanceof String) {
                String text = (String) variable;
                if (text.length() > maxVariableContentLength) {
                    builder.append(text, 0, maxVariableContentLength - 3).append("...");
                } else {
                    builder.append(text);
                }
            } else if (variable instanceof byte[]) {
                byte[] arr = (byte[]) variable;
                int len = arr.length > maxVariableContentLength ? maxVariableContentLength - 3 : arr.length;
                builder.append("bytes[");
                for (int i = 0; i < len; i++) {
                    if (i != 0) {
                        builder.append(", ");
                    }
                    builder.append(arr[i]);
                }
                if (arr.length > maxVariableContentLength) {
                    builder.append(", ...");
                }
                builder.append(']');
            } else {
                builder.append(variable);
            }
        }
    }

    private static class Inline extends SqlLogger {

        @Override
        public void append(
                StringBuilder builder,
                String sql,
                List<Object> variables,
                List<Integer> variablePositions,
                int maxVariableContentLength
        ) {
            int cloneFrom = 0;
            int paramIndex = 0;
            for (int index : variablePositions) {
                builder.append(sql, cloneFrom, index);
                cloneFrom = index;
                builder.append(" /* ");
                appendVariable(builder, variables.get(paramIndex++), maxVariableContentLength);
                builder.append(" */");
            }
            int len = sql.length();
            if (cloneFrom < len) {
                builder.append(sql, cloneFrom, len);
            }
        }

        private static void appendVariable(
                StringBuilder builder,
                Object variable,
                int maxVariableContentLength
        ) {

        }
    }
}
