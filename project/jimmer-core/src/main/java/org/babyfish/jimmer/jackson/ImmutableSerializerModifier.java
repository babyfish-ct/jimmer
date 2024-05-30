package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ListIterator;

class ImmutableSerializerModifier extends BeanSerializerModifier {

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
            String methodName = method.getName();
            ImmutableProp prop = null;
            if (method.getReturnType() == boolean.class &&
                    methodName.startsWith("is") &&
                    methodName.length() > 2 &&
                    Character.isUpperCase(methodName.charAt(2))) {
                prop = type.getProps().get(methodName);
            }
            if (prop == null) {
                String propName = StringUtil.propName(methodName, method.getReturnType() == boolean.class);
                prop = type.getProp(propName);
            }
            itr.set(new ImmutablePropertyWriter(writer, prop.getId()));
        }
        return beanProperties;
    }
}
