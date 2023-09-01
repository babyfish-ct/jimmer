package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.dialect.Dialect;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public abstract class SqlLogger {

    public abstract void append(
            StringBuilder builder,
            Dialect dialect,
            String sql,
            List<Object> variables,
            List<Integer> variablePositions
    );

    private static class Comment extends SqlLogger {

        private final int maxVariableContentLength;

        Comment(int maxVariableContentLength) {
            this.maxVariableContentLength = Math.max(maxVariableContentLength, 10);
        }

        @Override
        public void append(
                StringBuilder builder,
                Dialect dialect,
                String sql,
                List<Object> variables,
                List<Integer> variablePositions
        ) {
            int cloneFrom = 0;
            int paramIndex = 0;
            for (int index : variablePositions) {
                builder.append(sql, cloneFrom, index);
                cloneFrom = index;
                builder.append(" /* ");
                appendVariable(builder, variables.get(paramIndex++));
                builder.append(" */");
            }
            int len = sql.length();
            if (cloneFrom < len) {
                builder.append(sql, cloneFrom, len);
            }
        }

        private void appendVariable(
                StringBuilder builder,
                Object variable
        ) {
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
                Dialect dialect,
                String sql,
                List<Object> variables,
                List<Integer> variablePositions
        ) {
            int cloneFrom = 0;
            int paramIndex = 0;
            for (int index : variablePositions) {
                builder.append(sql, cloneFrom, index - 1);
                cloneFrom = index;
            }
            int len = sql.length();
            if (cloneFrom < len) {
                builder.append(sql, cloneFrom, len);
            }
        }

        private static void appendVariable(
                StringBuilder builder,
                Dialect dialect,
                Object variable
        ) {

        }

        private interface VariableAppender<T> {
            void append(StringBuilder builder, Dialect dialect, T variable);
        }

        private static class NullAppender implements VariableAppender<DbNull> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, DbNull variable) {
                builder.append("null");
            }
        }

        private static class StringAppender implements VariableAppender<String> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, String variable) {
                builder
                        .append('\'')
                        .append(variable.replace("'", "''"))
                        .append('\'');
            }
        }

        private static class BooleanAppender implements VariableAppender<Boolean> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, Boolean variable) {
                builder.append(variable ? 1 : 0);
            }
        }

        private static class CharAppender implements VariableAppender<Character> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, Character variable) {
                builder.append('\'').append(variable.charValue()).append('\'');
            }
        }

        private static class ByteAppender implements VariableAppender<Byte> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, Byte variable) {
                builder.append(variable.byteValue());
            }
        }

        private static class ShortAppender implements VariableAppender<Short> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, Short variable) {
                builder.append(variable.shortValue());
            }
        }

        private static class IntAppender implements VariableAppender<Integer> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, Integer variable) {
                builder.append(variable.intValue());
            }
        }

        private static class LongAppender implements VariableAppender<Long> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, Long variable) {
                builder.append(variable.longValue());
            }
        }

        private static class FloatAppender implements VariableAppender<Float> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, Float variable) {
                builder.append(variable.floatValue());
            }
        }

        private static class DoubleAppender implements VariableAppender<Double> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, Double variable) {
                builder.append(variable.doubleValue());
            }
        }

        private static class DateAppender implements VariableAppender<Date> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, Date variable) {
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:hh:ss");
                builder.append('\'').append(format.format(variable)).append('\'');
            }
        }

        private static class SqlDateAppender implements VariableAppender<java.sql.Date> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, java.sql.Date variable) {
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                builder.append('\'').append(format.format(variable)).append('\'');
            }
        }

        private static class SqlTimeAppender implements VariableAppender<java.sql.Time> {

            @Override
            public void append(StringBuilder builder, Dialect dialect, java.sql.Time variable) {
                DateFormat format = new SimpleDateFormat("HH:mm:ss");
                builder.append('\'').append(format.format(variable)).append('\'');
            }
        }
    }
}
