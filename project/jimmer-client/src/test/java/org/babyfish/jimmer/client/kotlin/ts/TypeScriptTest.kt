package org.babyfish.jimmer.client.kotlin.ts

import org.babyfish.jimmer.client.generator.ts.Context
import org.babyfish.jimmer.client.generator.ts.ServiceWriter
import org.babyfish.jimmer.client.kotlin.service.KBookService
import org.babyfish.jimmer.client.meta.Constants
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class TypeScriptTest {

    @Test
    fun testServiceWriter() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        val service = Constants.KOTLIN_METADATA.services[KBookService::class.java]
        ServiceWriter(ctx, service).flush()
        val code = out.toString()
        println(code)
    }

    private fun createContext(out: OutputStream): Context? {
        return Context(Constants.KOTLIN_METADATA, out, "Api", 4)
    }
}