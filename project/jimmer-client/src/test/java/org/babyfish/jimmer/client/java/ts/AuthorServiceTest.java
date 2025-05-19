package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.java.service.AuthorService;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;

public class AuthorServiceTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParser(new ParameterParserImpl())
                    .setGroups(Collections.singleton("authorService"))
                    .setGenericSupported(true)
                    .build();

    @Test
    public void testService() {
        Context ctx = new TypeScriptContext(METADATA);
        Source source = ctx.getRootSource("services/" + AuthorService.class.getSimpleName());
        StringWriter writer = new StringWriter();
        ctx.render(source, writer);
        Assertions.assertEquals(
                "import type {Executor} from '../';\n" +
                        "import type {AuthorDto} from '../model/dto/';\n" +
                        "import type {\n" +
                        "    AuthorSpecification, \n" +
                        "    Page, \n" +
                        "    PageRequest, \n" +
                        "    StreamingResponseBody\n" +
                        "} from '../model/static/';\n" +
                        "\n" +
                        "export class AuthorService {\n" +
                        "    \n" +
                        "    constructor(private executor: Executor) {}\n" +
                        "    \n" +
                        "    readonly findAuthorImage: (options: AuthorServiceOptions['findAuthorImage']) => Promise<\n" +
                        "        StreamingResponseBody\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/author/image/';\n" +
                        "        _uri += encodeURIComponent(options.id);\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<StreamingResponseBody>;\n" +
                        "    }\n" +
                        "    \n" +
                        "    readonly findAuthorPage: (options: AuthorServiceOptions['findAuthorPage']) => Promise<\n" +
                        "        Page<AuthorDto['AuthorService/SIMPLE_FETCHER']>\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/author/page';\n" +
                        "        Slet _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n" +
                        "        let _value: any = undefined;\n" +
                        "        _value = options.request.pageIndex;\n" +
                        "        _uri += _separator\n" +
                        "        _uri += 'pageIndex='\n" +
                        "        _uri += encodeURIComponent(_value);\n" +
                        "        _separator = '&';\n" +
                        "        _value = options.request.pageSize;\n" +
                        "        _uri += _separator\n" +
                        "        _uri += 'pageSize='\n" +
                        "        _uri += encodeURIComponent(_value);\n" +
                        "        _separator = '&';\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<Page<AuthorDto['AuthorService/SIMPLE_FETCHER']>>;\n" +
                        "    }\n" +
                        "    \n" +
                        "    readonly findAuthors: (options: AuthorServiceOptions['findAuthors']) => Promise<\n" +
                        "        ReadonlyArray<AuthorDto['AuthorService/SIMPLE_FETCHER']>\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/authors';\n" +
                        "        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n" +
                        "        let _value: any = undefined;\n" +
                        "        _value = options.specification.gender;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'gender='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        _value = options.specification.firstName;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'firstName='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        _value = options.specification.lastName;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'lastName='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<AuthorDto['AuthorService/SIMPLE_FETCHER']>>;\n" +
                        "    }\n" +
                        "    \n" +
                        "    readonly findIssue574Author: (options: AuthorServiceOptions['findIssue574Author']) => Promise<\n" +
                        "        AuthorDto['AuthorService/ISSUE_574_FETCHER'] | undefined\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/author/issue_574/';\n" +
                        "        _uri += encodeURIComponent(options.id);\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<AuthorDto['AuthorService/ISSUE_574_FETCHER'] | undefined>;\n" +
                        "    }\n" +
                        "    \n" +
                        "    readonly findSimpleAuthor: (options: AuthorServiceOptions['findSimpleAuthor']) => Promise<\n" +
                        "        AuthorDto['AuthorService/SIMPLE_FETCHER'] | undefined\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/author/simple/';\n" +
                        "        _uri += encodeURIComponent(options.id);\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<AuthorDto['AuthorService/SIMPLE_FETCHER'] | undefined>;\n" +
                        "    }\n" +
                        "    \n" +
                        "    readonly findSimpleAuthorById: (options: AuthorServiceOptions['findSimpleAuthorById']) => Promise<\n" +
                        "        AuthorDto['AuthorService/SIMPLE_FETCHER'] | undefined\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/author/simple/byId';\n" +
                        "        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n" +
                        "        let _value: any = undefined;\n" +
                        "        _value = options.id;\n" +
                        "        _uri += _separator\n" +
                        "        _uri += 'id='\n" +
                        "        _uri += encodeURIComponent(_value);\n" +
                        "        _separator = '&';\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<AuthorDto['AuthorService/SIMPLE_FETCHER'] | undefined>;\n" +
                        "    }\n" +
                        "    \n" +
                        "    readonly findSimpleAuthorByName: (options: AuthorServiceOptions['findSimpleAuthorByName']) => Promise<\n" +
                        "        AuthorDto['AuthorService/SIMPLE_FETCHER'] | undefined\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/author/simple/byName';\n" +
                        "        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n" +
                        "        let _value: any = undefined;\n" +
                        "        _value = options.name;\n" +
                        "        _uri += _separator\n" +
                        "        _uri += 'name='\n" +
                        "        _uri += encodeURIComponent(_value);\n" +
                        "        _separator = '&';\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<AuthorDto['AuthorService/SIMPLE_FETCHER'] | undefined>;\n" +
                        "    }\n" +
                        "    \n" +
                        "    readonly findSimpleAuthorByOptionalName: (options: AuthorServiceOptions['findSimpleAuthorByOptionalName']) => Promise<\n" +
                        "        AuthorDto['AuthorService/SIMPLE_FETCHER'] | undefined\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/author/simple/byOptionalName';\n" +
                        "        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n" +
                        "        let _value: any = undefined;\n" +
                        "        _value = options.name;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'name='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<AuthorDto['AuthorService/SIMPLE_FETCHER'] | undefined>;\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "export type AuthorServiceOptions = {\n" +
                        "    'findSimpleAuthor': {\n" +
                        "        readonly id: number\n" +
                        "    }, \n" +
                        "    'findSimpleAuthorById': {\n" +
                        "        readonly id: number\n" +
                        "    }, \n" +
                        "    'findSimpleAuthorByName': {\n" +
                        "        readonly name: string\n" +
                        "    }, \n" +
                        "    'findSimpleAuthorByOptionalName': {\n" +
                        "        readonly name?: string | undefined\n" +
                        "    }, \n" +
                        "    'findIssue574Author': {\n" +
                        "        readonly id: number\n" +
                        "    }, \n" +
                        "    'findAuthorImage': {\n" +
                        "        readonly id: number\n" +
                        "    }, \n" +
                        "    'findAuthors': {\n" +
                        "        readonly specification: AuthorSpecification\n" +
                        "    }, \n" +
                        "    'findAuthorPage': {\n" +
                        "        readonly request: PageRequest<AuthorSpecification>\n" +
                        "    }\n" +
                        "}\n",
                writer.toString()
        );
    }

    @Test
    public void testDto() {
        Context ctx = new TypeScriptContext(METADATA);
        Source source = ctx.getRootSource("model/dto/AuthorDto");
        StringWriter writer = new StringWriter();
        ctx.render(source, writer);
        Assertions.assertEquals(
                "import type {FullName} from '../embeddable/';\n" +
                        "import type {Gender} from '../enums/';\n" +
                        "\n" +
                        "export type AuthorDto = {\n" +
                        "    'AuthorService/ISSUE_574_FETCHER': {\n" +
                        "        readonly id: string;\n" +
                        "        readonly gender: Gender;\n" +
                        "    }, \n" +
                        "    /**\n" +
                        "     * Simple author DTO\n" +
                        "     */\n" +
                        "    'AuthorService/SIMPLE_FETCHER': {\n" +
                        "        readonly id: string;\n" +
                        "        readonly fullName: FullName;\n" +
                        "    }\n" +
                        "}\n",
                writer.toString()
        );
    }

    @Test
    public void testGender() {
        Context ctx = new TypeScriptContext(METADATA);
        Source source = ctx.getRootSource("model/enums/Gender");
        StringWriter writer = new StringWriter();
        ctx.render(source, writer);
        Assertions.assertEquals(
                "export const Gender_CONSTANTS = [\n" +
                        "    /**\n" +
                        "     * BOYS\n" +
                        "     */\n" +
                        "    'MALE', \n" +
                        "    /**\n" +
                        "     * GIRLS\n" +
                        "     */\n" +
                        "    'FEMALE'\n" +
                        "] as const;\n" +
                        "/**\n" +
                        " * The gender, which can only be `MALE` or `FEMALE`\n" +
                        " */\n" +
                        "export type Gender = typeof Gender_CONSTANTS[number];\n",
                writer.toString()
        );
    }
}
