package org.babyfish.jimmer.client.kotlin.ts

import org.babyfish.jimmer.client.common.OperationParserImpl
import org.babyfish.jimmer.client.common.ParameterParserImpl
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext
import org.babyfish.jimmer.client.runtime.Metadata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.StringWriter

class KApiErrorsTest {

    @Test
    fun testApiErrors() {
        val ctx = TypeScriptContext(METADATA)
        val apiErrorsSources = ctx.getRootSource("ApiErrors")
        val writer = StringWriter()
        ctx.render(apiErrorsSources, writer)
        Assertions.assertEquals(
                "export type AllErrors = {\n" +
                "        family: 'KBUSINESS', \n" +
                "        code: 'DATA_IS_FROZEN'\n" +
                "    } | {\n" +
                "        family: 'KBUSINESS', \n" +
                "        code: 'SERVICE_IS_SUSPENDED', \n" +
                "        planedResumeTime?: string | undefined\n" +
                "    };\n" +
                "export type ApiErrors = {\n" +
                "    'kbookService': {\n" +
                "        'saveBook': AllErrors & ({\n" +
                "                family: 'KBUSINESS', \n" +
                "                code: 'DATA_IS_FROZEN', \n" +
                "                readonly [key:string]: any\n" +
                "            } | {\n" +
                "                family: 'KBUSINESS', \n" +
                "                code: 'SERVICE_IS_SUSPENDED', \n" +
                "                readonly [key:string]: any\n" +
                "            }), \n" +
                "        'updateBook': AllErrors & ({\n" +
                "                family: 'KBUSINESS', \n" +
                "                code: 'DATA_IS_FROZEN', \n" +
                "                readonly [key:string]: any\n" +
                "            } | {\n" +
                "                family: 'KBUSINESS', \n" +
                "                code: 'SERVICE_IS_SUSPENDED', \n" +
                "                readonly [key:string]: any\n" +
                "            })\n" +
                "    }\n" +
                "};\n",
            writer.toString()
        )
    }

    companion object {
        private val METADATA = Metadata
            .newBuilder()
            .setOperationParser(OperationParserImpl())
            .setParameterParameter(ParameterParserImpl())
            .setGroups(listOf("kBookService"))
            .setGenericSupported(true)
            .build()
    }
}