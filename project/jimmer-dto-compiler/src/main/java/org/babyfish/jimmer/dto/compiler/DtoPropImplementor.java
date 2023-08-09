package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.jetbrains.annotations.Nullable;

interface DtoPropImplementor {

    BaseProp getBaseProp();

    int getBaseLine();

    String getAlias();

    int getAliasLine();

    @Nullable
    String getFuncName();
}
