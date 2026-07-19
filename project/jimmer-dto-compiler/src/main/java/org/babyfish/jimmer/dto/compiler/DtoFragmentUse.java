package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;

class DtoFragmentUse<T extends BaseType, P extends BaseProp> {

    final DtoFragment<T, P> fragment;

    final DtoParser.IncludeContext ast;

    DtoFragmentUse(DtoFragment<T, P> fragment, DtoParser.IncludeContext ast) {
        this.fragment = fragment;
        this.ast = ast;
    }
}
