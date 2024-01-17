package org.babyfish.jimmer.client.java.ts;

import org.babyfish.jimmer.client.common.OperationParserImpl;
import org.babyfish.jimmer.client.common.ParameterParserImpl;
import org.babyfish.jimmer.client.generator.Context;
import org.babyfish.jimmer.client.generator.ts.TypeScriptContext;
import org.babyfish.jimmer.client.java.model.Customer;
import org.babyfish.jimmer.client.java.service.CustomerService;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.source.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;

public class CustomerServiceTest {

    private static final Metadata METADATA =
            Metadata
                    .newBuilder()
                    .setOperationParser(new OperationParserImpl())
                    .setParameterParameter(new ParameterParserImpl())
                    .setGroups(Collections.singleton("customerService"))
                    .setGenericSupported(true)
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
                        "\n" +
                        "export class CustomerService {\n" +
                        "    \n" +
                        "    constructor(private executor: Executor) {}\n" +
                        "    \n" +
                        "    async findCustomers(options: CustomerServiceOptions['findCustomers']): Promise<\n" +
                        "        {readonly [key:string]: CustomerDto['CustomerService/DEFAULT_CUSTOMER']}\n" +
                        "    > {\n" +
                        "        let _uri = '/';\n" +
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
                        "}\n" +
                        "export type CustomerServiceOptions = {\n" +
                        "    'findCustomers': {\n" +
                        "        readonly name?: string | undefined\n" +
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
                        "    /**\n" +
                        "     */\n" +
                        "    'CustomerService/DEFAULT_CUSTOMER': {\n" +
                        "        readonly id: number;\n" +
                        "        readonly name: string;\n" +
                        "        readonly contact?: Contact | undefined;\n" +
                        "    }\n" +
                        "}\n",
                writer.toString()
        );
    }
}
