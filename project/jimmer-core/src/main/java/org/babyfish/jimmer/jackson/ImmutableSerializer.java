package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.io.IOException;

public class ImmutableSerializer extends StdSerializer<ImmutableSpi> {

    private ImmutableType immutableType;

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

    private void serializeFields(ImmutableSpi immutable, JsonGenerator gen, SerializerProvider provider) throws IOException {
        for (ImmutableProp prop : immutableType.getProps().values()) {
            if (immutable.__isLoaded(prop.getId())) {
                Object value = immutable.__get(prop.getId());
                if ((prop.isAssociation(TargetLevel.OBJECT) || prop.isScalarList()) && value != null) {
                    gen.writeFieldName(prop.getName());
                    TypeSerializer typeSer = null;
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
                    provider.defaultSerializeField(prop.getName(), value, gen);
                }
            }
        }
    }
}
