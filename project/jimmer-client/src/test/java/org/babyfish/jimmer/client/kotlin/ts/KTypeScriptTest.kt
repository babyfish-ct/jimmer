package org.babyfish.jimmer.client.kotlin.ts

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.generator.ts.Context
import org.babyfish.jimmer.client.generator.ts.DtoWriter
import org.babyfish.jimmer.client.generator.ts.ServiceWriter
import org.babyfish.jimmer.client.generator.ts.TypeDefinitionWriter
import org.babyfish.jimmer.client.kotlin.model.KBook
import org.babyfish.jimmer.client.kotlin.model.KBookInput
import org.babyfish.jimmer.client.kotlin.service.KBookService
import org.babyfish.jimmer.client.meta.Constants
import org.babyfish.jimmer.client.meta.StaticObjectType
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class KTypeScriptTest {

    @Test
    fun testServiceWriter() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        val service = Constants.KOTLIN_METADATA.services[KBookService::class.java]
        ServiceWriter(ctx, service).flush()
        val code = out.toString()
        println(code)
    }

    @Test
    fun testStaticObject() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        val bookInput = Constants.KOTLIN_METADATA.staticTypes[StaticObjectType.Key(KBookInput::class.java, null)]
        TypeDefinitionWriter(ctx, bookInput).flush()
        val code = out.toString()
        println(code)
    }

    @Test
    fun testSimpleDto() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        DtoWriter(ctx, KBook::class.java, ctx.dtoMap.get(KBook::class.java)).flush()
        val code = out.toString()
        println(code)
    }

    private fun createContext(out: OutputStream): Context {
        return Context(Constants.KOTLIN_METADATA, out, "Api", 4)
    }
}