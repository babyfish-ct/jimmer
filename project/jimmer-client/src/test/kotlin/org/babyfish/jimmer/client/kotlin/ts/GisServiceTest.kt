package org.babyfish.jimmer.client.kotlin.ts

import org.babyfish.jimmer.client.common.OperationParserImpl
import org.babyfish.jimmer.client.common.ParameterParserImpl
import org.babyfish.jimmer.client.generator.Context
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext
import org.babyfish.jimmer.client.kotlin.service.GisService
import org.babyfish.jimmer.client.runtime.Metadata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.StringWriter

class GisServiceTest {

    @Test
    fun testSources() {
        val ctx: Context = TypeScriptContext(METADATA)
        Assertions.assertEquals(
            "[Api, " +
                "ApiErrors, " +
                "ElementOf, " +
                "Executor, " +
                "RequestOf, " +
                "ResponseOf, " +
                "model/static/GisAreaView, " +
                "model/static/GisPoint, " +
                "model/static/GisRegionView, " +
                "services/GisService]",
            ctx.rootSources.toString()
        )
    }

    @Test
    fun testService() {
        val ctx: Context = TypeScriptContext(METADATA)
        val source = ctx.getRootSource("services/" + GisService::class.simpleName)
        val writer = StringWriter()
        ctx.render(source, writer)
        Assertions.assertEquals(
            "import type {Executor} from '../';\n" +
                "import type {GisAreaView} from '../model/static/';\n" +
                "\n" +
                "export class GisService {\n" +
                "    \n" +
                "    constructor(private executor: Executor) {}\n" +
                "    \n" +
                "    readonly findAreaById: (options: GisServiceOptions['findAreaById']) => Promise<\n" +
                "        GisAreaView\n" +
                "    > = async(options) => {\n" +
                "        let _uri = '/gisArea/';\n" +
                "        _uri += encodeURIComponent(options.id);\n" +
                "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<GisAreaView>;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "export type GisServiceOptions = {\n" +
                "    'findAreaById': {\n" +
                "        readonly id: number\n" +
                "    }\n" +
                "}\n",
            writer.toString()
        )
    }

    @Test
    fun testGisAreaView() {
        val ctx: Context = TypeScriptContext(METADATA)
        val source = ctx.getRootSource("model/static/GisAreaView")
        val writer = StringWriter()
        ctx.render(source, writer)
        Assertions.assertEquals(
            "import type {GisPoint, GisRegionView} from './';\n" +
                "\n" +
                "export interface GisAreaView {\n" +
                "    readonly id: number;\n" +
                "    readonly region: GisRegionView;\n" +
                "    readonly points: ReadonlyArray<GisPoint>;\n" +
                "    readonly name: string;\n" +
                "}\n",
            writer.toString()
        )
    }

    @Test
    fun testGisAreaPoint() {
        val ctx: Context = TypeScriptContext(METADATA)
        val source = ctx.getRootSource("model/static/GisPoint")
        val writer = StringWriter()
        ctx.render(source, writer)
        Assertions.assertEquals(
            "export interface GisPoint {\n" +
                "    readonly x: number;\n" +
                "    readonly y: number;\n" +
                "}\n",
            writer.toString()
        );
    }

    @Test
    fun testGisRegionView() {
        val ctx: Context = TypeScriptContext(METADATA)
        val source = ctx.getRootSource("model/static/GisRegionView")
        val writer = StringWriter()
        ctx.render(source, writer)
        Assertions.assertEquals(
            "export interface GisRegionView {\n" +
                "    readonly left: number;\n" +
                "    readonly top: number;\n" +
                "    readonly right: number;\n" +
                "    readonly bottom: number;\n" +
                "}\n",
            writer.toString()
        )
    }

    companion object {
        private val METADATA = Metadata
            .newBuilder()
            .setOperationParser(OperationParserImpl())
            .setParameterParser(ParameterParserImpl())
            .setGroups(listOf("gisService"))
            .setGenericSupported(true)
            .build()
    }
}