package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.babyfish.jimmer.jackson.ImmutableProps;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ListIterator;

class ImmutableSerializerModifierV2 extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(
            SerializationConfig config,
            BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties
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
            itr.set(new ImmutablePropertyWriterV2(writer, prop.getId()));
        }
        return beanProperties;
    }
}
