package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class TypeNameDeserializerV3 extends tools.jackson.databind.ValueDeserializer<TypeName> {
        @Override
        public TypeName deserialize(tools.jackson.core.JsonParser jp,
                                    tools.jackson.databind.DeserializationContext ctx) {
            String value = jp.getValueAsString();
            return TypeName.parse(value);
        }
    }
