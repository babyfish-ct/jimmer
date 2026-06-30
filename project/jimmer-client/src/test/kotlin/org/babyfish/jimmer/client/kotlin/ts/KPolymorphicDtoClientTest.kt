package org.babyfish.jimmer.client.kotlin.ts

import org.babyfish.jimmer.client.common.OperationParserImpl
import org.babyfish.jimmer.client.common.ParameterParserImpl
import org.babyfish.jimmer.client.generator.Context
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext
import org.babyfish.jimmer.client.kotlin.model.dto.KClientView
import org.babyfish.jimmer.client.runtime.Metadata
import org.babyfish.jimmer.client.runtime.ObjectType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.StringWriter

class KPolymorphicDtoClientTest {

    @Test
    fun testMetadataBranches() {
        val type = METADATA.getType(KClientView::class.java) as ObjectType
        Assertions.assertEquals(3, type.polymorphicBranches.size)
        Assertions.assertEquals(KClientView.Default::class.java, type.polymorphicBranches[0].javaType)
        Assertions.assertEquals(KClientView.KPerson::class.java, type.polymorphicBranches[1].javaType)
        Assertions.assertEquals(KClientView.KOrganization::class.java, type.polymorphicBranches[2].javaType)
    }

    @Test
    fun testTypeScript() {
        val ctx: Context = TypeScriptContext(METADATA)
        Assertions.assertTrue(
            render(ctx, "model/static/KClientView").contains(
                "export type KClientView = KClientView_Default | KClientView_KPerson | KClientView_KOrganization;"
            )
        )

        val defaultBranch = render(ctx, "model/static/KClientView_Default")
        Assertions.assertTrue(defaultBranch.contains("export interface KClientView_Default"))
        Assertions.assertTrue(defaultBranch.contains("readonly type: KClientType;"))
        Assertions.assertFalse(defaultBranch.contains("firstName"))
        Assertions.assertFalse(defaultBranch.contains("taxCode"))

        val personBranch = render(ctx, "model/static/KClientView_KPerson")
        Assertions.assertTrue(personBranch.contains("readonly firstName: string;"))
        Assertions.assertFalse(personBranch.contains("taxCode"))

        val organizationBranch = render(ctx, "model/static/KClientView_KOrganization")
        Assertions.assertTrue(organizationBranch.contains("readonly taxCode: string;"))
        Assertions.assertFalse(organizationBranch.contains("firstName"))
    }

    private fun render(ctx: Context, sourceName: String): String {
        val source = ctx.getRootSource(sourceName)
        val writer = StringWriter()
        ctx.render(source, writer)
        return writer.toString()
    }

    companion object {

        private val METADATA: Metadata =
            Metadata
                .newBuilder()
                .setOperationParser(OperationParserImpl())
                .setParameterParser(ParameterParserImpl())
                .setGroups(setOf("kPolymorphicDtoService"))
                .setGenericSupported(true)
                .build()
    }
}
