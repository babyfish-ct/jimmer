package org.babyfish.jimmer.client.kotlin.ts

import org.babyfish.jimmer.client.generator.Context
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext
import org.babyfish.jimmer.client.common.OperationParserImpl
import org.babyfish.jimmer.client.common.ParameterParserImpl
import org.babyfish.jimmer.client.kotlin.service.KBookService
import org.babyfish.jimmer.client.runtime.Metadata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.StringWriter

class KBookServiceTest {

    @Test
    fun testService() {
        val ctx: Context = TypeScriptContext(METADATA)
        val source = ctx.getRootSource("services/" + KBookService::class.simpleName)
        val writer = StringWriter()
        ctx.render(source, writer)
        Assertions.assertEquals(
            "import type {Executor} from '../';\n" +
                "import type {KAuthorDto, KBookDto} from '../model/dto/';\n" +
                "import type {Dynamic_KBook} from '../model/dynamic/';\n" +
                "import type {KBookInput, KPage, Tuple2} from '../model/static/';\n" +
                "\n" +
                "/**\n" +
                " * BookService interface\n" +
                " */\n" +
                "export class KBookService {\n" +
                "    \n" +
                "    constructor(private executor: Executor) {}\n" +
                "    \n" +
                "    async deleteBook(options: KBookServiceOptions['deleteBook']): Promise<\n" +
                "        number\n" +
                "    > {\n" +
                "        let _uri = '/book/';\n" +
                "        _uri += encodeURIComponent(options.id);\n" +
                "        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<number>;\n" +
                "    }\n" +
                "    \n" +
                "    /**\n" +
                "     * @return A list contains complex DTOs\n" +
                "     */\n" +
                "    async findComplexBooks(options: KBookServiceOptions['findComplexBooks']): Promise<\n" +
                "        ReadonlyArray<KBookDto['KBookService/COMPLEX_FETCHER']>\n" +
                "    > {\n" +
                "        let _uri = '/books/complex';\n" +
                "        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n" +
                "        let _value: any = undefined;\n" +
                "        _value = options.name;\n" +
                "        if (_value !== undefined && _value !== null) {\n" +
                "            _uri += _separator\n" +
                "            _uri += 'name='\n" +
                "            _uri += encodeURIComponent(_value);\n" +
                "            _separator = '&';\n" +
                "        }\n" +
                "        _value = options.storeName;\n" +
                "        if (_value !== undefined && _value !== null) {\n" +
                "            _uri += _separator\n" +
                "            _uri += 'storeName='\n" +
                "            _uri += encodeURIComponent(_value);\n" +
                "            _separator = '&';\n" +
                "        }\n" +
                "        _value = options.authorName;\n" +
                "        if (_value !== undefined && _value !== null) {\n" +
                "            _uri += _separator\n" +
                "            _uri += 'authorName='\n" +
                "            _uri += encodeURIComponent(_value);\n" +
                "            _separator = '&';\n" +
                "        }\n" +
                "        _value = options.minPrice;\n" +
                "        if (_value !== undefined && _value !== null) {\n" +
                "            _uri += _separator\n" +
                "            _uri += 'minPrice='\n" +
                "            _uri += encodeURIComponent(_value);\n" +
                "            _separator = '&';\n" +
                "        }\n" +
                "        _value = options.maxPrice;\n" +
                "        if (_value !== undefined && _value !== null) {\n" +
                "            _uri += _separator\n" +
                "            _uri += 'maxPrice='\n" +
                "            _uri += encodeURIComponent(_value);\n" +
                "            _separator = '&';\n" +
                "        }\n" +
                "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<KBookDto['KBookService/COMPLEX_FETCHER']>>;\n" +
                "    }\n" +
                "    \n" +
                "    /**\n" +
                "     * @return A list contains simple DTOs\n" +
                "     */\n" +
                "    async findSimpleBooks(): Promise<\n" +
                "        ReadonlyArray<KBookDto['KBookService/SIMPLE_FETCHER']>\n" +
                "    > {\n" +
                "        let _uri = '/books/simple';\n" +
                "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<KBookDto['KBookService/SIMPLE_FETCHER']>>;\n" +
                "    }\n" +
                "    \n" +
                "    async findTuples(options: KBookServiceOptions['findTuples']): Promise<\n" +
                "        KPage<Tuple2<KBookDto['KBookService/COMPLEX_FETCHER'], KAuthorDto['KBookService/AUTHOR_FETCHER']>>\n" +
                "    > {\n" +
                "        let _uri = '/tuples';\n" +
                "        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n" +
                "        let _value: any = undefined;\n" +
                "        _value = options.name;\n" +
                "        if (_value !== undefined && _value !== null) {\n" +
                "            _uri += _separator\n" +
                "            _uri += 'name='\n" +
                "            _uri += encodeURIComponent(_value);\n" +
                "            _separator = '&';\n" +
                "        }\n" +
                "        _value = options.pageIndex;\n" +
                "        if (_value !== undefined && _value !== null) {\n" +
                "            _uri += _separator\n" +
                "            _uri += 'pageIndex='\n" +
                "            _uri += encodeURIComponent(_value);\n" +
                "            _separator = '&';\n" +
                "        }\n" +
                "        _value = options.pageSize;\n" +
                "        if (_value !== undefined && _value !== null) {\n" +
                "            _uri += _separator\n" +
                "            _uri += 'pageSize='\n" +
                "            _uri += encodeURIComponent(_value);\n" +
                "            _separator = '&';\n" +
                "        }\n" +
                "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<KPage<Tuple2<KBookDto['KBookService/COMPLEX_FETCHER'], KAuthorDto['KBookService/AUTHOR_FETCHER']>>>;\n" +
                "    }\n" +
                "    \n" +
                "    async saveBook(options: KBookServiceOptions['saveBook']): Promise<\n" +
                "        Dynamic_KBook\n" +
                "    > {\n" +
                "        let _uri = '/book';\n" +
                "        return (await this.executor({uri: _uri, method: 'POST', body: options.body})) as Promise<Dynamic_KBook>;\n" +
                "    }\n" +
                "    \n" +
                "    async updateBook(options: KBookServiceOptions['updateBook']): Promise<\n" +
                "        Dynamic_KBook\n" +
                "    > {\n" +
                "        let _uri = '/book';\n" +
                "        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<Dynamic_KBook>;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "export type KBookServiceOptions = {\n" +
                "    'findSimpleBooks': {}, \n" +
                "    'findComplexBooks': {\n" +
                "        readonly name?: string | undefined, \n" +
                "        readonly storeName?: string | undefined, \n" +
                "        readonly authorName?: string | undefined, \n" +
                "        readonly minPrice?: number | undefined, \n" +
                "        readonly maxPrice?: number | undefined\n" +
                "    }, \n" +
                "    'findTuples': {\n" +
                "        readonly name?: string | undefined, \n" +
                "        readonly pageIndex: number, \n" +
                "        readonly pageSize: number\n" +
                "    }, \n" +
                "    'saveBook': {\n" +
                "        readonly body: KBookInput\n" +
                "    }, \n" +
                "    'updateBook': {\n" +
                "        readonly body: KBookInput\n" +
                "    }, \n" +
                "    'deleteBook': {\n" +
                "        readonly id: number\n" +
                "    }\n" +
                "}\n",
            writer.toString()
        )
    }

    @Test
    fun testModelDto() {
        val ctx: Context = TypeScriptContext(METADATA)
        val source = ctx.getRootSource("model/dto/KBookDto")
        val writer = StringWriter()
        ctx.render(source, writer)
        Assertions.assertEquals(
            "import type {KCoordinate} from '../embeddable/';\n" +
                "import type {KGender} from '../enums/';\n" +
                "\n" +
                "export type KBookDto = {\n" +
                "    /**\n" +
                "     * Complex Book DTO\n" +
                "     */\n" +
                "    'KBookService/COMPLEX_FETCHER': {\n" +
                "        /**\n" +
                "         * The id is long, but the client type is string\n" +
                "         * because JS cannot retain large long values\n" +
                "         */\n" +
                "        readonly id: string;\n" +
                "        /**\n" +
                "         * Created time\n" +
                "         */\n" +
                "        readonly createdTime: string;\n" +
                "        /**\n" +
                "         * Modified time\n" +
                "         */\n" +
                "        readonly modifiedTime: string;\n" +
                "        /**\n" +
                "         * The name of this book,\n" +
                "         * <p>Together with `edition`, this property forms the key of the book</p>\n" +
                "         */\n" +
                "        readonly name?: string | undefined;\n" +
                "        /**\n" +
                "         * The edition of this book,\n" +
                "         *  <p>Together with `name`, this property forms the key of the book</p>\n" +
                "         */\n" +
                "        readonly edition: number;\n" +
                "        /**\n" +
                "         * The price of this book\n" +
                "         */\n" +
                "        readonly price?: number | undefined;\n" +
                "        /**\n" +
                "         * The bookstore to which the current book belongs, null is allowed\n" +
                "         */\n" +
                "        readonly store?: {\n" +
                "            readonly id: number;\n" +
                "            readonly name?: string | undefined;\n" +
                "            readonly coordinate: KCoordinate;\n" +
                "            readonly level: number;\n" +
                "        } | null | undefined;\n" +
                "        /**\n" +
                "         * All authors involved in writing the work\n" +
                "         */\n" +
                "        readonly authors: ReadonlyArray<{\n" +
                "            /**\n" +
                "             * The id is long, but the client type is string\n" +
                "             * because JS cannot retain large long values\n" +
                "             */\n" +
                "            readonly id: string;\n" +
                "            /**\n" +
                "             * The first name of this author\n" +
                "             *  <p>Together with `lastName`, this property forms the key of the book</p>\n" +
                "             */\n" +
                "            readonly firstName: string;\n" +
                "            /**\n" +
                "             * The last name of this author\n" +
                "             *  <p>Together with `firstName`, this property forms the key of the book</p>\n" +
                "             */\n" +
                "            readonly lastName: string;\n" +
                "            readonly gender: KGender;\n" +
                "        }>;\n" +
                "    }\n" +
                "    /**\n" +
                "     * Simple Book DTO\n" +
                "     */\n" +
                "    'KBookService/SIMPLE_FETCHER': {\n" +
                "        /**\n" +
                "         * The id is long, but the client type is string\n" +
                "         * because JS cannot retain large long values\n" +
                "         */\n" +
                "        readonly id: string;\n" +
                "        /**\n" +
                "         * The name of this book,\n" +
                "         * <p>Together with `edition`, this property forms the key of the book</p>\n" +
                "         */\n" +
                "        readonly name?: string | undefined;\n" +
                "    }\n" +
                "}\n",
            writer.toString()
        )
    }

    @Test
    fun testKCoordinate() {
        val ctx: Context = TypeScriptContext(METADATA)
        val source = ctx.getRootSource("model/embeddable/KCoordinate")
        val writer = StringWriter()
        ctx.render(source, writer)
        Assertions.assertEquals(
            "export interface KCoordinate {\n" +
                "    /**\n" +
                "     * The latitude, from -180 to +180\n" +
                "     */\n" +
                "    readonly longitude: number;\n" +
                "    /**\n" +
                "     * The latitude, from -90 to +90\n" +
                "     */\n" +
                "    readonly latitude: number;\n" +
                "}\n",
            writer.toString()
        )
    }

    @Test
    fun testDynamicBook() {
        val ctx: Context = TypeScriptContext(METADATA)
        val source = ctx.getRootSource("model/dynamic/Dynamic_KBook")
        val writer = StringWriter()
        ctx.render(source, writer)
        Assertions.assertEquals(
            "import type {Dynamic_KAuthor, Dynamic_KBookStore} from './';\n" +
                "\n" +
                "/**\n" +
                " * The book object\n" +
                " * \n" +
                " */\n" +
                "export interface Dynamic_KBook {\n" +
                "    /**\n" +
                "     * The id is long, but the client type is string\n" +
                "     * because JS cannot retain large long values\n" +
                "     */\n" +
                "    readonly id?: string;\n" +
                "    /**\n" +
                "     * The name of this book,\n" +
                "     * <p>Together with `edition`, this property forms the key of the book</p>\n" +
                "     */\n" +
                "    readonly name?: string | undefined;\n" +
                "    /**\n" +
                "     * The edition of this book,\n" +
                "     *  <p>Together with `name`, this property forms the key of the book</p>\n" +
                "     */\n" +
                "    readonly edition?: number;\n" +
                "    /**\n" +
                "     * The price of this book\n" +
                "     */\n" +
                "    readonly price?: number | undefined;\n" +
                "    /**\n" +
                "     * The bookstore to which the current book belongs, null is allowed\n" +
                "     */\n" +
                "    readonly store?: Dynamic_KBookStore | undefined;\n" +
                "    /**\n" +
                "     * All authors involved in writing the work\n" +
                "     */\n" +
                "    readonly authors?: ReadonlyArray<Dynamic_KAuthor>;\n" +
                "}\n",
            writer.toString()
        )
    }

    @Test
    fun testPage() {
        val ctx: Context = TypeScriptContext(METADATA)
        val source = ctx.getRootSource("model/static/KPage")
        val writer = StringWriter()
        ctx.render(source, writer)
        Assertions.assertEquals(
            "/**\n" +
                " * The page object\n" +
                " */\n" +
                "export interface KPage<E> {\n" +
                "    /**\n" +
                "     * Total row count before paging\n" +
                "     */\n" +
                "    readonly totalRowCount: number;\n" +
                "    /**\n" +
                "     * Total page count before paging\n" +
                "     */\n" +
                "    readonly totalPageCount: number;\n" +
                "    /**\n" +
                "     * The entities in the current page\n" +
                "     */\n" +
                "    readonly entities: ReadonlyArray<E>;\n" +
                "}\n",
            writer.toString()
        )
    }

    @Test
    fun testGender() {
        val ctx: Context = TypeScriptContext(METADATA)
        val source = ctx.getRootSource("model/enums/KGender")
        val writer = StringWriter()
        ctx.render(source, writer)
        Assertions.assertEquals(
            "export const KGender_CONSTANTS = [\n" +
                "    /**\n" +
                "     * Boys\n" +
                "     */\n" +
                "    'MALE', \n" +
                "    /**\n" +
                "     * Girls\n" +
                "     */\n" +
                "    'FEMALE'\n" +
                "] as const;\n" +
                "/**\n" +
                " * The gender, which can only be `MALE` or `FEMALE`\n" +
                " * \n" +
                " */\n" +
                "export type KGender = typeof KGender_CONSTANTS[number];\n",
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