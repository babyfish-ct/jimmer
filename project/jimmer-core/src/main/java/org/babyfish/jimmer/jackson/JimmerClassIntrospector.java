package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AccessorNamingStrategy;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.POJOPropertiesCollector;

public class JimmerClassIntrospector extends BasicClassIntrospector {
    @Override
    protected POJOPropertiesCollector constructPropertyCollector(MapperConfig<?> config, AnnotatedClass classDef, JavaType type, boolean forSerialization, AccessorNamingStrategy accNaming) {
        return new JimmerPOJOPropertiesCollector(config, forSerialization, type, classDef, accNaming);
    }
}
