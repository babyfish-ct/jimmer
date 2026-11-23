package org.babyfish.jimmer.jackson.v3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import org.babyfish.jimmer.json.DtoJsonBinding;

class DtoPropertyWriterV3 extends BeanPropertyWriter {

    private final DtoJsonBinding.Property prop;

    public DtoPropertyWriterV3(BeanPropertyWriter base, DtoJsonBinding.Property prop) {
        super(base);
        this.prop = prop;
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext ctx) throws Exception {
        if (prop.isLoaded(bean)) {
            super.serializeAsProperty(bean, gen, ctx);
        }
    }

    @Override
    public void serializeAsElement(Object bean, JsonGenerator gen, SerializationContext ctx) throws Exception {
        if (prop.isLoaded(bean)) {
            super.serializeAsElement(bean, gen, ctx);
        }
    }
}
