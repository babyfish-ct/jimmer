package org.babyfish.jimmer.json;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

final class JsonFormats {

    private JsonFormats() {}

    static Object encode(JsonFormat format, Type type, Object value, ModelJson.Conversion fallback) {
        if (format != null && value != null) {
            if (format.shape() == JsonFormat.Shape.STRING && value instanceof Number) {
                return value.toString();
            }
            String pattern = format.pattern();
            if (!pattern.isEmpty()) {
                if (value instanceof Date) {
                    return dateFormat(pattern).format((Date) value);
                }
                if (value instanceof LocalDate) {
                    return ((LocalDate) value).format(DateTimeFormatter.ofPattern(pattern));
                }
                if (value instanceof LocalDateTime) {
                    return ((LocalDateTime) value).format(DateTimeFormatter.ofPattern(pattern));
                }
            }
        }
        return ModelJson.encode(type, value, fallback);
    }

    static Object decode(JsonFormat format, Type type, Object value, ModelJson.Conversion fallback) {
        if (format != null && value instanceof String) {
            String text = (String) value;
            if (format.shape() == JsonFormat.Shape.STRING) {
                if (type == byte.class || type == Byte.class) return Byte.valueOf(text);
                if (type == short.class || type == Short.class) return Short.valueOf(text);
                if (type == int.class || type == Integer.class) return Integer.valueOf(text);
                if (type == long.class || type == Long.class) return Long.valueOf(text);
                if (type == float.class || type == Float.class) return Float.valueOf(text);
                if (type == double.class || type == Double.class) return Double.valueOf(text);
                if (type == BigInteger.class) return new BigInteger(text);
                if (type == BigDecimal.class) return new BigDecimal(text);
            }
            String pattern = format.pattern();
            if (!pattern.isEmpty()) {
                if (type == Date.class) {
                    ParsePosition position = new ParsePosition(0);
                    Date result = dateFormat(pattern).parse(text, position);
                    if (result == null || position.getIndex() != text.length()) {
                        throw new IllegalArgumentException("Invalid date \"" + text + "\" for pattern \"" + pattern + "\"");
                    }
                    return result;
                }
                if (type == LocalDate.class) {
                    return LocalDate.parse(text, DateTimeFormatter.ofPattern(pattern));
                }
                if (type == LocalDateTime.class) {
                    return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
                }
            }
        }
        return ModelJson.decode(type, value, fallback);
    }

    private static SimpleDateFormat dateFormat(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }
}
