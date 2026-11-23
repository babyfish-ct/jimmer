package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import org.babyfish.jimmer.json.DtoJsonBinding;

class DtoPropertyWriterV2 extends BeanPropertyWriter {

    private final DtoJsonBinding.Property prop;

    public DtoPropertyWriterV2(BeanPropertyWriter base, DtoJsonBinding.Property prop) {
        super(base);
        this.prop = prop;
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        if (prop.isLoaded(bean)) {
            super.serializeAsField(bean, gen, prov);
        }
    }

    @Override
    public void serializeAsElement(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        if (prop.isLoaded(bean)) {
            super.serializeAsElement(bean, gen, prov);
        }
    }
}
