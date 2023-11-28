package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ApiService {

    String getTypeName();

    @Nullable
    List<String> getGroups();

    List<ApiOperation> getOperations();

    @Nullable
    Doc getDoc();

    @Nullable
    ApiOperation findOperation(String name, Class<?>... types);
}
