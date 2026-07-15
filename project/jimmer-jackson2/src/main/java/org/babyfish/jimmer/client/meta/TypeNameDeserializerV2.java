package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class TypeNameDeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<TypeName> {
        @Override
        public TypeName deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                    com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            String value = jp.getValueAsString();
            return TypeName.parse(value);
        }
    }
