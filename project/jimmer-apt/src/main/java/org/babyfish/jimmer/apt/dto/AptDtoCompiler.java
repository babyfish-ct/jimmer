package org.babyfish.jimmer.apt.dto;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;
import org.babyfish.jimmer.dto.compiler.*;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class AptDtoCompiler extends DtoCompiler<ImmutableType, ImmutableProp> {

    private static final Map<TypeName, SimplePropType> SIMPLE_PROP_TYPE_MAP;

    private final Elements elements;

    private final DtoModifier defaultNullableInputModifier;

    public AptDtoCompiler(DtoFile dtoFile, Elements elements, DtoModifier defaultNullableInputModifier) throws IOException {
        super(dtoFile);
        this.elements = elements;
        this.defaultNullableInputModifier = defaultNullableInputModifier;
    }

    @Override
    public DtoModifier getDefaultNullableInputModifier() {
        return defaultNullableInputModifier;
    }

    @Override
    protected Collection<ImmutableType> getSuperTypes(ImmutableType baseType) {
        return baseType.getSuperTypes();
    }

    @Override
    protected Map<String, ImmutableProp> getDeclaredProps(ImmutableType baseType) {
        return baseType.getDeclaredProps();
    }

    @Override
    protected Map<String, ImmutableProp> getProps(ImmutableType baseType) {
        return baseType.getProps();
    }

    @Override
    protected ImmutableType getTargetType(ImmutableProp baseProp) {
        return baseProp.getTargetType();
    }

    @Nullable
    @Override
    protected ImmutableProp getIdProp(ImmutableType baseType) {
        return baseType.getIdProp();
    }

    @Override
    protected boolean isGeneratedValue(ImmutableProp baseProp) {
        return baseProp.toElement().getAnnotation(GeneratedValue.class) != null;
    }

    @Override
    protected SimplePropType getSimplePropType(ImmutableProp baseProp) {
        TypeName typeName = baseProp.getTypeName();
        SimplePropType simplePropType = SIMPLE_PROP_TYPE_MAP.get(typeName);
        if (simplePropType == null) {
            return SimplePropType.NONE;
        }
        return simplePropType;
    }

    @Override
    protected SimplePropType getSimplePropType(PropConfig.PathNode<ImmutableProp> pathNode) {
        TypeName typeName = pathNode.isAssociatedId() ?
                pathNode.getProp().getTargetType().getIdProp().getTypeName() :
                pathNode.getProp().getTypeName();
        SimplePropType simplePropType = SIMPLE_PROP_TYPE_MAP.get(typeName);
        if (simplePropType == null) {
            return SimplePropType.NONE;
        }
        return simplePropType;
    }

    @Override
    protected boolean isSameType(ImmutableProp baseProp1, ImmutableProp baseProp2) {
        return baseProp1.getClientTypeName().equals(baseProp2.getClientTypeName());
    }

    @Override
    protected List<String> getEnumConstants(ImmutableProp baseProp) {
        if (baseProp.isList() || !baseProp.context().isEnum(baseProp.getElementType())) {
            return null;
        }
        Element element = ((DeclaredType)baseProp.toElement().getReturnType()).asElement();
        if (!(element instanceof TypeElement)) {
            return null;
        }
        List<String> constants = new ArrayList<>();
        for (Element childElement : element.getEnclosedElements()) {
            if (childElement.getKind() == ElementKind.ENUM_CONSTANT) {
                constants.add(childElement.getSimpleName().toString());
            }
        }
        return constants;
    }

    @Override
    protected Integer getGenericTypeCount(String qualifiedName) {
        TypeElement typeElement = elements.getTypeElement(qualifiedName);
        if (typeElement == null) {
            return null;
        }
        return typeElement.getTypeParameters().size();
    }

    static {
        Map<TypeName, SimplePropType> map = new HashMap<>();

        map.put(TypeName.BOOLEAN, SimplePropType.BOOLEAN);
        map.put(TypeName.BOOLEAN.box(), SimplePropType.BOOLEAN);
        map.put(TypeName.BYTE, SimplePropType.BYTE);
        map.put(TypeName.BYTE.box(), SimplePropType.BYTE);
        map.put(TypeName.SHORT, SimplePropType.SHORT);
        map.put(TypeName.SHORT.box(), SimplePropType.SHORT);
        map.put(TypeName.INT, SimplePropType.INT);
        map.put(TypeName.INT.box(), SimplePropType.INT);
        map.put(TypeName.LONG, SimplePropType.LONG);
        map.put(TypeName.LONG.box(), SimplePropType.LONG);
        map.put(TypeName.FLOAT, SimplePropType.FLOAT);
        map.put(TypeName.FLOAT.box(), SimplePropType.FLOAT);
        map.put(TypeName.DOUBLE, SimplePropType.DOUBLE);
        map.put(TypeName.DOUBLE.box(), SimplePropType.DOUBLE);

        map.put(ClassName.get(BigInteger.class), SimplePropType.BIG_INTEGER);
        map.put(ClassName.get(BigDecimal.class), SimplePropType.BIG_DECIMAL);

        map.put(ClassName.get(String.class), SimplePropType.STRING);

        SIMPLE_PROP_TYPE_MAP = map;
    }
}
