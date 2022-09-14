package org.babyfish.jimmer.jackson.meta;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotationMap;
import com.fasterxml.jackson.databind.introspect.TypeResolutionContext;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.jackson.PropUtils;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.List;

class BeanMember extends AnnotatedMember {

    private final ImmutableProp prop;

    private final JavaType type;

    BeanMember(TypeResolutionContext ctx, ImmutableProp prop) {
        super(ctx, annotationMap(prop));
        this.prop = prop;
        this.type = PropUtils.getJacksonType(prop);
    }

    @Override
    public Annotated withAnnotations(AnnotationMap fallback) {
        return this;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return prop.getDeclaringType().getJavaClass();
    }

    @Override
    public Member getMember() {
        return null;
    }

    @Override
    public void setValue(Object pojo, Object value) throws UnsupportedOperationException, IllegalArgumentException {
        throw new UnsupportedOperationException("Cannot set immutable property");
    }

    @Override
    public Object getValue(Object pojo) throws UnsupportedOperationException, IllegalArgumentException {
        return ImmutableObjects.get(pojo, prop.getId());
    }

    @Override
    public AnnotatedElement getAnnotated() {
        return null;
    }

    @Override
    protected int getModifiers() {
        return 0;
    }

    @Override
    public String getName() {
        return prop.getName();
    }

    @Override
    public JavaType getType() {
        return null;
    }

    @Override
    public Class<?> getRawType() {
        if (prop.isScalarList() || prop.isReferenceList(TargetLevel.OBJECT)) {
            return List.class;
        }
        return prop.getElementClass();
    }

    @Override
    public int hashCode() {
        return prop.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BeanMember)) {
            return false;
        }
        return prop == ((BeanMember)o).prop;
    }

    @Override
    public String toString() {
        return "BeanMember(" + prop + ")";
    }

    private static AnnotationMap annotationMap(ImmutableProp prop) {
        AnnotationMap map = new AnnotationMap();
        JsonIgnore jsonIgnore = prop.getAnnotation(JsonIgnore.class);
        if (jsonIgnore != null) {
            map = AnnotationMap.merge(map, AnnotationMap.of(JsonIgnore.class, jsonIgnore));
        }
        JsonFormat jsonFormat = prop.getAnnotation(JsonFormat.class);
        if (jsonFormat != null) {
            map = AnnotationMap.merge(map, AnnotationMap.of(JsonFormat.class, jsonFormat));
        }
        return map;
    }
}
