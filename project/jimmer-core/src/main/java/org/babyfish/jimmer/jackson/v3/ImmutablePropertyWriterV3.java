package org.babyfish.jimmer.jackson.v3;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;

class ImmutablePropertyWriterV3 extends BeanPropertyWriter {

    private final PropId propId;

    public ImmutablePropertyWriterV3(BeanPropertyWriter base, PropId propId) {
        super(base);
        this.propId = propId;
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext ctx) throws Exception {
        ImmutableSpi spi = (ImmutableSpi) bean;
        if (spi.__isLoaded(propId) && spi.__isVisible(propId)) {
            super.serializeAsProperty(bean, gen, ctx);
        }
    }

    @Override
    public void serializeAsElement(Object bean, JsonGenerator gen, SerializationContext ctx) throws Exception {
        ImmutableSpi spi = (ImmutableSpi) bean;
        if (spi.__isLoaded(propId) && spi.__isVisible(propId)) {
            super.serializeAsElement(bean, gen, ctx);
        }
    }
}
