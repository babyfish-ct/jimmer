package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;
import org.babyfish.jimmer.jackson.ImmutableProps;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ListIterator;

class ImmutableSerializerModifierV3 extends ValueSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(
            final SerializationConfig config,
            final BeanDescription.Supplier beanDesc,
            final List<BeanPropertyWriter> beanProperties
    ) {
        ImmutableType type = ImmutableType.tryGet(beanDesc.getBeanClass());
        if (type == null) {
            return beanProperties;
        }
        ListIterator<BeanPropertyWriter> itr = beanProperties.listIterator();
        while (itr.hasNext()) {
            BeanPropertyWriter writer = itr.next();
            Member member = writer.getMember().getMember();
            if (!(member instanceof Method) || member.getName().equals("getDummyPropForJacksonError__")) {
                itr.remove();
                continue;
            }
            Method method = (Method) member;
            ImmutableProp prop = ImmutableProps.get(type, method);
            itr.set(new ImmutablePropertyWriterV3(writer, prop.getId()));
        }
        return beanProperties;
    }
}
