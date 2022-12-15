package org.babyfish.jimmer.client.ts;

import org.babyfish.jimmer.client.generator.ts.*;
import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.model.Book;
import org.babyfish.jimmer.client.model.BookInput;
import org.babyfish.jimmer.client.model.Gender;
import org.babyfish.jimmer.client.model.Page;
import org.babyfish.jimmer.client.service.BookService;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TypeScriptTest {

    @Test
    public void testModuleWriter() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        new ModuleWriter(ctx).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testServiceWriter() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        Service service = Constants.METADATA.getServices().get(BookService.class);
        new ServiceWriter(ctx, service).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testRawBook() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        ImmutableObjectType bookType = Constants.METADATA.getRawImmutableObjectTypes().get(ImmutableType.get(Book.class));
        new TypeDefinitionWriter(ctx, bookType).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testBookInput() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        StaticObjectType bookInputType = Constants.METADATA.getStaticTypes().get(new StaticObjectType.Key(BookInput.class, null));
        new TypeDefinitionWriter(ctx, bookInputType).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testPage() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        StaticObjectType pageType = Constants.METADATA.getGenericTypes().get(Page.class);
        new TypeDefinitionWriter(ctx, pageType).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testTuple2() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        StaticObjectType tupleType = Constants.METADATA.getGenericTypes().get(Tuple2.class);
        new TypeDefinitionWriter(ctx, tupleType).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void testGender() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context ctx = createContext(out);
        EnumType genderType = Constants.METADATA.getEnumTypes().get(Gender.class);
        new TypeDefinitionWriter(ctx, genderType).flush();
        String code = out.toString();
        System.out.println(code);
    }

    @Test
    public void writeAll() throws IOException {
        FileOutputStream out = new FileOutputStream("/Users/chentao/tmp/ts.zip");
        try {
            new TypeScriptGenerator().generate(Constants.METADATA, out);
        } finally {
            out.close();
        }
    }
    
    private static Context createContext(OutputStream out) {
        return new Context(Constants.METADATA, out, "Api", 4);
    }
}
