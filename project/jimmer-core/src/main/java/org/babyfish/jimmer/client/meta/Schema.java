package org.babyfish.jimmer.client.meta;

import java.util.Map;

public interface Schema {

    Map<String, ApiService> getApiServiceMap();

    Map<String, TypeDefinition> getTypeDefinitionMap();
}
