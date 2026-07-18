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

public class ApiServiceImplSerializerV3 extends tools.jackson.databind.ValueSerializer<ApiServiceImpl<?>> {

        @Override
        public void serialize(ApiServiceImpl<?> service,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            ctx.defaultSerializeProperty("typeName", service.getTypeName(), gen);
            if (service.getGroups() != null) {
                ctx.defaultSerializeProperty("groups", service.getGroups(), gen);
            }
            if (service.getDoc() != null) {
                ctx.defaultSerializeProperty("doc", service.getDoc(), gen);
            }
            if (!service.getOperations().isEmpty()) {
                ctx.defaultSerializeProperty("operations", service.getOperations(), gen);
            }
            gen.writeEndObject();
        }
    }
