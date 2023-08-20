package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.jetbrains.annotations.Nullable;

interface DtoPropImplementor extends AbstractProp {

    BaseProp getBaseProp();

    int getBaseLine();

    int getAliasLine();

    @Nullable
    String getFuncName();
}
