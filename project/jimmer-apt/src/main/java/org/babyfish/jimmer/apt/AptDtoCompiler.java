package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.dto.compiler.DtoCompiler;
import org.babyfish.jimmer.sql.GeneratedValue;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AptDtoCompiler extends DtoCompiler<ImmutableType, ImmutableProp> {

    protected AptDtoCompiler(ImmutableType baseType, String dtoFilePath) {
        super(baseType, dtoFilePath);
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

    @Override
    protected boolean isGeneratedValue(ImmutableProp baseProp) {
        return baseProp.toElement().getAnnotation(GeneratedValue.class) != null;
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
        for (Element childElement : ((TypeElement)element).getEnclosedElements()) {
            if (childElement.getKind() == ElementKind.ENUM_CONSTANT) {
                constants.add(childElement.getSimpleName().toString());
            }
        }
        return constants;
    }
}
