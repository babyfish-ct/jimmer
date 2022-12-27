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

public class JTypeScriptTest {

    @Test
    public void testModule() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
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
    public void testBookService() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        Service service = Constants.JAVA_METADATA.getServices().get(BookService.class);
        new ServiceWriter(ctx, service, false).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { Book } from '../model/entities';\n" +
                        "import type { Page, Tuple2, BookInput } from '../model/static';\n" +
                        "import type { BookDto, AuthorDto } from '../model/dto';\n" +
                        "import type { Executor, Dynamic } from '../';\n" +
                        "\n" +
                        "/**\n" +
                        " * BookService interface\n" +
                        " */\n" +
                        "export class BookService {\n" +
                        "    \n" +
                        "    constructor(private executor: Executor) {}\n" +
                        "    \n" +
                        "    async deleteBook(options: BookServiceOptions['deleteBook']): Promise<number> {\n" +
                        "        let uri = '/java/book/';\n" +
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
                        "    async findComplexBooks(options: BookServiceOptions['findComplexBooks']): Promise<\n" +
                        "        ReadonlyArray<BookDto['BookService/COMPLEX_FETCHER']>\n" +
                        "    > {\n" +
                        "        let uri = '/java/books/complex';\n" +
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
                        "        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<BookDto['BookService/COMPLEX_FETCHER']>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async findSimpleBooks(): Promise<\n" +
                        "        ReadonlyArray<BookDto['BookService/SIMPLE_FETCHER']>\n" +
                        "    > {\n" +
                        "        let uri = '/java/books/simple';\n" +
                        "        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<BookDto['BookService/SIMPLE_FETCHER']>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async findTuples(options: BookServiceOptions['findTuples']): Promise<\n" +
                        "        Page<Tuple2<BookDto['BookService/COMPLEX_FETCHER'], AuthorDto['BookService/AUTHOR_FETCHER']>>\n" +
                        "    > {\n" +
                        "        let uri = '/java/tuples';\n" +
                        "        let separator = '?';\n" +
                        "        if (options.name !== undefined && options.name !== null) {\n" +
                        "            uri += separator;\n" +
                        "            uri += 'name=';\n" +
                        "            uri += encodeURIComponent(options.name);\n" +
                        "            separator = '&';\n" +
                        "        }\n" +
                        "        uri += separator;\n" +
                        "        uri += 'pageIndex=';\n" +
                        "        uri += encodeURIComponent(options.pageIndex);\n" +
                        "        uri += '&pageSize=';\n" +
                        "        uri += encodeURIComponent(options.pageSize);\n" +
                        "        return (await this.executor({uri, method: 'GET'})) as Page<Tuple2<BookDto['BookService/COMPLEX_FETCHER'], AuthorDto['BookService/AUTHOR_FETCHER']>>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async saveBooks(options: BookServiceOptions['saveBooks']): Promise<\n" +
                        "        Dynamic<Book>\n" +
                        "    > {\n" +
                        "        let uri = '/java/book';\n" +
                        "        return (await this.executor({uri, method: 'PUT', body: options.body})) as Dynamic<Book>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async version(): Promise<number> {\n" +
                        "        let uri = '/java/version';\n" +
                        "        return (await this.executor({uri, method: 'GET'})) as number\n" +
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
        Context ctx = createContext(out);
        Service service = Constants.JAVA_METADATA.getServices().get(BookService.class);
        new ServiceWriter(ctx, service, true).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { Book } from '../model/entities';\n" +
                        "import type { Page, Tuple2, BookInput } from '../model/static';\n" +
                        "import type { Gender } from '../model/enums';\n" +
                        "import type { Executor, Dynamic } from '../';\n" +
                        "\n" +
                        "/**\n" +
                        " * BookService interface\n" +
                        " */\n" +
                        "export class BookService {\n" +
                        "    \n" +
                        "    constructor(private executor: Executor) {}\n" +
                        "    \n" +
                        "    async deleteBook(options: {readonly id: number}): Promise<number> {\n" +
                        "        let uri = '/java/book/';\n" +
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
                        "        let uri = '/java/books/complex';\n" +
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
                        "        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<{\n" +
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
                        "            readonly name: string\n" +
                        "        }>\n" +
                        "    > {\n" +
                        "        let uri = '/java/books/simple';\n" +
                        "        return (await this.executor({uri, method: 'GET'})) as ReadonlyArray<{\n" +
                        "            readonly id: number, \n" +
                        "            readonly name: string\n" +
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
                        "        }>>\n" +
                        "    > {\n" +
                        "        let uri = '/java/tuples';\n" +
                        "        let separator = '?';\n" +
                        "        if (options.name !== undefined && options.name !== null) {\n" +
                        "            uri += separator;\n" +
                        "            uri += 'name=';\n" +
                        "            uri += encodeURIComponent(options.name);\n" +
                        "            separator = '&';\n" +
                        "        }\n" +
                        "        uri += separator;\n" +
                        "        uri += 'pageIndex=';\n" +
                        "        uri += encodeURIComponent(options.pageIndex);\n" +
                        "        uri += '&pageSize=';\n" +
                        "        uri += encodeURIComponent(options.pageSize);\n" +
                        "        return (await this.executor({uri, method: 'GET'})) as Page<Tuple2<{\n" +
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
                        "        }>>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async saveBooks(options: {readonly body: BookInput}): Promise<\n" +
                        "        Dynamic<Book>\n" +
                        "    > {\n" +
                        "        let uri = '/java/book';\n" +
                        "        return (await this.executor({uri, method: 'PUT', body: options.body})) as Dynamic<Book>\n" +
                        "    }\n" +
                        "    \n" +
                        "    async version(): Promise<number> {\n" +
                        "        let uri = '/java/version';\n" +
                        "        return (await this.executor({uri, method: 'GET'})) as number\n" +
                        "    }\n" +
                        "}",
                code
        );
    }

    @Test
    public void testAuthorService() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        Service service = Constants.JAVA_METADATA.getServices().get(AuthorService.class);
        new ServiceWriter(ctx, service, false).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { AuthorDto } from '../model/dto';\n" +
                        "import type { Executor } from '../';\n" +
                        "\n" +
                        "export class AuthorService {\n" +
                        "    \n" +
                        "    constructor(private executor: Executor) {}\n" +
                        "    \n" +
                        "    async findComplexAuthor(options: AuthorServiceOptions['findComplexAuthor']): Promise<\n" +
                        "        AuthorDto['AuthorService/COMPLEX_FETCHER'] | undefined\n" +
                        "    > {\n" +
                        "        let uri = '/author/complex/';\n" +
                        "        uri += encodeURIComponent(options.id);\n" +
                        "        return (await this.executor({uri, method: 'GET'})) as AuthorDto['AuthorService/COMPLEX_FETCHER'] | undefined\n" +
                        "    }\n" +
                        "    \n" +
                        "    async findSimpleAuthor(options: AuthorServiceOptions['findSimpleAuthor']): Promise<\n" +
                        "        AuthorDto['AuthorService/SIMPLE_FETCHER'] | undefined\n" +
                        "    > {\n" +
                        "        let uri = '/author/simple/';\n" +
                        "        uri += encodeURIComponent(options.id);\n" +
                        "        return (await this.executor({uri, method: 'GET'})) as AuthorDto['AuthorService/SIMPLE_FETCHER'] | undefined\n" +
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
        Context ctx = createContext(out);
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
                        "}",
                code
        );
    }

    @Test
    public void testRawBook() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        ImmutableObjectType bookType = Constants.JAVA_METADATA.getRawImmutableObjectTypes().get(ImmutableType.get(Book.class));
        new TypeDefinitionWriter(ctx, bookType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { BookStore, Author } from './';\n" +
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
                        "}",
                code
        );
    }

    @Test
    public void testRawAuthor() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        ImmutableObjectType authorType = Constants.JAVA_METADATA.getRawImmutableObjectTypes().get(ImmutableType.get(Author.class));
        new TypeDefinitionWriter(ctx, authorType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "import type { Book } from './';\n" +
                        "import type { Gender } from '../enums';\n" +
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
                        "}",
                code
        );
    }

    @Test
    public void testBookDto() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        new DtoWriter(ctx, Book.class, ctx.getDtoMap().get(Book.class)).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "export type BookDto = {\n" +
                        "    'BookService/SIMPLE_FETCHER': {\n" +
                        "        readonly id: number, \n" +
                        "        readonly name: string\n" +
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
        Context ctx = createContext(out);
        new DtoWriter(ctx, Author.class, ctx.getDtoMap().get(Author.class)).flush();
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
        Context ctx = createContext(out);
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
                        "}",
                code
        );
    }

    @Test
    public void testPage() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
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
                        "}",
                code
        );
    }

    @Test
    public void testTuple2() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        StaticObjectType tupleType = Constants.JAVA_METADATA.getGenericTypes().get(Tuple2.class);
        new TypeDefinitionWriter(ctx, tupleType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "export interface Tuple2<T1, T2> {\n" +
                        "    \n" +
                        "    readonly _1: T1;\n" +
                        "    \n" +
                        "    readonly _2: T2;\n" +
                        "}",
                code
        );
    }

    @Test
    public void testGender() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        EnumType genderType = Constants.JAVA_METADATA.getEnumTypes().get(Gender.class);
        new TypeDefinitionWriter(ctx, genderType).flush();
        String code = out.toString();
        Assertions.assertEquals(
                "export type Gender = 'MALE' | 'FEMALE';\n",
                code
        );
    }
    
    private static Context createContext(OutputStream out) {
        return new Context(Constants.JAVA_METADATA, out, "Api", 4);
    }
}
