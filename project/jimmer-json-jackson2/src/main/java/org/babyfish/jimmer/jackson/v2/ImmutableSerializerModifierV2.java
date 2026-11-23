package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.babyfish.jimmer.meta.impl.ImmutableProps;
import org.babyfish.jimmer.json.DtoJsonBinding;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ListIterator;

class ImmutableSerializerModifierV2 extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(
            SerializationConfig config,
            BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties
    ) {
        DtoJsonBinding binding = DtoJsonBinding.find(beanDesc.getBeanClass());
        if (binding != null) {
            Map<String, DtoJsonBinding.Property> props = new HashMap<>();
            for (DtoJsonBinding.Property prop : binding.getProperties()) {
                if (prop.writeToJson()) {
                    props.put(prop.getGetterName(), prop);
                }
            }
            ListIterator<BeanPropertyWriter> itr = beanProperties.listIterator();
            while (itr.hasNext()) {
                BeanPropertyWriter writer = itr.next();
                DtoJsonBinding.Property prop = props.get(writer.getMember().getName());
                if (prop == null) {
                    itr.remove();
                } else {
                    itr.set(new DtoPropertyWriterV2(writer, prop));
                }
            }
            return beanProperties;
        }
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
            ImmutableProp prop = ImmutableProps.tryGet(type, method);
            if (prop != null) {
                itr.set(new ImmutablePropertyWriterV2(writer, prop.getId()));
            } else {
                itr.remove();
            }
        }
        return beanProperties;
    }
}
