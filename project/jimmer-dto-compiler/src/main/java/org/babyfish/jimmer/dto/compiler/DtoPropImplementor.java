package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

interface DtoPropImplementor extends AbstractProp {

    BaseProp getBaseProp();

    Map<String, ?> getBasePropMap();

    int getBaseLine();

    int getBaseColumn();

    int getAliasLine();

    int getAliasColumn();

    @Nullable
    String getFuncName();

    Mandatory getMandatory();

    DtoModifier getInputModifier();
}
