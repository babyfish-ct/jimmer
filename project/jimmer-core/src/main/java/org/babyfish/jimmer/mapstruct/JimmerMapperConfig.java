package org.babyfish.jimmer.mapstruct;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.NullValueMappingStrategy;

@MapperConfig(
        collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
)
public interface JimmerMapperConfig {}
