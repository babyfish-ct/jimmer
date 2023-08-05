package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

public interface DtoProp<T extends BaseType, P extends BaseProp> extends DtoPropImplementor {

    @Override
    P getBaseProp();

    String getName();

    boolean isNullable();

    boolean isIdOnly();

    boolean isFlat();

    @Nullable
    String getAlias();

    @Nullable
    DtoType<T, P> getTargetType();

    boolean isRecursive();

    boolean isNewTarget();
}
