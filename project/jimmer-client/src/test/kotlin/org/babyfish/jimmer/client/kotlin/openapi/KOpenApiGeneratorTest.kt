package org.babyfish.jimmer.client.kotlin.openapi

import org.babyfish.jimmer.client.common.OperationParserImpl
import org.babyfish.jimmer.client.common.ParameterParserImpl
import org.babyfish.jimmer.client.generator.openapi.OpenApiGenerator
import org.babyfish.jimmer.client.generator.openapi.OpenApiProperties
import org.babyfish.jimmer.client.runtime.Metadata
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.util.*

class KOpenApiGeneratorTest {

    @Test
    fun testKBookService() {
        val metadata = Metadata
            .newBuilder()
            .setOperationParser(OperationParserImpl())
            .setParameterParameter(ParameterParserImpl())
            .setGroups(setOf("kBookService"))
            .build()
        val generator = OpenApiGenerator(
            metadata,
            OpenApiProperties.newBuilder()
                .setInfo(
                    OpenApiProperties.newInfoBuilder()
                        .setTitle("Book System")
                        .setDescription("You can use this system the operate book data")
                        .setVersion("2.0.0")
                        .build()
                )
                .setSecurities(
                    listOf(
                        Collections.singletonMap("tenantHeader", emptyList())
                    )
                )
                .setServers(
                    listOf(
                        OpenApiProperties.newServerBuilder()
                            .setUrl("http://localhost:8080")
                            .build()
                    )
                )
                .setComponents(
                    OpenApiProperties.newComponentsBuilder()
                        .setSecuritySchemes(
                            mapOf(
                                "tenantHeader" to
                                OpenApiProperties.newSecuritySchemeBuilder()
                                    .setType("apiKey")
                                    .setName("tenant")
                                    .setIn(OpenApiProperties.In.HEADER)
                                    .build()
                            )
                        )
                        .build()
                )
                .build()
        )
        val writer = StringWriter()
        generator.generate(writer)
        println(writer.toString())
    }
}