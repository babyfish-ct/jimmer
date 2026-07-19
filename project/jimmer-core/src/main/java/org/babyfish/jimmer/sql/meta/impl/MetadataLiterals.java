package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.jackson.codec.JsonCodec;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;

public class MetadataLiterals {

    private final static DateTimeFormatter ZONED_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[XXX][X]")
            .withZone(ZoneId.systemDefault());

    private final static DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final JsonCodec<?> JSON_CODEC = jsonCodec();

    private static final Map<Class<?>, Function<String, Object>> DEFAULT_VALUE_PARSER_MAP;

    private MetadataLiterals() {
    }

    public static Object valueOf(Type type, boolean nullable, String value) {
        if ("null".equals(value)) {
            if (!nullable) {
                throw new IllegalArgumentException("The default value of non-null type cannot be null");
            }
            return null;
        }
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            Function<String, Object> parser = DEFAULT_VALUE_PARSER_MAP.get(clazz);
            if (parser != null) {
                return parser.apply(value);
            }
            if (clazz.isEnum()) {
                for (Object constant : clazz.getEnumConstants()) {
                    Enum<?> enumValue = (Enum<?>) constant;
                    if (enumValue.name().equals(value)) {
                        return enumValue;
                    }
                }
            }
        }
        try {
            return JSON_CODEC.readerFor(tf -> tf.constructType(type)).read(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "The value \"" +
                            value +
                            "\" cannot be parsed as JSON object" +
                            "\"",
                    ex
            );
        }
    }

    static {
        Map<Class<?>, Function<String, Object>> map = new HashMap<>();
        map.put(boolean.class, "true"::equals);
        map.put(char.class, it -> it.charAt(0));
        map.put(byte.class, Byte::parseByte);
        map.put(short.class, Short::parseShort);
        map.put(int.class, Integer::parseInt);
        map.put(long.class, Long::parseLong);
        map.put(float.class, Float::parseFloat);
        map.put(double.class, Double::parseDouble);
        map.put(Boolean.class, "true"::equals);
        map.put(Character.class, it -> it.charAt(0));
        map.put(Byte.class, Byte::parseByte);
        map.put(Short.class, Short::parseShort);
        map.put(Integer.class, Integer::parseInt);
        map.put(Long.class, Long::parseLong);
        map.put(Float.class, Float::parseFloat);
        map.put(Double.class, Double::parseDouble);
        map.put(String.class, it -> it);
        map.put(UUID.class, it -> "randomUUID".equals(it) ? (Supplier<?>) UUID::randomUUID : UUID.fromString(it));
        map.put(java.sql.Date.class, it -> "now".equals(it) ? (Supplier<?>) () -> new java.sql.Date(new Date().getTime()) : new java.sql.Date(parseDate(it, "yyyy-MM-dd").getTime()));
        map.put(java.sql.Time.class, it -> "now".equals(it) ? (Supplier<?>) () -> new java.sql.Time(new Date().getTime()) : new java.sql.Time(parseDate(it, "HH:mm:ss").getTime()));
        map.put(Date.class, it -> "now".equals(it) ? (Supplier<?>) Date::new : parseDate(it, "yyyy-MM-dd HH:mm:ss"));
        map.put(LocalTime.class, it -> "now".equals(it) ? (Supplier<?>) LocalTime::now : TIME_FORMATTER.parse(it, LocalTime::from));
        map.put(LocalDate.class, it -> "now".equals(it) ? (Supplier<?>) LocalDate::now : DATE_FORMATTER.parse(it, LocalDate::from));
        map.put(LocalDateTime.class, it -> "now".equals(it) ? (Supplier<?>) LocalDateTime::now : LOCAL_DATE_TIME_FORMATTER.parse(it, LocalDateTime::from));
        map.put(OffsetDateTime.class, it -> "now".equals(it) ? (Supplier<?>) OffsetDateTime::now : ZONED_DATE_TIME_FORMATTER.parse(it, OffsetDateTime::from));
        map.put(ZonedDateTime.class, it -> "now".equals(it) ? (Supplier<?>) ZonedDateTime::now : ZONED_DATE_TIME_FORMATTER.parse(it, ZonedDateTime::from));
        map.put(Instant.class, it -> "now".equals(it) ? (Supplier<?>) Instant::now : ZONED_DATE_TIME_FORMATTER.parse(it, Instant::from));
        DEFAULT_VALUE_PARSER_MAP = map;
    }

    private static Date parseDate(String value, String format) {
        try {
            return new SimpleDateFormat(format).parse(value);
        } catch (ParseException ex) {
            throw new IllegalArgumentException(
                    "The value \"" +
                            value +
                            "\" cannot be parsed by date/time format \"" +
                            format +
                            "\"",
                    ex
            );
        }
    }
}
