package org.babyfish.jimmer.client.generator;

import org.babyfish.jimmer.client.EnableImplicitApi;
import org.babyfish.jimmer.client.meta.Api;

public class NoMetadataException extends RuntimeException {

    public NoMetadataException() {
        super(
                "There no exported API to render client code, is the `groups` illegal?\n" +
                        "- If it is, please specify concurrent `groups`, \n" +
                        "- otherwise, please choose one solution: \n" +
                        "  - Use \"@" + EnableImplicitApi.class.getName() + "\" to decorate any class; \n" +
                        "  - Use \"@" + Api.class.getName() + "\" to decorates all controllers and all operations. \n"
        );
    }
}
