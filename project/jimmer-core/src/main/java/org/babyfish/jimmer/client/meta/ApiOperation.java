package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ApiOperation {

    String getName();

    @Nullable
    List<String> getGroups();

    List<Parameter> getParameters();

    @Nullable
    TypeRef getReturnType();

    Doc getDoc();
}
