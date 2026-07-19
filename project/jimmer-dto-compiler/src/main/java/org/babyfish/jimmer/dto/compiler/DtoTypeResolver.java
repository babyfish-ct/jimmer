package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface DtoTypeResolver<T extends BaseType> {

    @Nullable
    DtoTypeInfo<T> resolve(String qualifiedName);
}
