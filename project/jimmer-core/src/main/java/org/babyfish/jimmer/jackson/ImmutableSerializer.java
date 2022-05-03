package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.babyfish.jimmer.meata.ImmutableProp;
import org.babyfish.jimmer.meata.ImmutableType;
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
            if (immutable.__isLoaded(prop.getName())) {
                Object value = immutable.__get(prop.getName());
                if ((prop.isAssociation() || prop.isScalarList()) && value != null) {
                    gen.writeFieldName(prop.getName());
                    TypeSerializer typeSer = null;
                    if (!prop.isEntityList() &&
                            value instanceof ImmutableSpi &&
                            ((ImmutableSpi)value).__type() != immutableType) {
                        typeSer = provider.findTypeSerializer(Utils.getJacksonType(prop));
                    }
                    if (typeSer != null) {
                        provider.findValueSerializer(value.getClass()).serializeWithType(value, gen, provider, typeSer);
                    } else {
                        provider.findValueSerializer(Utils.getJacksonType(prop)).serialize(value, gen, provider);
                    }
                } else {
                    provider.defaultSerializeField(prop.getName(), value, gen);
                }
            }
        }
    }
}
