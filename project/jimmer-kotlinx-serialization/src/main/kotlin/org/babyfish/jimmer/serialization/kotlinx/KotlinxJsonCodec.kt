package org.babyfish.jimmer.serialization.kotlinx

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import org.babyfish.jimmer.json.codec.JsonCodec
import org.babyfish.jimmer.json.codec.JsonCodecCustomization
import org.babyfish.jimmer.json.codec.JsonConverter
import org.babyfish.jimmer.json.codec.JsonCodecFamily
import org.babyfish.jimmer.json.codec.JsonReader
import org.babyfish.jimmer.json.codec.JsonType
import org.babyfish.jimmer.json.codec.JsonWriter
import org.babyfish.jimmer.json.codec.Node
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.runtime.DraftSpi
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.runtime.Internal
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

/**
 * JSON codec backed by kotlinx.serialization.
 *
 * This codec is intentionally explicit: applications can pass it to Jimmer APIs such as
 * `JSqlClient.Builder#setJsonCodec` or `setDefaultSerializedTypeJsonCodec` when the
 * serialized application model is kotlinx-serializable.
 */
@OptIn(ExperimentalSerializationApi::class)
class KotlinxJsonCodec @JvmOverloads constructor(
    private val json: Json = DEFAULT_JSON
) : JsonCodec {

    override fun withCustomizations(vararg customizations: JsonCodecCustomization): JsonCodec =
        this

    override fun converter(): JsonConverter =
        KotlinxJsonConverter(json)

    override fun <T : Any?> readerFor(type: JsonType): JsonReader<T> =
        readerFor(KotlinxJsonTypes.constructType(type))

    override fun treeReader(): JsonReader<Node> =
        KotlinxTreeReader(json)

    override fun writer(): JsonWriter =
        KotlinxJsonWriter(json, null)

    override fun writerFor(type: JsonType): JsonWriter =
        writerFor(KotlinxJsonTypes.constructType(type))

    override fun family(): JsonCodecFamily =
        JsonCodecFamily.KOTLINX_SERIALIZATION

    private fun <T> readerFor(type: KType): JsonReader<T> =
        KotlinxJsonReader(json, type)

    private fun writerFor(type: KType): JsonWriter =
        KotlinxJsonWriter(json, type)

    companion object {
        @JvmField
        val DEFAULT_JSON: Json = Json {
            ignoreUnknownKeys = true
        }
    }
}

class KotlinxJsonCodecProvider : org.babyfish.jimmer.json.codec.JsonCodecProvider {
    override fun family(): JsonCodecFamily =
        JsonCodecFamily.KOTLINX_SERIALIZATION

    override fun create(): JsonCodec =
        KotlinxJsonCodec()
}

@OptIn(ExperimentalSerializationApi::class)
private class KotlinxJsonReader<T>(
    private val json: Json,
    private val type: KType
) : JsonReader<T> {

    override fun read(json: String): T =
        decode(this.json.parseToJsonElement(json))

    override fun read(json: ByteArray): T =
        read(String(json, StandardCharsets.UTF_8))

    override fun read(reader: Reader): T =
        read(reader.readText())

    override fun read(inputStream: InputStream): T =
        read(inputStream.readBytes())

    @Suppress("UNCHECKED_CAST")
    private fun decode(element: JsonElement): T =
        KotlinxJsonSupport.decodeElement(json, element, type) as T
}

@OptIn(ExperimentalSerializationApi::class)
private class KotlinxTreeReader(
    private val json: Json
) : JsonReader<Node> {

    override fun read(json: String): Node =
        KotlinxJsonNode(this.json.parseToJsonElement(json))

    override fun read(json: ByteArray): Node =
        read(String(json, StandardCharsets.UTF_8))

    override fun read(reader: Reader): Node =
        read(reader.readText())

    override fun read(inputStream: InputStream): Node =
        read(inputStream.readBytes())
}

@OptIn(ExperimentalSerializationApi::class)
private class KotlinxJsonWriter(
    private val json: Json,
    private val type: KType?
) : JsonWriter {

    override fun withDefaultPrettyPrinter(): JsonWriter =
        KotlinxJsonWriter(
            Json(json) {
                prettyPrint = true
            },
            type
        )

    override fun writeAsString(obj: Any?): String =
        json.encodeToString(JsonElement.serializer(), encode(obj))

    override fun writeAsBytes(obj: Any?): ByteArray =
        writeAsString(obj).toByteArray(StandardCharsets.UTF_8)

    override fun write(writer: Writer, obj: Any?) {
        writer.write(writeAsString(obj))
    }

    override fun write(outputStream: OutputStream, obj: Any?) {
        outputStream.write(writeAsBytes(obj))
    }

    private fun encode(value: Any?): JsonElement =
        if (type !== null) {
            KotlinxJsonSupport.encodeElement(json, value, type)
        } else {
            KotlinxJsonSupport.encodeUntyped(json, value)
        }
}

