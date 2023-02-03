package org.babyfish.jimmer.meta.impl.dto.ast;

import org.babyfish.jimmer.meta.impl.dto.ast.spi.BaseProp;
import org.babyfish.jimmer.meta.impl.dto.ast.spi.BaseType;
import org.jetbrains.annotations.Nullable;

public interface DtoProp<T extends BaseType, P extends BaseProp> {

    P getBaseProp();

    String getName();

    boolean isNullable();

    boolean isIdOnly();

    @Nullable
    String getAlias();

    @Nullable
    DtoType<T, P> getTargetType();

    boolean isRecursive();

    boolean isNewTarget();
}
