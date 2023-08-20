package org.babyfish.jimmer.dto.compiler;

interface AbstractPropBuilder {

    String getAlias();

    AbstractProp build();
}
