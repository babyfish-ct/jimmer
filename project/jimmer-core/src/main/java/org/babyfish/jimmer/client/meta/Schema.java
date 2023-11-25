package org.babyfish.jimmer.client.meta;

import java.util.List;
import java.util.Map;

public interface Schema {

    List<ApiService> getApiServices();

    Map<String, TypeDefinition> getTypeDefinitionMap();
}