@OptIn(ExperimentalSerializationApi::class)
private class KotlinxJsonConverter(
    private val json: Json
) : JsonConverter {

    override fun <T : Any?> convert(value: Any?, targetType: Class<T>): T =
        convert(value, KotlinxJsonTypes.constructType(targetType))

    override fun <T : Any?> convert(value: Any?, targetType: JsonType): T =
        convert(value, KotlinxJsonTypes.constructType(targetType))

    @Suppress("UNCHECKED_CAST")
    private fun <T> convert(value: Any?, type: KType): T {
        val element = when (value) {
            is KotlinxJsonNode -> value.element
            is JsonElement -> value
            else -> KotlinxJsonSupport.encodeUntyped(json, value)
        }
        return KotlinxJsonSupport.decodeElement(json, element, type) as T
    }
}

@OptIn(ExperimentalSerializationApi::class)
private object KotlinxJsonSupport {

    fun encodeUntyped(json: Json, value: Any?): JsonElement =
        when (value) {
            null -> JsonNull
            is JsonElement -> value
            is ImmutableSpi -> encodeImmutable(json, value)
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Char -> JsonPrimitive(value.toString())
            is UUID -> JsonPrimitive(value.toString())
            is Enum<*> -> JsonPrimitive(value.name)
            is Iterable<*> -> JsonArray(value.map { encodeUntyped(json, it) })
            is Array<*> -> JsonArray(value.map { encodeUntyped(json, it) })
            is BooleanArray -> JsonArray(value.map { JsonPrimitive(it) })
            is ByteArray -> JsonArray(value.map { JsonPrimitive(it) })
            is ShortArray -> JsonArray(value.map { JsonPrimitive(it) })
            is IntArray -> JsonArray(value.map { JsonPrimitive(it) })
            is LongArray -> JsonArray(value.map { JsonPrimitive(it) })
            is FloatArray -> JsonArray(value.map { JsonPrimitive(it) })
            is DoubleArray -> JsonArray(value.map { JsonPrimitive(it) })
            is CharArray -> JsonArray(value.map { JsonPrimitive(it.toString()) })
            is Map<*, *> -> JsonObject(
                value.entries.associate { (key, entryValue) ->
                    key.toString() to encodeUntyped(json, entryValue)
                }
            )
            else -> encodeElement(json, value, value::class.createType())
        }

    fun encodeElement(json: Json, value: Any?, type: KType): JsonElement {
        if (value == null) {
            return JsonNull
        }
        if (value is ImmutableSpi) {
            return encodeImmutable(json, value)
        }
        val serializer = json.serializersModule.serializer(type) as KSerializer<Any?>
        return json.encodeToJsonElement(serializer, value)
    }

    fun decodeElement(json: Json, element: JsonElement, type: KType): Any? {
        if (element is JsonNull) {
            return null
        }
        val immutableType = type.javaClassOrNull()?.let(ImmutableType::tryGet)
        if (immutableType !== null && element is JsonObject) {
            return decodeImmutable(json, element, immutableType)
        }
        val serializer = json.serializersModule.serializer(type) as KSerializer<Any?>
        return json.decodeFromJsonElement(serializer, element)
    }

    private fun encodeImmutable(json: Json, value: ImmutableSpi): JsonObject {
        val fields = linkedMapOf<String, JsonElement>()
        for (prop in value.__type().props.values) {
            val propId = prop.id
            if (value.__isLoaded(propId) && value.__isVisible(propId)) {
                fields[prop.name] = encodeUntyped(json, value.__get(propId))
            }
        }
        return JsonObject(fields)
    }

    private fun decodeImmutable(json: Json, element: JsonObject, immutableType: ImmutableType): Any =
        Internal.produce(immutableType, null) { draft ->
            val spi = draft as DraftSpi
            for (prop in immutableType.props.values) {
                val propElement = element[prop.name] ?: continue
                val value = decodeElement(json, propElement, KotlinxJsonTypes.constructType(prop.genericType))
                spi.__set(prop.id, value)
            }
        }

    private fun KType.javaClassOrNull(): Class<*>? =
        jvmErasure.java
}

private object KotlinxJsonTypes {

    fun constructType(type: JsonType): KType =
        constructType(type.type)

    fun constructType(type: Type): KType =
        type.toKType()

    fun constructParametricType(parametrized: Class<*>, vararg parameterClasses: Class<*>): KType =
        parametrized.kotlin.createType(
            parameterClasses.map { KTypeProjection.invariant(constructType(it)) }
        )

    fun constructParametricType(parametrized: Class<*>, parameterClasses: Array<KType>): KType =
        parametrized.kotlin.createType(
            parameterClasses.map { KTypeProjection.invariant(it) }
        )

    fun constructArrayType(componentType: Class<*>): KType =
        constructArrayType(constructType(componentType))

    fun constructArrayType(componentType: KType): KType =
        Array<Any?>::class.createType(listOf(KTypeProjection.invariant(componentType)))

    fun constructCollectionType(collectionType: Class<out Collection<*>>, elementType: Class<*>): KType =
        constructCollectionType(collectionType, constructType(elementType))

    fun constructCollectionType(collectionType: Class<out Collection<*>>, elementType: KType): KType =
        collectionType.kotlin.createType(listOf(KTypeProjection.invariant(elementType)))

