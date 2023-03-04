package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import org.babyfish.jimmer.jackson.meta.BeanProps;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.io.IOException;
import java.time.OffsetDateTime;

public class ImmutableSerializer extends StdSerializer<ImmutableSpi> {

    private final ImmutableType immutableType;

    @SuppressWarnings("unchecked")
    public ImmutableSerializer(ImmutableType immutableType) {
        super((Class<ImmutableSpi>)immutableType.getJavaClass());
        this.immutableType = immutableType;
    }

    @Override
    public void serialize(
            ImmutableSpi value,
            JsonGenerator gen,
            SerializerProvider provider
    ) throws IOException {
        gen.writeStartObject();
        this.serializeFields(value, gen, provider);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(
            ImmutableSpi value,
            JsonGenerator gen,
            SerializerProvider serializers,
            TypeSerializer typeSer
    ) throws IOException {
        typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.START_OBJECT));
        this.serializeFields(value, gen, serializers);
        gen.writeEndObject();
    }

    @SuppressWarnings("unchecked")
    private void serializeFields(ImmutableSpi immutable, JsonGenerator gen, SerializerProvider provider) throws IOException {
        for (ImmutableProp prop : immutableType.getProps().values()) {
            JsonIgnore ignore = prop.getAnnotation(JsonIgnore.class);
            if (ignore != null && ignore.value()) {
                continue;
            }
            if (immutable.__isLoaded(prop.getId()) && immutable.__isVisible(prop.getId())) {
                Object value = immutable.__get(prop.getId());
                if (value != null && prop.getConverter() != null) {
                    value = ((Converter<Object>)prop.getConverter()).output(value);
                }
                if (value == null) {
                    provider.defaultSerializeField(prop.getName(), null, gen);
                } else if (prop.isAssociation(TargetLevel.OBJECT) || prop.isScalarList()) {
                    TypeSerializer typeSer = null;
                    gen.writeFieldName(prop.getName());
                    if (!prop.isReferenceList(TargetLevel.OBJECT) &&
                            value instanceof ImmutableSpi &&
                            ((ImmutableSpi)value).__type() != immutableType) {
                        typeSer = provider.findTypeSerializer(PropUtils.getJacksonType(prop));
                    }
                    if (typeSer != null) {
                        provider.findValueSerializer(value.getClass()).serializeWithType(value, gen, provider, typeSer);
                    } else {
                        provider.findValueSerializer(PropUtils.getJacksonType(prop)).serialize(value, gen, provider);
                    }
                } else {
                    gen.writeFieldName(prop.getName());
                    JsonSerializer<?> serializer = provider.findTypedValueSerializer(
                            prop.getElementClass(),
                            true,
                            BeanProps.get(provider.getTypeFactory(), prop)
                    );
                    if (serializer instanceof DateSerializer) {
                        serializer = ((DateSerializer) serializer).createContextual(
                                provider,
                                BeanProps.get(provider.getTypeFactory(), prop)
                        );
                    } else if (serializer instanceof LocalDateSerializer) {
                        serializer = ((LocalDateSerializer) serializer).createContextual(
                                provider,
                                BeanProps.get(provider.getTypeFactory(), prop)
                        );
                    } else if (serializer instanceof LocalDateTimeSerializer) {
                        serializer = ((LocalDateTimeSerializer) serializer).createContextual(
                                provider,
                                BeanProps.get(provider.getTypeFactory(), prop)
                        );
                    } else if (serializer instanceof OffsetDateTimeSerializer) {
                        serializer = ((OffsetDateTimeSerializer) serializer).createContextual(
                                provider,
                                BeanProps.get(provider.getTypeFactory(), prop)
                        );
                    } else if (serializer instanceof ZonedDateTimeSerializer) {
                        serializer = ((ZonedDateTimeSerializer) serializer).createContextual(
                                provider,
                                BeanProps.get(provider.getTypeFactory(), prop)
                        );
                    }
                    ((JsonSerializer<Object>) serializer).serialize(value, gen, provider);
                }
            }
        }
    }
}
