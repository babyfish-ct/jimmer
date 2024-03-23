package org.babyfish.jimmer.dto.compiler;

interface AbstractPropBuilder {

    String getAlias();

    AliasPattern getAliasPattern();

    AbstractProp build(DtoType<?, ?> type);
}
