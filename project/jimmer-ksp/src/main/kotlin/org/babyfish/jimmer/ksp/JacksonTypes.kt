package org.babyfish.jimmer.ksp

import com.squareup.kotlinpoet.ClassName

class JacksonTypes(
    val jsonIgnore: ClassName,
    val jsonValue: ClassName,
    val jsonFormat: ClassName,
    val jsonProperty: ClassName,
    val jsonPropertyOrder: ClassName,
    val jsonCreator: ClassName,
    val jsonSerializer: ClassName,
    val jsonSerialize: ClassName,
    val jsonDeserialize: ClassName,
    val jsonPojoBuilder: ClassName,
    val jsonNaming: ClassName,
    val jsonGenerator: ClassName,
    val serializeProvider: ClassName
)