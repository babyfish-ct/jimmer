package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public interface DtoProp<T extends BaseType, P extends BaseProp> extends DtoPropImplementor {

    DtoProp<T, P> toTailProp();

    @Override
    P getBaseProp();

    @Override
    Map<String, P> getBasePropMap();

    String getBasePath();

    @Nullable
    DtoProp<T, P> getNextProp();

    String getName();

    boolean isBaseNullable();

    boolean isIdOnly();

    boolean isFlat();

    boolean isFunc(String ... funcNames);

    @Nullable
    String getAlias();

    @Nullable
    PropConfig<P> getConfig();

    @Nullable
    DtoType<T, P> getTargetType();

    @Nullable
    EnumType getEnumType();

    boolean isRecursive();

    Set<LikeOption> getLikeOptions();
}
