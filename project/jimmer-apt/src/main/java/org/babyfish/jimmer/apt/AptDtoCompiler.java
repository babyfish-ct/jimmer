package org.babyfish.jimmer.apt;

import org.babyfish.jimmer.apt.meta.ImmutableProp;
import org.babyfish.jimmer.apt.meta.ImmutableType;
import org.babyfish.jimmer.dto.compiler.DtoCompiler;

import java.util.Collection;
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
}
