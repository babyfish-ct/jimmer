package org.babyfish.jimmer.json.gson;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.babyfish.jimmer.json.AbstractJsonCodec;
import org.babyfish.jimmer.json.JsonIgnore;
import org.babyfish.jimmer.json.ModelJson;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.*;
import java.util.*;

public class GsonCodec extends AbstractJsonCodec {

    private final Gson mapper;
    private final Gson prettyMapper;

    public GsonCodec() {
        this(new GsonBuilder().disableHtmlEscaping().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL));
    }

    public GsonCodec(GsonBuilder builder) {
        mapper = configure(builder).create();
        prettyMapper = mapper.newBuilder().setPrettyPrinting().create();
    }

    /** Registers Jimmer support on an application-owned builder. */
    public static GsonBuilder configure(GsonBuilder builder) {
        ExclusionStrategy ignore = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipClass(Class<?> type) {
                return false;
            }

            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                JsonIgnore annotation = field.getAnnotation(JsonIgnore.class);
                if (annotation != null) {
                    return annotation.value();
                }
                String name = field.getName();
                String suffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                for (String getter : Arrays.asList("get" + suffix, "is" + suffix, name)) {
                    try {
                        annotation = field.getDeclaringClass().getMethod(getter).getAnnotation(JsonIgnore.class);
                        if (annotation != null) {
                            return annotation.value();
                        }
                    } catch (NoSuchMethodException ex) {
                        // The field has no getter with this naming convention.
                    }
                }
                return false;
            }
        };
        return builder.addSerializationExclusionStrategy(ignore).addDeserializationExclusionStrategy(ignore)
                .registerTypeAdapterFactory(new Factory());
    }

    @Override
    protected Object read(String json, Type type) {
        return type == Object.class ? javaValue(JsonParser.parseString(json)) : mapper.fromJson(json, type);
    }

    @Override
    public void writeTree(Writer target, Object tree) {
        TreeWriter.INSTANCE.toJson(tree, target);
    }

    private static class TreeWriter {
        static final Gson INSTANCE = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create();
    }

    @Override
    protected String write(Object value, Type type, boolean pretty) {
        Gson gson = pretty ? prettyMapper : mapper;
        return type != null ? gson.toJson(value, type) : gson.toJson(value);
    }

    private static Object javaValue(JsonElement element) {
        if (element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> field : element.getAsJsonObject().entrySet()) {
                result.put(field.getKey(), javaValue(field.getValue()));
            }
            return result;
        }
        if (element.isJsonArray()) {
            List<Object> result = new ArrayList<>(element.getAsJsonArray().size());
            for (JsonElement item : element.getAsJsonArray()) {
                result.add(javaValue(item));
            }
            return result;
        }
        JsonPrimitive scalar = element.getAsJsonPrimitive();
        if (scalar.isNumber()) {
            return scalar.getAsBigDecimal();
        }
        return scalar.isBoolean() ? scalar.getAsBoolean() : scalar.getAsString();
    }

    private static class Factory implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> token) {
            Class<?> raw = token.getRawType();
            if (raw == LocalDate.class || raw == LocalDateTime.class || raw == LocalTime.class ||
                    raw == Instant.class || raw == OffsetDateTime.class || raw == OffsetTime.class ||
                    raw == ZonedDateTime.class || raw == Duration.class || raw == Period.class) {
                return temporal(raw);
            }
            Type type = token.getType();
            if (!ModelJson.supports(type)) {
                return null;
            }
            TypeAdapter<JsonElement> elements = gson.getAdapter(JsonElement.class);
            TypeAdapter<Object> treeWriter = gson.getAdapter(Object.class);
            return new TypeAdapter<T>() {
                @Override
                @SuppressWarnings("unchecked")
                public T read(JsonReader in) throws IOException {
                    return (T) ModelJson.decode(type, javaValue(elements.read(in)),
                            (fieldType, value) -> gson.fromJson(gson.toJsonTree(value), fieldType));
                }

                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    Object tree = ModelJson.encode(type, value,
                            (fieldType, item) -> javaValue(gson.toJsonTree(item, fieldType)));
                    boolean serializeNulls = out.getSerializeNulls();
                    out.setSerializeNulls(true);
                    try {
                        treeWriter.write(out, tree);
                    } finally {
                        out.setSerializeNulls(serializeNulls);
                    }
                }
            };
        }

        private static <T> TypeAdapter<T> temporal(Class<?> raw) {
            Method parse;
            try {
                parse = raw.getMethod("parse", CharSequence.class);
            } catch (NoSuchMethodException ex) {
                throw new AssertionError(ex);
            }
            return new TypeAdapter<T>() {
                @Override
                @SuppressWarnings("unchecked")
                public T read(JsonReader in) throws IOException {
                    try {
                        return (T) parse.invoke(null, in.nextString());
                    } catch (ReflectiveOperationException ex) {
                        throw new JsonParseException("Invalid " + raw.getSimpleName(), ex);
                    }
                }

                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    out.value(value.toString());
                }
            }.nullSafe();
        }
    }
}