    fun constructMapType(mapType: Class<out Map<*, *>>, keyType: Class<*>, valueType: Class<*>): KType =
        constructMapType(mapType, constructType(keyType), constructType(valueType))

    fun constructMapType(mapType: Class<out Map<*, *>>, keyType: KType, valueType: KType): KType =
        mapType.kotlin.createType(
            listOf(
                KTypeProjection.invariant(keyType),
                KTypeProjection.invariant(valueType)
            )
        )

    private fun Type.toKType(): KType =
        when (this) {
            is Class<*> -> toKType()
            is ParameterizedType -> {
                val rawClass = rawType as? Class<*>
                    ?: throw IllegalArgumentException("Parameterized raw type \"$rawType\" is not a class")
                rawClass.kotlin.createType(
                    actualTypeArguments.map { argument ->
                        KTypeProjection.invariant(argument.toKType())
                    }
                )
            }
            is GenericArrayType -> constructArrayType(genericComponentType.toKType())
            is WildcardType -> upperBounds.firstOrNull()?.toKType() ?: Any::class.starProjectedType
            is TypeVariable<*> -> bounds.firstOrNull()?.toKType() ?: Any::class.starProjectedType
            else -> throw IllegalArgumentException("Unsupported type: $this")
        }

    private fun Class<*>.toKType(): KType =
        kotlin.createType()
}

private class KotlinxJsonNode(
    internal val element: JsonElement
) : Node {

    override fun get(index: Int): Node? =
        (element as? JsonArray)?.getOrNull(index)?.let(::KotlinxJsonNode)

    override fun get(fieldName: String): Node? =
        (element as? JsonObject)?.get(fieldName)?.let(::KotlinxJsonNode)

    override fun fieldsIterator(): Iterator<Map.Entry<String, Node>> {
        val jsonObject = element as? JsonObject ?: return emptyMap<String, Node>().entries.iterator()
        return jsonObject.entries
            .map { (key, value) -> mapEntry(key, KotlinxJsonNode(value)) }
            .iterator()
    }

    override fun isNull(): Boolean =
        element is JsonNull

    override fun canCastTo(type: Class<*>): Boolean =
        CASTER_MAP.containsKey(type)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> castTo(type: Class<T>): T {
        val caster = CASTER_MAP[type]
            ?: throw IllegalArgumentException("Cannot cast node to type ${type.name}")
        return caster(element) as T
    }

    override fun <T : Any?> convertTo(targetType: Class<T>, converter: JsonConverter): T =
        converter.convert(element, targetType)

    override fun equals(other: Any?): Boolean =
        other is KotlinxJsonNode && element == other.element

    override fun hashCode(): Int =
        element.hashCode()

    override fun toString(): String =
        element.toString()

    private fun mapEntry(key: String, value: Node): Map.Entry<String, Node> =
        object : Map.Entry<String, Node> {
            override val key: String = key
            override val value: Node = value
        }

    companion object {
        private val CASTER_MAP: Map<Class<*>, (JsonElement) -> Any?> = mapOf(
            Boolean::class.javaPrimitiveType!! to { it.jsonPrimitive().booleanOrNull },
            Boolean::class.java to { it.jsonPrimitive().booleanOrNull },
            Char::class.javaPrimitiveType!! to { it.jsonPrimitive().contentOrNull?.firstOrNull() },
            Char::class.java to { it.jsonPrimitive().contentOrNull?.firstOrNull() },
            Byte::class.javaPrimitiveType!! to { it.jsonPrimitive().intOrNull?.toByte() },
            Byte::class.java to { it.jsonPrimitive().intOrNull?.toByte() },
            Short::class.javaPrimitiveType!! to { it.jsonPrimitive().intOrNull?.toShort() },
            Short::class.java to { it.jsonPrimitive().intOrNull?.toShort() },
            Int::class.javaPrimitiveType!! to { it.jsonPrimitive().intOrNull },
            Int::class.java to { it.jsonPrimitive().intOrNull },
            Long::class.javaPrimitiveType!! to { it.jsonPrimitive().longOrNull },
            Long::class.java to { it.jsonPrimitive().longOrNull },
            Float::class.javaPrimitiveType!! to { it.jsonPrimitive().floatOrNull },
            Float::class.java to { it.jsonPrimitive().floatOrNull },
            Double::class.javaPrimitiveType!! to { it.jsonPrimitive().doubleOrNull },
            Double::class.java to { it.jsonPrimitive().doubleOrNull },
            BigInteger::class.java to { it.jsonPrimitive().contentOrNull?.toBigInteger() },
            BigDecimal::class.java to { it.jsonPrimitive().contentOrNull?.toBigDecimal() },
            String::class.java to { it.jsonPrimitive().contentOrNull },
            UUID::class.java to { it.jsonPrimitive().contentOrNull?.let(UUID::fromString) }
        )

        private fun JsonElement.jsonPrimitive(): JsonPrimitive =
            this as? JsonPrimitive ?: throw SerializationException("JSON element is not a primitive: $this")
    }
}
