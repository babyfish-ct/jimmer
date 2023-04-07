package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.generator.ts.*;
import org.babyfish.jimmer.client.java.model.*;
import org.babyfish.jimmer.client.java.service.AuthorService;
import org.babyfish.jimmer.client.java.service.BookService;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

public class JTypeScriptTest {

    @Test
    public void testModule() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        new ModuleWriter(ctx).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { Executor } from './';\n" +
                        "\n" +
                        "import { AuthorService, BookService } from './services';\n" +
                        "\n" +
                        "export class Api {\n" +
                        "    \n" +
                        "    readonly authorService: AuthorService;\n" +
                        "    \n" +
                        "    readonly bookService: BookService;\n" +
                        "    \n" +
                        "    constructor(executor: Executor) {\n" +
                        "        this.authorService = new AuthorService(executor);\n" +
                        "        this.bookService = new BookService(executor);\n" +
                        "    }\n" +
                        "}",
                code
        );
    }

    @Test
    public void testModuleErrors() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        new ModuleErrorsWriter(ctx).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { ExportedSavePath } from './model/static';\n" +
                        "\n" +
                        "export type AllErrors = \n" +
                        "    {\n" +
                        "        readonly family: \"BusinessError\",\n" +
                        "        readonly code: \"GLOBAL_TENANT_REQUIRED\"\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"BusinessError\",\n" +
                        "        readonly code: \"ILLEGAL_PATH_NODES\",\n" +
                        "        readonly \"pathNodes\": string\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"BusinessError\",\n" +
                        "        readonly code: \"OUT_OF_RANGE\",\n" +
                        "        readonly \"min\": number,\n" +
                        "        readonly \"max\": number\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"CANNOT_CREATE_TARGET\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"CANNOT_DISSOCIATE_TARGETS\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"FAILED_REMOTE_VALIDATION\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"ILLEGAL_GENERATED_ID\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"ILLEGAL_ID_GENERATOR\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"ILLEGAL_TARGET_ID\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"ILLEGAL_VERSION\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"KEY_NOT_UNIQUE\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"LONG_REMOTE_ASSOCIATION\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"NEITHER_ID_NOR_KEY\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"NO_ID_GENERATOR\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"NO_KEY_PROPS\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"NO_NON_ID_PROPS\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"NO_VERSION\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"NULL_TARGET\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    } | \n" +
                        "    {\n" +
                        "        readonly family: \"SaveErrorCode\",\n" +
                        "        readonly code: \"REVERSED_REMOTE_ASSOCIATION\",\n" +
                        "        readonly \"exportedPath\": ExportedSavePath\n" +
                        "    }\n" +
                        ";\n" +
                        "\n" +
                        "export type ApiErrors = {\n" +
                        "    \"authorService\": {\n" +
                        "        \"findComplexAuthor\": AllErrors & (\n" +
                        "            {\n" +
                        "                readonly family: 'BusinessError',\n" +
                        "                readonly code: 'OUT_OF_RANGE',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'BusinessError',\n" +
                        "                readonly code: 'ILLEGAL_PATH_NODES',\n" +
                        "                readonly [key:string]: any\n" +
                        "            }\n" +
                        "        ),\n" +
                        "        \"findSimpleAuthor\": AllErrors & (\n" +
                        "            {\n" +
                        "                readonly family: 'BusinessError',\n" +
                        "                readonly code: 'GLOBAL_TENANT_REQUIRED',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'BusinessError',\n" +
                        "                readonly code: 'OUT_OF_RANGE',\n" +
                        "                readonly [key:string]: any\n" +
                        "            }\n" +
                        "        )\n" +
                        "    },\n" +
                        "    \"bookService\": {\n" +
                        "        \"saveBooks\": AllErrors & (\n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'NULL_TARGET',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'ILLEGAL_TARGET_ID',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'CANNOT_DISSOCIATE_TARGETS',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'CANNOT_CREATE_TARGET',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'NO_ID_GENERATOR',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'ILLEGAL_ID_GENERATOR',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'ILLEGAL_GENERATED_ID',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'NO_KEY_PROPS',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'NO_NON_ID_PROPS',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'NO_VERSION',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'ILLEGAL_VERSION',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'KEY_NOT_UNIQUE',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'NEITHER_ID_NOR_KEY',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'REVERSED_REMOTE_ASSOCIATION',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'LONG_REMOTE_ASSOCIATION',\n" +
                        "                readonly [key:string]: any\n" +
                        "            } | \n" +
                        "            {\n" +
                        "                readonly family: 'SaveErrorCode',\n" +
                        "                readonly code: 'FAILED_REMOTE_VALIDATION',\n" +
                        "                readonly [key:string]: any\n" +
                        "            }\n" +
                        "        )\n" +
                        "    }\n" +
                        "};\n",
                code
        );
    }

    @Test
    public void testBookService() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        Service service = Constants.JAVA_METADATA.getServices().get(BookService.class);
        new ServiceWriter(ctx, service).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { Dynamic, Executor } from '../';\n" +
                        "import type { AuthorDto, BookDto } from '../model/dto';\n" +
                        "import type { Book } from '../model/entities';\n" +
                        "import type { BookInput, FindBookArguments, Page, Tuple2 } from '../model/static';\n" +
                        "\n" +
                        "/**\n" +
                        " * BookService interface\n" +
                        " */\n" +
                        "export class BookService {\n" +
                        "    \n" +
                        "    constructor(private executor: Executor) {}\n" +
                        "    \n" +
                        "    async deleteBook(options: BookServiceOptions['deleteBook']): Promise<number> {\n" +
                        "        let _uri = '/java/book/';\n" +
                        "        _uri += encodeURIComponent(options.id);\n" +
                        "        return (await this.executor({uri: _uri, method: 'DELETE'})) as number\n" +
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
                        "    async findComplexBooks(options: BookServiceOptions['findComplexBooks']): Promise<\n" +
                        "        ReadonlyArray<BookDto['BookService/COMPLEX_FETCHER']>\n" +
                        "    > {\n" +
                        "        let _uri = '/java/books/complex';\n" +
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
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<BookDto['BookService/COMPLEX_FETCHER']>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async findComplexBooksByArguments(options: BookServiceOptions['findComplexBooksByArguments']): Promise<\n" +
                        "        ReadonlyArray<BookDto['BookService/COMPLEX_FETCHER']>\n" +
                        "    > {\n" +
                        "        let _uri = '/java/books/complex2';\n" +
                        "        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n" +
                        "        let _value: any = undefined;\n" +
                        "        _value = options.arguments.authorNames?.join(',');\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'authorNames='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        _value = options.arguments.maxPrice;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'maxPrice='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        _value = options.arguments.minPrice;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'minPrice='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        _value = options.arguments.name;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'name='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        _value = options.arguments.storeName;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'storeName='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<BookDto['BookService/COMPLEX_FETCHER']>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async findSimpleBooks(): Promise<\n" +
                        "        ReadonlyArray<BookDto['BookService/SIMPLE_FETCHER']>\n" +
                        "    > {\n" +
                        "        let _uri = '/java/books/simple';\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<BookDto['BookService/SIMPLE_FETCHER']>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async findTuples(options: BookServiceOptions['findTuples']): Promise<\n" +
                        "        Page<Tuple2<BookDto['BookService/COMPLEX_FETCHER'], AuthorDto['BookService/AUTHOR_FETCHER'] | undefined>>\n" +
                        "    > {\n" +
                        "        let _uri = '/java/tuples';\n" +
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
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Page<Tuple2<BookDto['BookService/COMPLEX_FETCHER'], AuthorDto['BookService/AUTHOR_FETCHER'] | undefined>>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async saveBooks(options: BookServiceOptions['saveBooks']): Promise<\n" +
                        "        Dynamic<Book>\n" +
                        "    > {\n" +
                        "        let _uri = '/java/book';\n" +
                        "        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Dynamic<Book>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async version(): Promise<number> {\n" +
                        "        let _uri = '/java/version';\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as number\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "export type BookServiceOptions = {\n" +
                        "    'deleteBook': {readonly id: number},\n" +
                        "    'findComplexBooks': {\n" +
                        "        readonly name: string, \n" +
                        "        readonly storeName?: string, \n" +
                        "        readonly authorName?: string, \n" +
                        "        readonly minPrice?: number, \n" +
                        "        readonly maxPrice?: number\n" +
                        "    },\n" +
                        "    'findComplexBooksByArguments': {readonly arguments: FindBookArguments},\n" +
                        "    'findSimpleBooks': {},\n" +
                        "    'findTuples': {\n" +
                        "        \n" +
                        "        /**\n" +
                        "         * Match the book name, optional\n" +
                        "         */\n" +
                        "        readonly name?: string, \n" +
                        "        \n" +
                        "        /**\n" +
                        "         * Start from 0, not 1\n" +
                        "         */\n" +
                        "        readonly pageIndex: number, \n" +
                        "        readonly pageSize: number\n" +
                        "    },\n" +
                        "    'saveBooks': {readonly body: BookInput},\n" +
                        "    'version': {}\n" +
                        "}",
                code
        );
    }

    @Test
    public void testBookServiceByAnonymousMode() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out, true);
        Service service = Constants.JAVA_METADATA.getServices().get(BookService.class);
        new ServiceWriter(ctx, service).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { Dynamic, Executor } from '../';\n" +
                        "import type { Book } from '../model/entities';\n" +
                        "import type { Gender } from '../model/enums';\n" +
                        "import type { BookInput, FindBookArguments, Page, Tuple2 } from '../model/static';\n" +
                        "\n" +
                        "/**\n" +
                        " * BookService interface\n" +
                        " */\n" +
                        "export class BookService {\n" +
                        "    \n" +
                        "    constructor(private executor: Executor) {}\n" +
                        "    \n" +
                        "    async deleteBook(options: {readonly id: number}): Promise<number> {\n" +
                        "        let _uri = '/java/book/';\n" +
                        "        _uri += encodeURIComponent(options.id);\n" +
                        "        return (await this.executor({uri: _uri, method: 'DELETE'})) as number\n" +
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
                        "    async findComplexBooks(options: {\n" +
                        "        readonly name: string, \n" +
                        "        readonly storeName?: string, \n" +
                        "        readonly authorName?: string, \n" +
                        "        readonly minPrice?: number, \n" +
                        "        readonly maxPrice?: number\n" +
                        "    }): Promise<\n" +
                        "        ReadonlyArray<{\n" +
                        "            readonly id: number, \n" +
                        "            readonly name: string, \n" +
                        "            readonly edition: number, \n" +
                        "            readonly price: number, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * The bookstore to which the current book belongs, null is allowd\n" +
                        "             */\n" +
                        "            readonly store?: {\n" +
                        "                readonly id: number, \n" +
                        "                readonly name: string\n" +
                        "            }, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * All authors involved in writing the work\n" +
                        "             */\n" +
                        "            readonly authors: ReadonlyArray<{\n" +
                        "                readonly id: number, \n" +
                        "                readonly firstName: string, \n" +
                        "                readonly lastName: string\n" +
                        "            }>\n" +
                        "        }>\n" +
                        "    > {\n" +
                        "        let _uri = '/java/books/complex';\n" +
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
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<{\n" +
                        "            readonly id: number, \n" +
                        "            readonly name: string, \n" +
                        "            readonly edition: number, \n" +
                        "            readonly price: number, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * The bookstore to which the current book belongs, null is allowd\n" +
                        "             */\n" +
                        "            readonly store?: {\n" +
                        "                readonly id: number, \n" +
                        "                readonly name: string\n" +
                        "            }, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * All authors involved in writing the work\n" +
                        "             */\n" +
                        "            readonly authors: ReadonlyArray<{\n" +
                        "                readonly id: number, \n" +
                        "                readonly firstName: string, \n" +
                        "                readonly lastName: string\n" +
                        "            }>\n" +
                        "        }>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async findComplexBooksByArguments(options: {readonly arguments: FindBookArguments}): Promise<\n" +
                        "        ReadonlyArray<{\n" +
                        "            readonly id: number, \n" +
                        "            readonly name: string, \n" +
                        "            readonly edition: number, \n" +
                        "            readonly price: number, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * The bookstore to which the current book belongs, null is allowd\n" +
                        "             */\n" +
                        "            readonly store?: {\n" +
                        "                readonly id: number, \n" +
                        "                readonly name: string\n" +
                        "            }, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * All authors involved in writing the work\n" +
                        "             */\n" +
                        "            readonly authors: ReadonlyArray<{\n" +
                        "                readonly id: number, \n" +
                        "                readonly firstName: string, \n" +
                        "                readonly lastName: string\n" +
                        "            }>\n" +
                        "        }>\n" +
                        "    > {\n" +
                        "        let _uri = '/java/books/complex2';\n" +
                        "        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n" +
                        "        let _value: any = undefined;\n" +
                        "        _value = options.arguments.authorNames?.join(',');\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'authorNames='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        _value = options.arguments.maxPrice;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'maxPrice='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        _value = options.arguments.minPrice;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'minPrice='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        _value = options.arguments.name;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'name='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        _value = options.arguments.storeName;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'storeName='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<{\n" +
                        "            readonly id: number, \n" +
                        "            readonly name: string, \n" +
                        "            readonly edition: number, \n" +
                        "            readonly price: number, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * The bookstore to which the current book belongs, null is allowd\n" +
                        "             */\n" +
                        "            readonly store?: {\n" +
                        "                readonly id: number, \n" +
                        "                readonly name: string\n" +
                        "            }, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * All authors involved in writing the work\n" +
                        "             */\n" +
                        "            readonly authors: ReadonlyArray<{\n" +
                        "                readonly id: number, \n" +
                        "                readonly firstName: string, \n" +
                        "                readonly lastName: string\n" +
                        "            }>\n" +
                        "        }>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async findSimpleBooks(): Promise<\n" +
                        "        ReadonlyArray<{\n" +
                        "            readonly id: number, \n" +
                        "            readonly name: string, \n" +
                        "            readonly storeId?: number\n" +
                        "        }>\n" +
                        "    > {\n" +
                        "        let _uri = '/java/books/simple';\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as ReadonlyArray<{\n" +
                        "            readonly id: number, \n" +
                        "            readonly name: string, \n" +
                        "            readonly storeId?: number\n" +
                        "        }>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async findTuples(options: {\n" +
                        "        \n" +
                        "        /**\n" +
                        "         * Match the book name, optional\n" +
                        "         */\n" +
                        "        readonly name?: string, \n" +
                        "        \n" +
                        "        /**\n" +
                        "         * Start from 0, not 1\n" +
                        "         */\n" +
                        "        readonly pageIndex: number, \n" +
                        "        readonly pageSize: number\n" +
                        "    }): Promise<\n" +
                        "        Page<Tuple2<{\n" +
                        "            readonly id: number, \n" +
                        "            readonly name: string, \n" +
                        "            readonly edition: number, \n" +
                        "            readonly price: number, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * The bookstore to which the current book belongs, null is allowd\n" +
                        "             */\n" +
                        "            readonly store?: {\n" +
                        "                readonly id: number, \n" +
                        "                readonly name: string\n" +
                        "            }, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * All authors involved in writing the work\n" +
                        "             */\n" +
                        "            readonly authors: ReadonlyArray<{\n" +
                        "                readonly id: number, \n" +
                        "                readonly firstName: string, \n" +
                        "                readonly lastName: string\n" +
                        "            }>\n" +
                        "        }, {\n" +
                        "            readonly id: number, \n" +
                        "            readonly firstName: string, \n" +
                        "            readonly lastName: string, \n" +
                        "            readonly gender: Gender, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * All the books i have written\n" +
                        "             */\n" +
                        "            readonly books: ReadonlyArray<{\n" +
                        "                readonly id: number, \n" +
                        "                readonly name: string, \n" +
                        "                \n" +
                        "                /**\n" +
                        "                 * The bookstore to which the current book belongs, null is allowd\n" +
                        "                 */\n" +
                        "                readonly store?: {\n" +
                        "                    readonly id: number, \n" +
                        "                    readonly name: string\n" +
                        "                }\n" +
                        "            }>\n" +
                        "        } | undefined>>\n" +
                        "    > {\n" +
                        "        let _uri = '/java/tuples';\n" +
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
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Page<Tuple2<{\n" +
                        "            readonly id: number, \n" +
                        "            readonly name: string, \n" +
                        "            readonly edition: number, \n" +
                        "            readonly price: number, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * The bookstore to which the current book belongs, null is allowd\n" +
                        "             */\n" +
                        "            readonly store?: {\n" +
                        "                readonly id: number, \n" +
                        "                readonly name: string\n" +
                        "            }, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * All authors involved in writing the work\n" +
                        "             */\n" +
                        "            readonly authors: ReadonlyArray<{\n" +
                        "                readonly id: number, \n" +
                        "                readonly firstName: string, \n" +
                        "                readonly lastName: string\n" +
                        "            }>\n" +
                        "        }, {\n" +
                        "            readonly id: number, \n" +
                        "            readonly firstName: string, \n" +
                        "            readonly lastName: string, \n" +
                        "            readonly gender: Gender, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * All the books i have written\n" +
                        "             */\n" +
                        "            readonly books: ReadonlyArray<{\n" +
                        "                readonly id: number, \n" +
                        "                readonly name: string, \n" +
                        "                \n" +
                        "                /**\n" +
                        "                 * The bookstore to which the current book belongs, null is allowd\n" +
                        "                 */\n" +
                        "                readonly store?: {\n" +
                        "                    readonly id: number, \n" +
                        "                    readonly name: string\n" +
                        "                }\n" +
                        "            }>\n" +
                        "        } | undefined>>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async saveBooks(options: {readonly body: BookInput}): Promise<\n" +
                        "        Dynamic<Book>\n" +
                        "    > {\n" +
                        "        let _uri = '/java/book';\n" +
                        "        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Dynamic<Book>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async version(): Promise<number> {\n" +
                        "        let _uri = '/java/version';\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as number\n" +
                        "    }\n" +
                        "}",
                code
        );
    }

    @Test
    public void testAuthorService() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        Service service = Constants.JAVA_METADATA.getServices().get(AuthorService.class);
        new ServiceWriter(ctx, service).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { Executor } from '../';\n" +
                        "import type { AuthorDto } from '../model/dto';\n" +
                        "\n" +
                        "export class AuthorService {\n" +
                        "    \n" +
                        "    constructor(private executor: Executor) {}\n" +
                        "    \n" +
                        "    async findComplexAuthor(options: AuthorServiceOptions['findComplexAuthor']): Promise<\n" +
                        "        AuthorDto['AuthorService/COMPLEX_FETCHER'] | undefined\n" +
                        "    > {\n" +
                        "        let _uri = '/author/complex/';\n" +
                        "        _uri += encodeURIComponent(options.id);\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as AuthorDto['AuthorService/COMPLEX_FETCHER'] | undefined\n" +
                        "    }\n" +
                        "    \n" +
                        "    async findSimpleAuthor(options: AuthorServiceOptions['findSimpleAuthor']): Promise<\n" +
                        "        AuthorDto['AuthorService/SIMPLE_FETCHER'] | undefined\n" +
                        "    > {\n" +
                        "        let _uri = '/author/simple/';\n" +
                        "        _uri += encodeURIComponent(options.id);\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as AuthorDto['AuthorService/SIMPLE_FETCHER'] | undefined\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "export type AuthorServiceOptions = {\n" +
                        "    'findComplexAuthor': {readonly id: number},\n" +
                        "    'findSimpleAuthor': {readonly id: number}\n" +
                        "}",
                code
        );
    }

    @Test
    public void testRawBookStore() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        ImmutableObjectType bookStoreType = Constants.JAVA_METADATA.getRawImmutableObjectTypes().get(ImmutableType.get(BookStore.class));
        new TypeDefinitionWriter(ctx, bookStoreType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { Book } from './';\n" +
                        "\n" +
                        "export interface BookStore {\n" +
                        "    \n" +
                        "    readonly id: number;\n" +
                        "    \n" +
                        "    readonly name: string;\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * All books available in this bookstore\n" +
                        "     */\n" +
                        "    readonly books: ReadonlyArray<Book>;\n" +
                        "}\n",
                code
        );
    }

    @Test
    public void testRawBook() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        ImmutableObjectType bookType = Constants.JAVA_METADATA.getRawImmutableObjectTypes().get(ImmutableType.get(Book.class));
        new TypeDefinitionWriter(ctx, bookType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { Author, BookStore } from './';\n" +
                        "\n" +
                        "export interface Book {\n" +
                        "    \n" +
                        "    readonly id: number;\n" +
                        "    \n" +
                        "    readonly name: string;\n" +
                        "    \n" +
                        "    readonly edition: number;\n" +
                        "    \n" +
                        "    readonly price: number;\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * The bookstore to which the current book belongs, null is allowd\n" +
                        "     */\n" +
                        "    readonly store?: BookStore;\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * All authors involved in writing the work\n" +
                        "     */\n" +
                        "    readonly authors: ReadonlyArray<Author>;\n" +
                        "    \n" +
                        "    readonly storeId?: number;\n" +
                        "    \n" +
                        "    readonly authorIds: ReadonlyArray<number>;\n" +
                        "}\n",
                code
        );
    }

    @Test
    public void testRawAuthor() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        ImmutableObjectType authorType = Constants.JAVA_METADATA.getRawImmutableObjectTypes().get(ImmutableType.get(Author.class));
        new TypeDefinitionWriter(ctx, authorType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { Gender } from '../enums';\n" +
                        "import type { Book } from './';\n" +
                        "\n" +
                        "export interface Author {\n" +
                        "    \n" +
                        "    readonly id: number;\n" +
                        "    \n" +
                        "    readonly firstName: string;\n" +
                        "    \n" +
                        "    readonly lastName: string;\n" +
                        "    \n" +
                        "    readonly gender: Gender;\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * All the books i have written\n" +
                        "     */\n" +
                        "    readonly books: ReadonlyArray<Book>;\n" +
                        "}\n",
                code
        );
    }

    @Test
    public void testBookDto() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        new DtoWriter(ctx, Book.class).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "export type BookDto = {\n" +
                        "    'BookService/SIMPLE_FETCHER': {\n" +
                        "        readonly id: number, \n" +
                        "        readonly name: string, \n" +
                        "        readonly storeId?: number\n" +
                        "    }, \n" +
                        "    'BookService/COMPLEX_FETCHER': {\n" +
                        "        readonly id: number, \n" +
                        "        readonly name: string, \n" +
                        "        readonly edition: number, \n" +
                        "        readonly price: number, \n" +
                        "        readonly store?: {\n" +
                        "            readonly id: number, \n" +
                        "            readonly name: string\n" +
                        "        }, \n" +
                        "        readonly authors: ReadonlyArray<{\n" +
                        "            readonly id: number, \n" +
                        "            readonly firstName: string, \n" +
                        "            readonly lastName: string\n" +
                        "        }>\n" +
                        "    }\n" +
                        "}",
                code
        );
    }

    @Test
    public void testAuthorDto() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        new DtoWriter(ctx, Author.class).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { Gender } from '../enums';\n" +
                        "\n" +
                        "export type AuthorDto = {\n" +
                        "    'AuthorService/SIMPLE_FETCHER': {\n" +
                        "        readonly id: number, \n" +
                        "        readonly firstName: string, \n" +
                        "        readonly lastName: string\n" +
                        "    }, \n" +
                        "    'AuthorService/COMPLEX_FETCHER': {\n" +
                        "        readonly id: number, \n" +
                        "        readonly firstName: string, \n" +
                        "        readonly lastName: string, \n" +
                        "        readonly books: ReadonlyArray<{\n" +
                        "            readonly id: number, \n" +
                        "            readonly name: string, \n" +
                        "            readonly edition: number, \n" +
                        "            readonly price: number, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * The bookstore to which the current book belongs, null is allowd\n" +
                        "             */\n" +
                        "            readonly store?: {\n" +
                        "                readonly id: number, \n" +
                        "                readonly name: string\n" +
                        "            }\n" +
                        "        }>\n" +
                        "    }, \n" +
                        "    'BookService/AUTHOR_FETCHER': {\n" +
                        "        readonly id: number, \n" +
                        "        readonly firstName: string, \n" +
                        "        readonly lastName: string, \n" +
                        "        readonly gender: Gender, \n" +
                        "        readonly books: ReadonlyArray<{\n" +
                        "            readonly id: number, \n" +
                        "            readonly name: string, \n" +
                        "            \n" +
                        "            /**\n" +
                        "             * The bookstore to which the current book belongs, null is allowd\n" +
                        "             */\n" +
                        "            readonly store?: {\n" +
                        "                readonly id: number, \n" +
                        "                readonly name: string\n" +
                        "            }\n" +
                        "        }>\n" +
                        "    }\n" +
                        "}",
                code
        );
    }

    @Test
    public void testBookInput() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        StaticObjectType bookInputType = Constants.JAVA_METADATA.getStaticTypes().get(new StaticObjectType.Key(BookInput.class, null));
        new TypeDefinitionWriter(ctx, bookInputType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "export interface BookInput {\n" +
                        "    \n" +
                        "    readonly authorIds: ReadonlyArray<number>;\n" +
                        "    \n" +
                        "    readonly edition: number;\n" +
                        "    \n" +
                        "    readonly name: string;\n" +
                        "    \n" +
                        "    readonly price: number;\n" +
                        "    \n" +
                        "    /**\n" +
                        "     * Null is allowed\n" +
                        "     */\n" +
                        "    readonly storeId?: number;\n" +
                        "}\n",
                code
        );
    }

    @Test
    public void testPage() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        StaticObjectType pageType = Constants.JAVA_METADATA.getGenericTypes().get(Page.class);
        new TypeDefinitionWriter(ctx, pageType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "export interface Page<E> {\n" +
                        "    \n" +
                        "    readonly entities: ReadonlyArray<E>;\n" +
                        "    \n" +
                        "    readonly totalPageCount: number;\n" +
                        "    \n" +
                        "    readonly totalRowCount: number;\n" +
                        "}\n",
                code
        );
    }

    @Test
    public void testTuple2() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        StaticObjectType tupleType = Constants.JAVA_METADATA.getGenericTypes().get(Tuple2.class);
        new TypeDefinitionWriter(ctx, tupleType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "export interface Tuple2<T1, T2> {\n" +
                        "    \n" +
                        "    readonly _1: T1;\n" +
                        "    \n" +
                        "    readonly _2: T2;\n" +
                        "}\n",
                code
        );
    }

    @Test
    public void testGender() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TsContext ctx = createContext(out);
        EnumType genderType = Constants.JAVA_METADATA.getEnumTypes().get(Gender.class);
        new TypeDefinitionWriter(ctx, genderType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "export type Gender = 'MALE' | 'FEMALE';\n",
                code
        );
    }
    
    private static TsContext createContext(OutputStream out, boolean anonymous) {
        return new TsContext(Constants.JAVA_METADATA, out, "Api", 4, anonymous);
    }

    private static TsContext createContext(OutputStream out) {
        return createContext(out, false);
    }
}
