package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface AbstractProp {

    String getName();

    String getAlias();

    boolean isNullable();

    int getAliasLine();

    int getAliasColumn();

    List<Anno> getAnnotations();

    String getDoc();

    @Nullable
    default String getFuncName() {
        return null;
    }

    @Nullable
    default DtoModifier getInputModifier() {
        return null;
    }

    @Nullable
    default BaseProp getBasePropOrNull() {
        return null;
    }
}
