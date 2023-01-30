package org.babyfish.jimmer.meta.impl.dto.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DtoTest {

    @Test
    public void test() {
        List<Dto> dtoList = Dto.parse(
                        "input BookInput {\n" +
                        "    #allScalars\n" +
                        "    -tenant\n" +
                        "    id(store)\n" +
                        "    id(authors) as authorIds\n" +
                        "}\n" +
                        "input CompositeBookInput {\n" +
                        "    #allScalars(Book)\n" +
                        "    -tenant\n" +
                        "    id(store)\n" +
                        "    id(authors) as authorIds\n" +
                        "    chapters {\n" +
                        "        #allScalars\n" +
                        "    }\n" +
                        "}"
        );
        assertContentEquals(
                "[" +
                        "--->Dto{" +
                        "--->--->isInput=true, " +
                        "--->--->name='BookInput', " +
                        "--->--->allScalars=true, " +
                        "--->--->allScalarImmutableTypes=null, " +
                        "--->--->explicitProps=[" +
                        "--->--->--->DtoProp{" +
                        "--->--->--->--->negative=true, " +
                        "--->--->--->--->name='tenant', " +
                        "--->--->--->--->idOnly=false, " +
                        "--->--->--->--->alias='null', " +
                        "--->--->--->--->targetDto=null, " +
                        "--->--->--->--->recursive=false" +
                        "--->--->--->}, " +
                        "--->--->--->DtoProp{" +
                        "--->--->--->--->negative=false, " +
                        "--->--->--->--->name='store', " +
                        "--->--->--->--->idOnly=true, " +
                        "--->--->--->--->alias='null', " +
                        "--->--->--->--->targetDto=null, " +
                        "--->--->--->--->recursive=false" +
                        "--->--->--->}, " +
                        "--->--->--->DtoProp{" +
                        "--->--->--->--->negative=false, " +
                        "--->--->--->--->name='authors', " +
                        "--->--->--->--->idOnly=true, " +
                        "--->--->--->--->alias='authorIds', " +
                        "--->--->--->--->targetDto=null, " +
                        "--->--->--->--->recursive=false" +
                        "--->--->--->}" +
                        "--->--->]" +
                        "--->}, " +
                        "--->Dto{" +
                        "--->--->isInput=true, " +
                        "--->--->name='CompositeBookInput', " +
                        "--->--->allScalars=true, " +
                        "--->--->allScalarImmutableTypes=[Book], " +
                        "--->--->explicitProps=[" +
                        "--->--->--->DtoProp{" +
                        "--->--->--->--->negative=true, " +
                        "--->--->--->--->name='tenant', " +
                        "--->--->--->--->idOnly=false, " +
                        "--->--->--->--->alias='null', " +
                        "--->--->--->--->targetDto=null, " +
                        "--->--->--->--->recursive=false" +
                        "--->--->--->}, " +
                        "--->--->--->DtoProp{" +
                        "--->--->--->--->negative=false, " +
                        "--->--->--->--->name='store', " +
                        "--->--->--->--->idOnly=true, " +
                        "--->--->--->--->alias='null', " +
                        "--->--->--->--->targetDto=null, " +
                        "--->--->--->--->recursive=false" +
                        "--->--->--->}, " +
                        "--->--->--->DtoProp{" +
                        "--->--->--->--->negative=false, " +
                        "--->--->--->--->name='authors', " +
                        "--->--->--->--->idOnly=true, " +
                        "--->--->--->--->alias='authorIds', " +
                        "--->--->--->--->targetDto=null, " +
                        "--->--->--->--->recursive=false" +
                        "--->--->--->}, " +
                        "--->--->--->DtoProp{" +
                        "--->--->--->--->negative=false, " +
                        "--->--->--->--->name='chapters', " +
                        "--->--->--->--->idOnly=false, " +
                        "--->--->--->--->alias='null', " +
                        "--->--->--->--->targetDto=Dto{" +
                        "--->--->--->--->--->isInput=true, " +
                        "--->--->--->--->--->name='null', " +
                        "--->--->--->--->--->allScalars=true, " +
                        "--->--->--->--->--->allScalarImmutableTypes=null, " +
                        "--->--->--->--->--->explicitProps=[]" +
                        "--->--->--->--->}, " +
                        "--->--->--->--->recursive=false" +
                        "--->--->--->}" +
                        "--->--->]" +
                        "--->}" +
                        "]".replace("--->", ""),
                dtoList.toString()
        );
    }

    private static void assertContentEquals(String expected, String actual) {
        Assertions.assertEquals(
                expected.replace("--->", ""),
                actual
        );
    }
}
