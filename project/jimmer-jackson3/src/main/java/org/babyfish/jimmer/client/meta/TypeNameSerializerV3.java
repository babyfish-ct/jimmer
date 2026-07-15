package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class TypeNameSerializerV3 extends tools.jackson.databind.ValueSerializer<TypeName> {
        @Override
        public void serialize(TypeName typeName,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeString(typeName.toString(true));
        }
    }
