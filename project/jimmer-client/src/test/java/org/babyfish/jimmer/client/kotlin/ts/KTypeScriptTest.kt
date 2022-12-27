package org.babyfish.jimmer.client.kotlin.ts

import org.babyfish.jimmer.client.generator.ts.*
import org.babyfish.jimmer.client.kotlin.model.*
import org.babyfish.jimmer.client.kotlin.service.KBookService
import org.babyfish.jimmer.client.meta.Constants
import org.babyfish.jimmer.client.meta.StaticObjectType
import org.babyfish.jimmer.meta.ImmutableType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class KTypeScriptTest {

    @Test
    fun testModule() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        ModuleWriter(ctx).flush()
        val code = out.toString()
        Assertions.assertEquals(
            "import type { Executor } from './';\n" +
                "\n" +
                "import { KBookService } from './services';\n" +
                "\n" +
                "export class Api {\n" +
                "    \n" +
                "    readonly kbookService: KBookService;\n" +
                "    \n" +
                "    constructor(executor: Executor) {\n" +
                "        this.kbookService = new KBookService(executor);\n" +
                "    }\n" +
                "}",
            code
        )
    }

    @Test
    fun testKBookService() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        val service = Constants.KOTLIN_METADATA.services[KBookService::class.java]
        ServiceWriter(ctx, service, false).flush()
        val code = out.toString()
        Assertions.assertEquals(
            "import type { KBook } from '../model/entities';\n" +
                "import type { KPage, Tuple2, KBookInput } from '../model/static';\n" +
                "import type { KBookDto, KAuthorDto } from '../model/dto';\n" +
                "import type { Executor, Dynamic } from '../';\n" +
                "\n" +
                "/**\n" +
                " * BookService interface\n" +
                " */\n" +
                "export class KBookService {\n" +
                "    \n" +
                "    constructor(private executor: Executor) {}\n" +
                "    \n" +
                "    async deleteBook(options: KBookServiceOptions['deleteBook']): Promise<number> {\n" +
                "        let uri = '/book/';\n" +
                "        uri += encodeURIComponent(options.id);\n" +
                "        return (await this.executor({uri, method: 'DELETE'})) as number\n" +
                "    }\n" +
                "    \n" +
                "    /**\n" +
                "     * Find book list\n" +
                "     * \n" +
                "     * Format of each element:\n" +
                "     * -   id\n" +
                "     * -   name\n" +
                "     * -   edition\n" +
                "     * -   price\n" +
                "     * -   store\n" +
                "     *     -   id\n" +
                "     *     -   name\n" +
                "     * -   authors\n" +
                "     *     -   id\n" +
                "     *     -   firstName\n" +
                "     */\n" +
                "    async findComplexBooks(options: KBookServiceOptions['findComplexBooks']): Promise<\n" +
                "        ReadonlyArray<KBookDto['KBookService/COMPLEX_FETCHER']>\n" +
                "    > {\n" +
                "        let uri = '/books/complex';\n" +
                "        uri += '?name=';\n" +
                "        uri += encodeURIComponent(options.name);\n" +
                "        if (options.storeName !== undefined && options.storeName !== null) {\n" +
                "            uri += '&storeName=';\n" +
                "            uri += encodeURIComponent(options.storeName);\n" +
                "        }\n" +
                "        if (options.authorName !== undefined && options.authorName !== null) {\n" +
                "            uri += '&authorName=';\n" +
                "            uri += encodeURIComponent(options.authorName);\n" +
                "        }\n" +
                "        if (options.minPrice !== undefined && options.minPrice !== null) {\n" +
                "            uri += '&minPrice=';\n" +
                "            uri += encodeURIComponent(options.minPrice);\n" +
                "        }\n" +
                "        if (options.maxPrice !== undefined && options.maxPrice !== null) {\n" +
                "            uri += '&maxPrice=';\n" +
                "            uri += encodeURIComponent(options.maxPrice);\n" +
                "        }\n" +
                "        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<KBookDto['KBookService/COMPLEX_FETCHER']>\n" +
                "    }\n" +
                "    \n" +
                "    async findSimpleBooks(): Promise<\n" +
                "        ReadonlyArray<KBookDto['KBookService/SIMPLE_FETCHER']>\n" +
                "    > {\n" +
                "        let uri = '/books/simple';\n" +
                "        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<KBookDto['KBookService/SIMPLE_FETCHER']>\n" +
                "    }\n" +
                "    \n" +
                "    async findTuples(options: KBookServiceOptions['findTuples']): Promise<\n" +
                "        KPage<Tuple2<KBookDto['KBookService/COMPLEX_FETCHER'], KAuthorDto['KBookService/AUTHOR_FETCHER']>>\n" +
                "    > {\n" +
                "        let uri = '/tuples';\n" +
                "        uri += '?name=';\n" +
                "        uri += encodeURIComponent(options.name);\n" +
                "        if (options.pageIndex !== undefined && options.pageIndex !== null) {\n" +
                "            uri += '&pageIndex=';\n" +
                "            uri += encodeURIComponent(options.pageIndex);\n" +
                "        }\n" +
                "        uri += '&pageSize=';\n" +
                "        uri += encodeURIComponent(options.pageSize);\n" +
                "        return (await this.executor({uri, method: 'GET'})) as KPage<Tuple2<KBookDto['KBookService/COMPLEX_FETCHER'], KAuthorDto['KBookService/AUTHOR_FETCHER']>>\n" +
                "    }\n" +
                "    \n" +
                "    async saveBooks(options: KBookServiceOptions['saveBooks']): Promise<\n" +
                "        Dynamic<KBook> | undefined\n" +
                "    > {\n" +
                "        let uri = '/book';\n" +
                "        return (await this.executor({uri, method: 'PUT', body: options.body})) as Dynamic<KBook> | undefined\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "export type KBookServiceOptions = {\n" +
                "    'deleteBook': {readonly id: number},\n" +
                "    'findComplexBooks': {\n" +
                "        readonly name: string, \n" +
                "        readonly storeName?: string, \n" +
                "        readonly authorName?: string, \n" +
                "        readonly minPrice?: number, \n" +
                "        readonly maxPrice?: number\n" +
                "    },\n" +
                "    'findSimpleBooks': {},\n" +
                "    'findTuples': {\n" +
                "        \n" +
                "        /**\n" +
                "         * Match the book name, optional\n" +
                "         */\n" +
                "        readonly name: string, \n" +
                "        \n" +
                "        /**\n" +
                "         * Start from 0, not 1\n" +
                "         */\n" +
                "        readonly pageIndex?: number, \n" +
                "        readonly pageSize: number\n" +
                "    },\n" +
                "    'saveBooks': {readonly body: KBookInput}\n" +
                "}",
            code
        )
    }

    @Test
    fun testRawKBookStore() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        val bookStoreType = Constants.KOTLIN_METADATA.rawImmutableObjectTypes[ImmutableType.get(KBookStore::class.java)]
        TypeDefinitionWriter(ctx, bookStoreType).flush()
        val code = out.toString()
        Assertions.assertEquals(
            "import type { KBook } from './';\n" +
                "\n" +
                "export interface KBookStore {\n" +
                "    \n" +
                "    readonly id: number;\n" +
                "    \n" +
                "    readonly name?: string;\n" +
                "    \n" +
                "    readonly books: ReadonlyArray<KBook>;\n" +
                "}",
            code
        )
    }

    @Test
    fun testRawKBook() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        val bookType = Constants.KOTLIN_METADATA.rawImmutableObjectTypes[ImmutableType.get(KBook::class.java)]
        TypeDefinitionWriter(ctx, bookType).flush()
        val code = out.toString()
        Assertions.assertEquals(
            "import type { KBookStore, KAuthor } from './';\n" +
                "\n" +
                "export interface KBook {\n" +
                "    \n" +
                "    readonly id: number;\n" +
                "    \n" +
                "    readonly name?: string;\n" +
                "    \n" +
                "    readonly edition: number;\n" +
                "    \n" +
                "    readonly price?: number;\n" +
                "    \n" +
                "    readonly store?: KBookStore;\n" +
                "    \n" +
                "    readonly authors: ReadonlyArray<KAuthor>;\n" +
                "}",
            code
        )
    }

    @Test
    fun testRawKAuthor() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        val authorType = Constants.KOTLIN_METADATA.rawImmutableObjectTypes[ImmutableType.get(KAuthor::class.java)]
        TypeDefinitionWriter(ctx, authorType).flush()
        val code = out.toString()
        Assertions.assertEquals(
            "import type { KBook } from './';\n" +
                "import type { KGender } from '../enums';\n" +
                "\n" +
                "export interface KAuthor {\n" +
                "    \n" +
                "    readonly id: number;\n" +
                "    \n" +
                "    readonly firstName: string;\n" +
                "    \n" +
                "    readonly lastName: string;\n" +
                "    \n" +
                "    readonly gender?: KGender;\n" +
                "    \n" +
                "    readonly books: ReadonlyArray<KBook>;\n" +
                "}",
            code
        )
    }

    @Test
    fun testBookDto() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        DtoWriter(ctx, KBook::class.java, ctx.dtoMap[KBook::class.java]).flush()
        val code = out.toString()
        Assertions.assertEquals(
            "import type { KGender } from '../enums';\n" +
                "\n" +
                "export type KBookDto = {\n" +
                "    'KBookService/SIMPLE_FETCHER': {\n" +
                "        readonly id: number, \n" +
                "        readonly name?: string\n" +
                "    }, \n" +
                "    'KBookService/COMPLEX_FETCHER': {\n" +
                "        readonly id: number, \n" +
                "        readonly name?: string, \n" +
                "        readonly edition: number, \n" +
                "        readonly price?: number, \n" +
                "        readonly store?: {\n" +
                "            readonly id: number, \n" +
                "            readonly name?: string\n" +
                "        }, \n" +
                "        readonly authors: ReadonlyArray<{\n" +
                "            readonly id: number, \n" +
                "            readonly firstName: string, \n" +
                "            readonly lastName: string, \n" +
                "            readonly gender?: KGender\n" +
                "        }>\n" +
                "    }\n" +
                "}",
            code
        )
    }

    @Test
    fun testKAuthorDto() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        DtoWriter(ctx, KAuthor::class.java, ctx.dtoMap[KAuthor::class.java]).flush()
        val code = out.toString()
        Assertions.assertEquals(
            "import type { KGender } from '../enums';\n" +
                "\n" +
                "export type KAuthorDto = {\n" +
                "    'KBookService/AUTHOR_FETCHER': {\n" +
                "        readonly id: number, \n" +
                "        readonly firstName: string, \n" +
                "        readonly lastName: string, \n" +
                "        readonly gender?: KGender, \n" +
                "        readonly books: ReadonlyArray<{\n" +
                "            readonly id: number, \n" +
                "            readonly name?: string, \n" +
                "            readonly edition: number, \n" +
                "            readonly price?: number, \n" +
                "            readonly store?: {\n" +
                "                readonly id: number, \n" +
                "                readonly name?: string\n" +
                "            }\n" +
                "        }>\n" +
                "    }\n" +
                "}",
            code
        )
    }

    @Test
    fun testKBookInput() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        val bookInputType = Constants.KOTLIN_METADATA.staticTypes[StaticObjectType.Key(KBookInput::class.java, null)]
        TypeDefinitionWriter(ctx, bookInputType).flush()
        val code = out.toString()
        Assertions.assertEquals(
            "export interface KBookInput {\n" +
                "    \n" +
                "    readonly authorIds: ReadonlyArray<number>;\n" +
                "    \n" +
                "    readonly edition: number;\n" +
                "    \n" +
                "    readonly name: string;\n" +
                "    \n" +
                "    readonly price: number;\n" +
                "    \n" +
                "    readonly storeId?: number;\n" +
                "}",
            code
        )
    }

    @Test
    fun testKPage() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        val pageType = Constants.KOTLIN_METADATA.staticTypes[StaticObjectType.Key(KPage::class.java, null)]
        TypeDefinitionWriter(ctx, pageType).flush()
        val code = out.toString()
        Assertions.assertEquals(
            "export interface KPage<E> {\n" +
                "    \n" +
                "    readonly entities: ReadonlyArray<E>;\n" +
                "    \n" +
                "    readonly totalPageCount: number;\n" +
                "    \n" +
                "    readonly totalRowCount: number;\n" +
                "}",
            code
        )
    }

    @Test
    fun testKGender() {
        val out = ByteArrayOutputStream()
        val ctx = createContext(out)
        val genderType = Constants.KOTLIN_METADATA.enumTypes[KGender::class.java]
        TypeDefinitionWriter(ctx, genderType).flush()
        val code = out.toString()
        Assertions.assertEquals(
            "export type KGender = 'MALE' | 'FEMALE';\n",
            code
        )
    }

    private fun createContext(out: OutputStream): Context {
        return Context(Constants.KOTLIN_METADATA, out, "Api", 4)
    }
}