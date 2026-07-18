package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class TypeNameSerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<TypeName> {
        @Override
        public void serialize(TypeName typeName,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeString(typeName.toString(true));
        }
    }
