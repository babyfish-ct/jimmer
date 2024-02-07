package org.babyfish.jimmer.dto.compiler;

import java.util.List;

public interface AbstractProp {

    String getAlias();

    boolean isNullable();

    int getAliasLine();

    int getAliasColumn();

    List<Anno> getAnnotations();

    String getDoc();
}
