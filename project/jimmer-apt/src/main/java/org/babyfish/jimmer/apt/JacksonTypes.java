package org.babyfish.jimmer.apt;

import com.squareup.javapoet.ClassName;

public class JacksonTypes {

    public final ClassName jsonIgnore;

    public final ClassName jsonValue;

    public final ClassName jsonPropertyOrder;

    public final ClassName jsonFormat;

    public final ClassName jsonSerializer;

    public final ClassName jsonSerialize;

    public final ClassName jsonDeserialize;

    public final ClassName jsonPojoBuilder;

    public final ClassName jsonNaming;

    public final ClassName jsonGenerator;

    public final ClassName serializerProvider;

    public JacksonTypes(
            ClassName jsonIgnore,
            ClassName jsonValue,
            ClassName jsonPropertyOrder,
            ClassName jsonFormat,
            ClassName jsonSerializer,
            ClassName jsonSerialize,
            ClassName jsonDeserialize,
            ClassName jsonPojoBuilder,
            ClassName jsonNaming,
            ClassName jsonGenerator,
            ClassName serializerProvider
    ) {
        this.jsonIgnore = jsonIgnore;
        this.jsonValue = jsonValue;
        this.jsonPropertyOrder = jsonPropertyOrder;
        this.jsonFormat = jsonFormat;
        this.jsonSerializer = jsonSerializer;
        this.jsonSerialize = jsonSerialize;
        this.jsonDeserialize = jsonDeserialize;
        this.jsonPojoBuilder = jsonPojoBuilder;
        this.jsonNaming = jsonNaming;
        this.jsonGenerator = jsonGenerator;
        this.serializerProvider = serializerProvider;
    }
}
