package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;

class DtoPropBuilder<T extends BaseType, P extends BaseProp> {

    private final P baseProp;

    private final String funcName;

    private final String alias;

    private final boolean optional;

    private final DtoTypeBuilder<T, P> targetTypeBuilder;

    private final boolean recursive;

    DtoPropBuilder(P baseProp, boolean optional) {
        this(baseProp, optional, null, null, null, false);
    }

    DtoPropBuilder(
            P baseProp,
            boolean optional,
            String funcName,
            String alias,
            DtoTypeBuilder<T, P> targetTypeBuilder,
            boolean recursive
    ) {
        this.baseProp = baseProp;
        this.optional = optional;
        this.funcName = funcName;
        this.alias = alias;
        this.targetTypeBuilder = targetTypeBuilder;
        this.recursive = recursive;
    }

    DtoProp<T, P> build() {
        return new DtoPropImpl<>(
                baseProp,
                alias,
                targetTypeBuilder != null ? targetTypeBuilder.build() : null,
                optional,
                funcName,
                recursive
        );
    }
}
