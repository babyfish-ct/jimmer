package org.babyfish.jimmer.json.jackson.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;

class ObjectMapperBuilder extends MapperBuilder<ObjectMapper, ObjectMapperBuilder> {
    ObjectMapperBuilder(ObjectMapper mapper) {
        super(mapper);
    }
}
