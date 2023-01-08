package org.babyfish.jimmer.client.java.feign;

import org.babyfish.jimmer.client.generator.java.feign.FeignContext;
import org.babyfish.jimmer.client.generator.java.feign.DtoWriter;
import org.babyfish.jimmer.client.java.model.Book;
import org.babyfish.jimmer.client.meta.Constants;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class JFeignTest {

    @Test
    public void testBookDto() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new DtoWriter(createContext(out), Book.class).flush();
        String code = out.toString();
        System.out.println(code);
    }

    private static FeignContext createContext(OutputStream out) {
        return new FeignContext(Constants.JAVA_METADATA, out, "Api", 4, "com.myapp");
    }
}
