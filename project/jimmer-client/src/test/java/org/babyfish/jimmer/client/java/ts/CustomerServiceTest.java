package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.java.model.Customer;
import org.babyfish.jimmer.client.java.service.CustomerLoginInfo;
import org.babyfish.jimmer.client.java.service.CustomerService;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.VirtualType;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.StringWriter;
import java.util.Collections;

public class CustomerServiceTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParser(new ParameterParserImpl())
                    .setGroups(Collections.singleton("customerService"))
                    .setGenericSupported(true)
                    .setVirtualTypeMap(
                            Collections.singletonMap(
                                    TypeName.of(MultipartFile.class),
                                    VirtualType.FILE
                            )
                    )
                    .build();

    @Test
    public void testCustomerService() {
        Context ctx = new TypeScriptContext(METADATA);
        Source source = ctx.getRootSource("services/" + CustomerService.class.getSimpleName());
        StringWriter writer = new StringWriter();
        ctx.render(source, writer);
        Assertions.assertEquals(
                "import type {Executor} from '../';\n" +
                        "import type {CustomerDto} from '../model/dto/';\n" +
                        "import type {CustomerInput, CustomerLoginInfo} from '../model/static/';\n" +
                        "\n" +
                        "export class CustomerService {\n" +
                        "    \n" +
                        "    constructor(private executor: Executor) {}\n" +
                        "    \n" +
                        "    readonly addImage: (options: CustomerServiceOptions['addImage']) => Promise<\n" +
                        "        void\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/customer/image';\n" +
                        "        const _formData = new FormData();\n" +
                        "        const _body = options.body;\n" +
                        "        _formData.append(\"newFile\", _body.file);\n" +
                        "        return (await this.executor({uri: _uri, method: 'PATCH', body: _formData})) as Promise<void>;\n" +
                        "    }\n" +
                        "    \n" +
                        "    readonly changeImage: (options: CustomerServiceOptions['changeImage']) => Promise<\n" +
                        "        void\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/customer/images/';\n" +
                        "        _uri += encodeURIComponent(options.index);\n" +
                        "        const _formData = new FormData();\n" +
                        "        const _body = options.body;\n" +
                        "        _formData.append(\"file\", _body.file);\n" +
                        "        return (await this.executor({uri: _uri, method: 'PATCH', body: _formData})) as Promise<void>;\n" +
                        "    }\n" +
                        "    \n" +
                        "    readonly findCustomers: (options: CustomerServiceOptions['findCustomers']) => Promise<\n" +
                        "        {readonly [key:string]: CustomerDto['CustomerService/DEFAULT_CUSTOMER']}\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/customers';\n" +
                        "        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n" +
                        "        let _value: any = undefined;\n" +
                        "        _value = options.name;\n" +
                        "        if (_value !== undefined && _value !== null) {\n" +
                        "            _uri += _separator\n" +
                        "            _uri += 'name='\n" +
                        "            _uri += encodeURIComponent(_value);\n" +
                        "            _separator = '&';\n" +
                        "        }\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<{readonly [key:string]: CustomerDto['CustomerService/DEFAULT_CUSTOMER']}>;\n" +
                        "    }\n" +
                        "    \n" +
                        "    readonly login: (options: CustomerServiceOptions['login']) => Promise<\n" +
                        "        void\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/login';\n" +
                        "        let _separator = _uri.indexOf('?') === -1 ? '?' : '&';\n" +
                        "        let _value: any = undefined;\n" +
                        "        _value = options.info.userName;\n" +
                        "        _uri += _separator\n" +
                        "        _uri += 'userName='\n" +
                        "        _uri += encodeURIComponent(_value);\n" +
                        "        _separator = '&';\n" +
                        "        _value = options.info.password;\n" +
                        "        _uri += _separator\n" +
                        "        _uri += 'password='\n" +
                        "        _uri += encodeURIComponent(_value);\n" +
                        "        _separator = '&';\n" +
                        "        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<void>;\n" +
                        "    }\n" +
                        "    \n" +
                        "    readonly saveCustomer: (options: CustomerServiceOptions['saveCustomer']) => Promise<\n" +
                        "        {readonly [key:string]: number}\n" +
                        "    > = async(options) => {\n" +
                        "        let _uri = '/customer';\n" +
                        "        const _formData = new FormData();\n" +
                        "        const _body = options.body;\n" +
                        "        if (_body.input) {\n" +
                        "            _formData.append(\n" +
                        "                \"input\", \n" +
                        "                new Blob(\n" +
                        "                    [JSON.stringify(_body.input)], \n" +
                        "                    {type: \"application/json\"}\n" +
                        "                )\n" +
                        "            );\n" +
                        "        }\n" +
                        "        for (const file of _body.files) {\n" +
                        "            _formData.append(\"files\", file);\n" +
                        "        }\n" +
                        "        return (await this.executor({uri: _uri, method: 'POST', body: _formData})) as Promise<{readonly [key:string]: number}>;\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "export type CustomerServiceOptions = {\n" +
                        "    'findCustomers': {\n" +
                        "        readonly name?: string | undefined\n" +
                        "    }, \n" +
                        "    'saveCustomer': {\n" +
                        "        readonly body: {\n" +
                        "            readonly input?: CustomerInput | undefined, \n" +
                        "            readonly files: ReadonlyArray<File>\n" +
                        "        }\n" +
                        "    }, \n" +
                        "    'changeImage': {\n" +
                        "        readonly index: number, \n" +
                        "        readonly body: {\n" +
                        "            readonly file: File\n" +
                        "        }\n" +
                        "    }, \n" +
                        "    'addImage': {\n" +
                        "        readonly body: {\n" +
                        "            readonly file: File\n" +
                        "        }\n" +
                        "    }, \n" +
                        "    'login': {\n" +
                        "        readonly info: CustomerLoginInfo\n" +
                        "    }\n" +
                        "}\n",
                writer.toString()
        );
    }

    @Test
    public void testCustomerDto() {
        Context ctx = new TypeScriptContext(METADATA);
        Source source = ctx.getRootSource("model/dto/" + Customer.class.getSimpleName() + "Dto");
        StringWriter writer = new StringWriter();
        ctx.render(source, writer);
        Assertions.assertEquals(
                "import type {Contact} from '../embeddable/';\n" +
                "\n" +
                "export type CustomerDto = {\n" +
                "    'CustomerService/DEFAULT_CUSTOMER': {\n" +
                "        readonly id: number;\n" +
                "        readonly name: string;\n" +
                "        readonly contact?: Contact | undefined;\n" +
                "    }\n" +
                "}\n",
                writer.toString()
        );
    }

    @Test
    public void testCustomerLoginInfo() {
        Context ctx = new TypeScriptContext(METADATA);
        Source source = ctx.getRootSource("model/static/" + CustomerLoginInfo.class.getSimpleName());
        StringWriter writer = new StringWriter();
        ctx.render(source, writer);
        Assertions.assertEquals(
                "export interface CustomerLoginInfo {\n" +
                        "    readonly userName: string;\n" +
                        "    readonly password: string;\n" +
                        "}\n",
                writer.toString()
        );
    }
}
