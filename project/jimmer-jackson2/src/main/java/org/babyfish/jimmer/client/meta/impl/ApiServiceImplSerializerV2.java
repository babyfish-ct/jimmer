package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.client.meta.ApiOperation;
import org.babyfish.jimmer.client.meta.ApiService;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeName;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ApiServiceImplSerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<ApiServiceImpl<?>> {

        @Override
        public void serialize(ApiServiceImpl<?> service,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("typeName", service.getTypeName(), gen);
            if (service.getGroups() != null) {
                provider.defaultSerializeField("groups", service.getGroups(), gen);
            }
            if (service.getDoc() != null) {
                provider.defaultSerializeField("doc", service.getDoc(), gen);
            }
            if (!service.getOperations().isEmpty()) {
                provider.defaultSerializeField("operations", service.getOperations(), gen);
            }
            gen.writeEndObject();
        }
    }
