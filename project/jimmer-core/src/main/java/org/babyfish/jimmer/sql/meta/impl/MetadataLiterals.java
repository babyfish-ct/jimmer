package org.babyfish.jimmer.sql.meta.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.jackson.impl.JacksonUtils;

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

public class MetadataLiterals {

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new ImmutableModule());

    private static final Map<Class<?>, Function<String, Object>> DEFAULT_VALUE_PARSER_MAP;

    private MetadataLiterals() {}

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
            return OBJECT_MAPPER.readValue(value, JacksonUtils.getJacksonType(type));
        } catch (JsonProcessingException ex) {
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
        map.put(UUID.class, UUID::fromString);
        map.put(java.sql.Date.class, it -> new java.sql.Date(("now".equals(it) ? new Date() : parseDate(it, "yyyy-MM-dd")).getTime()));
        map.put(java.sql.Time.class, it -> new java.sql.Time(("now".equals(it) ? new Date() : parseDate(it, "HH:mm:ss")).getTime()));
        map.put(Date.class, it -> "now".equals(it) ? new Date() : parseDate(it, "yyyy-MM-dd HH:mm:ss"));
        map.put(LocalTime.class, it -> "now".equals(it) ? LocalTime.now() : LocalTime.parse(it, TIME_FORMATTER));
        map.put(LocalDate.class, it -> "now".equals(it) ? LocalDate.now() : LocalDate.parse(it, DATE_FORMATTER));
        map.put(LocalDateTime.class, it -> "now".equals(it) ? LocalDateTime.now() : LocalDateTime.parse(it, DATE_TIME_FORMATTER));
        map.put(OffsetDateTime.class, it -> "now".equals(it) ? OffsetDateTime.now() : OffsetDateTime.parse(it, DATE_TIME_FORMATTER));
        map.put(ZonedDateTime.class, it -> "now".equals(it) ? ZonedDateTime.now() : ZonedDateTime.parse(it, DATE_TIME_FORMATTER));
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
