package org.babyfish.jimmer.sql.runtime;

import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class PrettySqlAppender {

    static final int DEFAULT_MAX_VARIABLE_LENGTH = 100;

    private static final Comment DEAULT_COMMENT = new Comment(DEFAULT_MAX_VARIABLE_LENGTH);

    public abstract void append(
            StringBuilder builder,
            String sql,
            List<Object> variables,
            List<Integer> variablePositions
    );

    public static PrettySqlAppender comment(int maxVariableLength) {
        return maxVariableLength == DEFAULT_MAX_VARIABLE_LENGTH ?
                DEAULT_COMMENT :
                new Comment(maxVariableLength);
    }

    public static PrettySqlAppender inline() {
        return Inline.INSTANCE;
    }

    private static class Comment extends PrettySqlAppender {

        private final int maxVariableContentLength;

        Comment(int maxVariableContentLength) {
            this.maxVariableContentLength = Math.max(maxVariableContentLength, 10);
        }

        @Override
        public void append(
                StringBuilder builder,
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
            if (variable instanceof DbLiteral) {
                ((DbLiteral) variable).renderToComment(builder);
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

        @Override
        public int hashCode() {
            return maxVariableContentLength;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Comment comment = (Comment) o;

            return maxVariableContentLength == comment.maxVariableContentLength;
        }

        @Override
        public String toString() {
            return "Comment{" +
                    "maxVariableContentLength=" + maxVariableContentLength +
                    '}';
        }
    }

    private static class Inline extends PrettySqlAppender {

        static final Inline INSTANCE = new Inline();

        private static final Map<Class<?>, VariableAppender<?>> APPENDER_MAP;

        private static final DateTimeFormatter DATE_FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd");

        private static final DateTimeFormatter DATE_TIME_FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        private static final VariableAppender<?> ANY_APPENDER =
                new VariableAppender<Object>() {
                    @Override
                    public void append(StringBuilder builder, Object variable) {
                        builder.append('\'').append(variable).append('\'');
                    }
                };

        @Override
        public void append(
                StringBuilder builder,
                String sql,
                List<Object> variables,
                List<Integer> variablePositions
        ) {
            int cloneFrom = 0;
            int paramIndex = 0;
            for (int index : variablePositions) {
                builder.append(sql, cloneFrom, index - 1);
                cloneFrom = index;
                appendVariable(builder, variables.get(paramIndex++));
            }
            int len = sql.length();
            if (cloneFrom < len) {
                builder.append(sql, cloneFrom, len);
            }
        }

        @SuppressWarnings("unchecked")
        private static void appendVariable(
                StringBuilder builder,
                Object variable
        ) {
            if (variable instanceof DbLiteral) {
                ((DbLiteral)variable).renderValue(builder);
            }
            VariableAppender<?> appender = APPENDER_MAP.get(variable.getClass());
            if (appender == null) {
                appender = ANY_APPENDER;
            }
            ((VariableAppender<Object>)appender).append(builder, variable);
        }

        @Override
        public int hashCode() {
            return 2;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Inline;
        }

        @Override
        public String toString() {
            return "Inline";
        }

        private interface VariableAppender<T> {
            void append(StringBuilder builder, T variable);
        }

        private static class StringAppender implements VariableAppender<String> {

            @Override
            public void append(StringBuilder builder, String variable) {
                builder
                        .append('\'')
                        .append(variable.replace("'", "''"))
                        .append('\'');
            }
        }

        private static class BooleanAppender implements VariableAppender<Boolean> {

            @Override
            public void append(StringBuilder builder, Boolean variable) {
                builder.append(variable ? 1 : 0);
            }
        }

        private static class CharAppender implements VariableAppender<Character> {

            @Override
            public void append(StringBuilder builder, Character variable) {
                builder.append('\'').append(variable.charValue()).append('\'');
            }
        }

        private static class ByteAppender implements VariableAppender<Byte> {

            @Override
            public void append(StringBuilder builder, Byte variable) {
                builder.append(variable.byteValue());
            }
        }

        private static class ShortAppender implements VariableAppender<Short> {

            @Override
            public void append(StringBuilder builder, Short variable) {
                builder.append(variable.shortValue());
            }
        }

        private static class IntAppender implements VariableAppender<Integer> {

            @Override
            public void append(StringBuilder builder, Integer variable) {
                builder.append(variable.intValue());
            }
        }

        private static class LongAppender implements VariableAppender<Long> {

            @Override
            public void append(StringBuilder builder, Long variable) {
                builder.append(variable.longValue());
            }
        }

        private static class FloatAppender implements VariableAppender<Float> {

            @Override
            public void append(StringBuilder builder, Float variable) {
                builder.append(variable.floatValue());
            }
        }

        private static class DoubleAppender implements VariableAppender<Double> {

            @Override
            public void append(StringBuilder builder, Double variable) {
                builder.append(variable.doubleValue());
            }
        }

        private static class DateAppender implements VariableAppender<Date> {

            @Override
            public void append(StringBuilder builder, Date variable) {
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:hh:ss");
                builder.append('\'').append(format.format(variable)).append('\'');
            }
        }

        private static class SqlDateAppender implements VariableAppender<java.sql.Date> {

            @Override
            public void append(StringBuilder builder, java.sql.Date variable) {
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                builder.append('\'').append(format.format(variable)).append('\'');
            }
        }

        private static class SqlTimeAppender implements VariableAppender<java.sql.Time> {

            @Override
            public void append(StringBuilder builder, Time variable) {
                DateFormat format = new SimpleDateFormat("HH:mm:ss");
                builder.append('\'').append(format.format(variable)).append('\'');
            }
        }

        private static class LocalDateAppender implements VariableAppender<LocalDate> {

            @Override
            public void append(StringBuilder builder, LocalDate variable) {
                builder.append('\'').append(DATE_FORMATTER.format(variable)).append('\'');
            }
        }

        private static class LocalDateTimeAppender implements VariableAppender<LocalDateTime> {

            @Override
            public void append(StringBuilder builder, LocalDateTime variable) {
                builder.append('\'').append(DATE_TIME_FORMATTER.format(variable)).append('\'');
            }
        }

        private static class OffsetDateTimeAppender implements VariableAppender<OffsetDateTime> {

            @Override
            public void append(StringBuilder builder, OffsetDateTime variable) {
                builder.append('\'').append(DATE_TIME_FORMATTER.format(variable)).append('\'');
            }
        }

        private static class ZonedDateTimeAppender implements VariableAppender<ZonedDateTime> {

            @Override
            public void append(StringBuilder builder, ZonedDateTime variable) {
                builder.append('\'').append(DATE_TIME_FORMATTER.format(variable)).append('\'');
            }
        }

        private static class ByteArrayAppender implements VariableAppender<byte[]> {

            private static final char[] CHARS = {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'A', 'B','C', 'D', 'E', 'F',
            };

            @Override
            public void append(StringBuilder builder, byte[] variable) {
                builder.append("0x");
                for (byte b : variable) {
                    builder.append(CHARS[(b >>> 4) & 0xF]);
                    builder.append(CHARS[b & 0xF]);
                }
            }
        }

        static {
            Map<Class<?>, VariableAppender<?>> map = new HashMap<>();
            map.put(String.class, new StringAppender());
            map.put(Boolean.class, new BooleanAppender());
            map.put(Character.class, new CharAppender());
            map.put(Byte.class, new ByteAppender());
            map.put(Short.class, new ShortAppender());
            map.put(Integer.class, new IntAppender());
            map.put(Long.class, new LongAppender());
            map.put(Float.class, new FloatAppender());
            map.put(Double.class, new DoubleAppender());
            map.put(Date.class, new DateAppender());
            map.put(java.sql.Date.class, new SqlDateAppender());
            map.put(java.sql.Time.class, new SqlTimeAppender());
            map.put(LocalDate.class, new LocalDateAppender());
            map.put(LocalDateTime.class, new LocalDateTimeAppender());
            map.put(OffsetDateTime.class, new OffsetDateTimeAppender());
            map.put(ZonedDateTime.class, new ZonedDateTimeAppender());
            map.put(byte[].class, new ByteArrayAppender());
            APPENDER_MAP = map;
        }
    }
}
