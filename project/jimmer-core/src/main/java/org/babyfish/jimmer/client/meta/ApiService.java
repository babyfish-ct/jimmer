package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ApiService {

    String getTypeName();

    @Nullable
    List<String> getGroups();

    List<ApiOperation> getOperations();

    Doc getDoc();

    ApiOperation findOperation(String name, Class<?>... types);

    ApiOperation findOperation(String name, String... typeNames);
}
