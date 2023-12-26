package org.babyfish.jimmer.dto.compiler;

import java.util.List;

public interface AbstractProp {

    String getAlias();

    int getAliasLine();

    int getAliasColumn();

    List<Anno> getAnnotations();

    String getDoc();
}
