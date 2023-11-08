package org.babyfish.jimmer.apt.dto;

import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.NotNull;

public class BaseTypeImpl implements BaseType {

    @NotNull
    @Override
    public String getName() {
        return null;
    }

    @NotNull
    @Override
    public String getPackageName() {
        return null;
    }

    @NotNull
    @Override
    public String getQualifiedName() {
        return null;
    }

    @Override
    public boolean isEntity() {
        return false;
    }
}
