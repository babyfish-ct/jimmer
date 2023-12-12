package org.babyfish.jimmer.client.meta;

import java.util.Map;

public interface Schema {

    Map<TypeName, ApiService> getApiServiceMap();

    Map<TypeName, TypeDefinition> getTypeDefinitionMap();
}
