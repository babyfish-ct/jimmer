package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import org.babyfish.jimmer.meta.ImmutableType;

import java.util.Map;

public class JimmerPOJOPropertiesCollector extends POJOPropertiesCollector {
    protected JimmerPOJOPropertiesCollector(MapperConfig<?> config, boolean forSerialization, JavaType type, AnnotatedClass classDef, AccessorNamingStrategy accessorNaming) {
        super(config, forSerialization, type, classDef, accessorNaming);
    }


    @Override
    protected void _addGetterMethod(Map<String, POJOPropertyBuilder> props, AnnotatedMethod m, AnnotationIntrospector ai) {
        super._addGetterMethod(props, m, ai);
        Class<?> clazz = m.getDeclaringClass();
        if (ImmutableType.tryGet(clazz) != null) {
            String implName = _accessorNaming.findNameForRegularGetter(m, m.getName());
            if(implName == null) {
                implName = _accessorNaming.findNameForIsGetter(m, m.getName());
            }
            if (implName != null) {
                return;
            }
            _property(props, m.getName()).addGetter(m, null, false, true, false);
        }
    }
}
