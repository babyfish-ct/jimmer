package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DtoCompilerTest {

    @Test
    public void testSimple() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
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
                        "}// End"
        );
        assertContentEquals(
                ("[" +
                        "--->input BookInput {" +
                        "--->--->@optional id, " +
                        "--->--->name, " +
                        "--->--->edition, " +
                        "--->--->price, " +
                        "--->--->id(store) as storeId, " +
                        "--->--->id(authors) as authorIds" +
                        "--->}, " +
                        "--->input CompositeBookInput {" +
                        "--->--->@optional id, " +
                        "--->--->name, " +
                        "--->--->edition, " +
                        "--->--->price, " +
                        "--->--->id(store) as storeId, " +
                        "--->--->id(authors) as authorIds, " +
                        "--->--->chapters: input {" +
                        "--->--->--->@optional id, " +
                        "--->--->--->index, " +
                        "--->--->--->title" +
                        "--->--->}" +
                        "--->}" +
                        "]").replace("--->", ""),
                dtoTypes.toString()
        );
    }

    @Test
    public void testOptionalAllScalars() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
                "input BookSpecification {\n" +
                        "    #allScalars?" +
                        "}\n"
        );
        assertContentEquals(
                "input BookSpecification {" +
                        "--->@optional id, " +
                        "--->@optional name, " +
                        "--->@optional edition, " +
                        "--->@optional price, " +
                        "--->@optional tenant" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testRequiredAllScalars() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
                "input BookSpecification {\n" +
                        "    #allScalars!" +
                        "}\n"
        );
        assertContentEquals(
                "input BookSpecification {" +
                        "--->@required id, " +
                        "--->@required name, " +
                        "--->@required edition, " +
                        "--->@required price, " +
                        "--->@required tenant" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testInputRequired() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
                "input BookInput {\n" +
                        "    id!\n" +
                        "    id(store)\n" +
                        "}\n"
        );
        assertContentEquals(
                "input BookInput {@required id, id(store) as storeId}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testInputOnlyRequired() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
                "inputOnly BookInput {\n" +
                        "    id!\n" +
                        "    id(store)!\n" +
                        "}\n"
        );
        assertContentEquals(
                "inputOnly BookInput {@required id, @required id(store) as storeId}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testRecursive() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.treeNode().compile(
                "input TreeNodeInput {" +
                        "    name" +
                        "    childNodes {" +
                        "        name" +
                        "    }*" +
                        "}"
        );
        assertContentEquals(
                "[" +
                        "--->input TreeNodeInput {" +
                        "--->--->name, " +
                        "--->--->@optional childNodes: input {" +
                        "--->--->--->name, " +
                        "--->--->--->@optional childNodes: ..." +
                        "--->--->}*" +
                        "--->}" +
                        "]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testExtends() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
                "abstract input BookKeyInput {\n" +
                        "    name\n" +
                        "    edition\n" +
                        "}\n" +
                        "abstract input CommonInput {\n" +
                        "    price\n" +
                        "    tenant\n" +
                        "}\n" +
                        "input BookInput : CommonInput, BookKeyInput {\n" +
                        "    -price\n" +
                        "    id(store)\n" +
                        "    id(authors) as authorIds\n" +
                        "}\n" +
                        "input CompositeInput: BookInput {" +
                        "    id(authors) as authorIdList\n" +
                        "    chapters {\n" +
                        "        #allScalars\n" +
                        "        -id\n" +
                        "    }\n" +
                        "}\n"
        );
        assertContentEquals(
                "[" +
                        "--->input BookInput {" +
                        "--->--->tenant, " +
                        "--->--->name, " +
                        "--->--->edition, " +
                        "--->--->id(store) as storeId, " +
                        "--->--->id(authors) as authorIds" +
                        "--->}, " +
                        "--->input CompositeInput {" +
                        "--->--->tenant, " +
                        "--->--->name, " +
                        "--->--->edition, " +
                        "--->--->id(store) as storeId, " +
                        "--->--->id(authors) as authorIdList, " +
                        "--->--->chapters: input {" +
                        "--->--->--->index, " +
                        "--->--->--->title" +
                        "--->--->}" +
                        "--->}" +
                        "]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testOverride() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
                "abstract A {\n" +
                        "    store {\n" +
                        "        id\n" +
                        "    }\n" +
                        "}\n" +
                        "abstract B {\n" +
                        "    id(store)\n" +
                        "}\n" +
                        "BookView : A, B {\n" +
                        "    store {\n" +
                        "        name\n" +
                        "    }\n" +
                        "}\n"
        );
        assertContentEquals(
                "[BookView {store: {name}}]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testOverride2() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
                "abstract A {\n" +
                        "    id as data\n" +
                        "}\n" +
                        "abstract B {\n" +
                        "    id(store) as data\n" +
                        "}\n" +
                        "BookView : A, B {\n" +
                        "    authors as data {\n" +
                        "        firstName\n" +
                        "        lastName\n" +
                        "    }\n" +
                        "}\n"
        );
        assertContentEquals(
                "[BookView {authors as data: {firstName, lastName}}]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testFlat1() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
                "BookFlatView {\n" +
                        "    id\n" +
                        "    name?\n" +
                        "    flat(store) {\n" +
                        "        as(^ -> parent) {\n" +
                        "            #allScalars\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        );
        assertContentEquals(
                "[" +
                        "--->BookFlatView {" +
                        "--->--->id, " +
                        "--->--->@optional name, " +
                        "--->--->@optional store.id as parentId, " +
                        "--->--->@optional store.name as parentName, " +
                        "--->--->@optional store.website as parentWebsite" +
                        "--->}" +
                        "]",
                dtoTypes.toString()
        );
        assertContentEquals(
                "[" +
                        "--->@optional flat(store): {" +
                        "--->--->id as parentId, " +
                        "--->--->name as parentName, " +
                        "--->--->website as parentWebsite" +
                        "--->}" +
                        "]",
                dtoTypes.get(0).getHiddenFlatProps().toString()
        );
    }

    @Test
    public void testFlat2() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.treeNode().compile(
                "FlatTreeNode {\n" +
                        "    #allScalars\n" +
                        "    flat(parent) {\n" +
                        "        as(^ -> parent) {\n" +
                        "            #allScalars\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        );
        assertContentEquals(
                "FlatTreeNode {" +
                        "--->id, " +
                        "--->name, " +
                        "--->@optional parent.id as parentId, " +
                        "--->@optional parent.name as parentName" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testFlat3() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
                "BookFlatView {\n" +
                        "    id\n" +
                        "    name\n" +
                        "    flat(store) {\n" +
                        "        as(^ -> parent) {\n" +
                        "            id\n" +
                        "            name\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        );
        assertContentEquals(
                "[" +
                        "--->BookFlatView {" +
                        "--->--->id, " +
                        "--->--->name, " +
                        "--->--->@optional store.id as parentId, " +
                        "--->--->@optional store.name as parentName" +
                        "--->}" +
                        "]",
                dtoTypes.toString()
        );
        assertContentEquals(
                "[" +
                        "--->@optional flat(store): {" +
                        "--->--->id as parentId, " +
                        "--->--->name as parentName" +
                        "--->}" +
                        "]",
                dtoTypes.get(0).getHiddenFlatProps().toString()
        );
    }

    @Test
    public void testFlat4() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
                "BookFlatView {\n" +
                        "    id\n" +
                        "    name\n" +
                        "    flat(creator) {\n" +
                        "        as(^ -> created) {\n" +
                        "            #allScalars\n" +
                        "            gender\n" +
                        "        }\n" +
                        "    }\n" +
                        "    flat(editor) {\n" +
                        "        as(^ -> modified) {\n" +
                        "            #allScalars\n" +
                        "            gender\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        );
        assertContentEquals(
                "[" +
                        "--->BookFlatView {" +
                        "--->--->id, " +
                        "--->--->name, " +
                        "--->--->creator.id as createdId, " +
                        "--->--->creator.name as createdName, " +
                        "--->--->creator.gender as createdGender, " +
                        "--->--->editor.id as modifiedId, " +
                        "--->--->editor.name as modifiedName, " +
                        "--->--->editor.gender as modifiedGender" +
                        "--->}" +
                        "]",
                dtoTypes.toString()
        );
        assertContentEquals(
                "[" +
                        "--->flat(creator): {" +
                        "--->--->id as createdId, " +
                        "--->--->name as createdName, " +
                        "--->--->gender as createdGender" +
                        "--->}, " +
                        "--->flat(editor): {" +
                        "--->--->id as modifiedId, " +
                        "--->--->name as modifiedName, " +
                        "--->--->gender as modifiedGender" +
                        "--->}" +
                        "]",
                dtoTypes.get(0).getHiddenFlatProps().toString()
        );
    }

    @Test
    public void testUserProp() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
                "import com.company.pkg.data.User\n" +
                        "import com.company.pkg.data.Configuration as Cfg\n" +
                        "import com.company.pkg.data.{\n" +
                        "    ConfigurationKey as _K\n, " +
                        "    NestedElement as _E\n" +
                        "}\n" +
                        "import java.util.SequencedSet as _OSet\n" +
                        "import com.company.pkg.TopLevel as _T\n" +
                        "\n" +
                        "input-only Customer {\n" +
                        "    a: Int\n" +
                        "    b: User?\n" +
                        "    c: MutableList<Cfg>\n" +
                        "    d: List<Map<_K, Array<_E?>>>?\n" +
                        "    e: _OSet<_T.Nested>\n" +
                        "}"
        );
        assertContentEquals(
                "inputOnly Customer {" +
                        "    a: Int, " +
                        "    b: com.company.pkg.data.User?, " +
                        "    c: MutableList<com.company.pkg.data.Configuration>, " +
                        "    d: List<" +
                        "        Map<" +
                        "            com.company.pkg.data.ConfigurationKey, " +
                        "            Array<" +
                        "                com.company.pkg.data.NestedElement?" +
                        "            >" +
                        "        >" +
                        "    >?, " +
                        "    e: java.util.SequencedSet<com.company.pkg.TopLevel.Nested>" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testAnnotation() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book().compile(
                "import org.framework.annotations.{A, B, C, D}\n" +
                        "import org.framework.annotations.{Shallow, Deep}\n" +
                        "import org.framework.enums.{A as EnumA, B as EnumB}\n" +
                        "\n" +
                        "@Shallow\n" +
                        "Book {\n" +
                        "    @Shallow\n" +
                        "    name\n" +
                        "    flat(store) {\n" +
                        "        as(^ -> parent) {\n" +
                        "            @Deep" +
                        "            name\n" +
                        "        }\n" +
                        "    }\n" +
                        "    @A(\"name1\", device = \"Com\" + \"piler\", level = 3)\n" +
                        "    @B(\n" +
                        "        value = \"name2\", \n" +
                        "        phases=[\"Com\" + \"pile\", \"Run\" + \"time\"]\n" +
                        "    )\n" +
                        "    @B(\n" +
                        "        value = \"name3\", \n" +
                        "        phases={\"Dep\" + \"loy\", \n\"Run\" + \"time\"}, \n" +
                        "        items=[\n" +
                        "            @C({EnumA.CHOICE_1, EnumA.CHOICE_2}),\n" +
                        "            C([EnumB.CHOICE_1, EnumB.CHOICE_2]),\n" +
                        "            @D,\n" +
                        "            D()\n" +
                        "        ]\n" +
                        "    )\n" +
                        "    tags: MutableList<String>\n" +
                        "}"
        );
        assertContentEquals(
                "@org.framework.annotations.Shallow " +
                        "Book {" +
                        "--->@org.framework.annotations.Shallow " +
                        "--->name, " +
                        "--->@optional " +
                        "--->@org.framework.annotations.Deep " +
                        "--->store.name as parentName, " +
                        "--->@org.framework.annotations.A(" +
                        "--->--->value = \"name1\", " +
                        "--->--->device = \"Compiler\", " +
                        "--->--->level = 3" +
                        "--->)" +
                        "--->@org.framework.annotations.B(" +
                        "--->--->value = \"name2\", " +
                        "--->--->phases = [\"Compile\", \"Runtime\"]" +
                        "--->)" +
                        "--->@org.framework.annotations.B(" +
                        "--->--->value = \"name3\", " +
                        "--->--->phases = [\"Deploy\", \"Runtime\"], " +
                        "--->--->items = [" +
                        "--->--->--->@org.framework.annotations.C(" +
                        "--->--->--->--->value = [" +
                        "--->--->--->--->--->org.framework.enums.A.CHOICE_1, " +
                        "--->--->--->--->--->org.framework.enums.A.CHOICE_2" +
                        "--->--->--->--->]" +
                        "--->--->--->), " +
                        "--->--->--->@org.framework.annotations.C(" +
                        "--->--->--->--->value = [" +
                        "--->--->--->--->--->org.framework.enums.B.CHOICE_1, " +
                        "--->--->--->--->--->org.framework.enums.B.CHOICE_2" +
                        "--->--->--->--->]" +
                        "--->--->--->), " +
                        "--->--->--->@org.framework.annotations.D, " +
                        "--->--->--->@org.framework.annotations.D" +
                        "--->--->]" +
                        "--->) " +
                        "--->alias: MutableList<String>" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testIllegalPropertyName() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookView {\n" +
                            "    city\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 2 of \"src/main/dto/pkg/Book.dto\": " +
                        "There is no property \"city\" in \"org.babyfish.jimmer.sql.model.Book\" or its super types",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalSyntax() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookView {\n" +
                            "    #<allScalars>\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 2 of \"src/main/dto/pkg/Book.dto\": " +
                        "extraneous input '<' expecting Identifier",
                ex.getMessage()
        );
    }

    @Test
    public void testDuplicateAlias() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookView {\n" +
                            "    id\n" +
                            "    name as id\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 3 of \"src/main/dto/pkg/Book.dto\": " +
                        "Duplicated property alias \"id\"",
                ex.getMessage()
        );
    }

    @Test
    public void testDuplicateAlias2() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookView {\n" +
                            "    id\n" +
                            "    name\n" +
                            "    flat(store) {\n" +
                            "        name\n" +
                            "    }\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 5 of \"src/main/dto/pkg/Book.dto\": " +
                        "Duplicated property alias \"name\"",
                ex.getMessage()
        );
    }

    @Test
    public void testDuplicateAlias3() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookView {\n" +
                            "    id\n" +
                            "    name as myName\n" +
                            "    myName: String\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 4 of \"src/main/dto/pkg/Book.dto\": " +
                        "Duplicated property alias \"myName\"",
                ex.getMessage()
        );
    }

    @Test
    public void testReferenceTwice() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookView {\n" +
                            "    authors {\n" +
                            "        #allScalars\n" +
                            "    }\n" +
                            "    id(authors) as authorIds\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 5 of \"src/main/dto/pkg/Book.dto\": " +
                        "Base property \"entity::authors\" cannot be referenced twice",
                ex.getMessage()
        );
    }

    @Test
    public void testNoBody() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookView {\n" +
                            "    flat(store)\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 2 of \"src/main/dto/pkg/Book.dto\": " +
                        "Illegal property \"store\", the child body is required",
                ex.getMessage()
        );
    }

    @Test
    public void testUnnecessaryBody() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookView {\n" +
                            "    id(store) {\n" +
                            "        id\n" +
                            "}\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 2 of \"src/main/dto/pkg/Book.dto\": " +
                        "Illegal property \"store\", child body cannot be specified by it is id view property",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalIdFunc() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookView {\n" +
                            "    id(name)\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 2 of \"src/main/dto/pkg/Book.dto\": " +
                        "Cannot call the function \"id\" because the current " +
                        "prop \"entity::name\" is not entity level association property",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalFlatFunc() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookView {\n" +
                            "    flat(authors)\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 2 of \"src/main/dto/pkg/Book.dto\": " +
                        "Cannot call the function \"flat\" " +
                        "because the current prop \"entity::authors\" is list",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalRequired() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "input BookSpecification {\n" +
                            "    id\n" +
                            "    id(store)!\n" +
                            "}\n"
            );
        });
        Assertions.assertEquals(
                "Error at line 3 of \"src/main/dto/pkg/Book.dto\": " +
                        "Illegal required modifier '!' for non-id property, " +
                        "it can only be used in input-only type",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalRecursiveFunc() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookView {\n" +
                            "    store {\n" +
                            "        name\n" +
                            "    }*\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 4 of \"src/main/dto/pkg/Book.dto\": " +
                        "Illegal symbol \"*\", the property \"store\" is not recursive",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalRecursiveFunc2() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "Book {\n" +
                            "    name\n" +
                            "    flat(store) {\n" +
                            "        name\n" +
                            "    }*\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 5 of \"src/main/dto/pkg/Book.dto\": " +
                        "Illegal symbol \"*\", the property \"store\" is not recursive",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalAlias() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "Book {\n" +
                            "    name\n" +
                            "    flat(store) as parent {\n" +
                            "        name as parentName\n" +
                            "    }\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 3 of \"src/main/dto/pkg/Book.dto\": " +
                        "The alias cannot be specified when the function `flat` is used",
                ex.getMessage()
        );
    }

    @Test
    public void testMustOverride() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "abstract A { id(store) }\n" +
                            "abstract B { store {name} }\n" +
                            "Book : A, B {\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 3 of \"src/main/dto/pkg/Book.dto\": " +
                        "Illegal dto type \"Book\", the base property \"store\" is defined differently " +
                        "by multiple super type so that it must be overridden",
                ex.getMessage()
        );
    }

    @Test
    public void testMustOverride2() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "abstract A { id as value }\n" +
                            "abstract B { name as value }\n" +
                            "BookView : A, B {\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 3 of \"src/main/dto/pkg/Book.dto\": " +
                        "Illegal dto type \"BookView\", " +
                        "the property alias \"value\" is defined differently " +
                        "by multiple super type so that it must be overridden",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalNegativeProp() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookView {\n" +
                            "#allScalars\n" +
                            "-tag\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 3 of \"src/main/dto/pkg/Book.dto\": " +
                        "There is no property alias \"tag\" that is need to be removed",
                ex.getMessage()
        );
    }

    private static void assertContentEquals(String expected, String actual) {
        Assertions.assertEquals(
                expected.replace("--->", "").replace("    ", ""),
                actual
        );
    }

    private static class BaseTypeImpl implements BaseType {

        private final String qualifiedName;

        final Map<String, BaseProp> propMap;

        BaseTypeImpl(String qualifiedName, BaseProp... baseProps) {
            this.qualifiedName = qualifiedName;
            this.propMap = Arrays.stream(baseProps).collect(
                    Collectors.toMap(
                            BaseProp::getName,
                            Function.identity(),
                            (a, b) -> a,
                            LinkedHashMap::new
                    )
            );
        }

        @NotNull
        @Override
        public String getPackageName() {
            int index = qualifiedName.lastIndexOf('.');
            return qualifiedName.substring(0, index);
        }

        @Override
        public String getName() {
            int index = qualifiedName.lastIndexOf('.');
            return qualifiedName.substring(index + 1);
        }

        @Override
        public String getQualifiedName() {
            return qualifiedName;
        }

        @Override
        public boolean isEntity() {
            return true;
        }
    }

    private static class BasePropImpl implements BaseProp {

        private final String name;

        private final Supplier<BaseType> targetTypeSupplier;

        private final boolean isNullable;

        private final boolean isList;

        BasePropImpl(String name) {
            this(name, null, false, false);
        }

        BasePropImpl(String name, Supplier<BaseType> targetTypeSupplier, boolean isNullable, boolean isList) {
            this.name = name;
            this.targetTypeSupplier = targetTypeSupplier;
            this.isNullable = isNullable;
            this.isList = isList;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isNullable() {
            return isNullable;
        }

        public BaseType getTargetType() {
            if (targetTypeSupplier == null) {
                return null;
            }
            BaseType targetType = targetTypeSupplier.get();
            if (targetType == null) {
                throw new IllegalStateException("targetTypeSupplier returns nothing");
            }
            return targetType;
        }

        public boolean isList() {
            return isList;
        }

        @Override
        public boolean isTransient() {
            return false;
        }

        @Override
        public boolean isFormula() {
            return false;
        }

        @Nullable
        @Override
        public BaseProp getIdViewBaseProp() {
            return null;
        }

        @Nullable
        @Override
        public BaseProp getManyToManyViewBaseProp() {
            return null;
        }

        @Override
        public boolean isAssociation(boolean entityLevel) {
            return getTargetType() != null;
        }

        @Override
        public boolean hasTransientResolver() {
            return false;
        }

        @Override
        public boolean isId() {
            return name.equals("id");
        }

        @Override
        public boolean isKey() {
            return true;
        }

        @Override
        public boolean isRecursive() {
            return name.equals("parent") || name.equals("childNodes");
        }

        @Override
        public String toString() {
            return "entity::" + name;
        }
    }

    private static class MyDtoCompiler extends DtoCompiler<BaseType, BaseProp> {

        private static final Map<String, BaseType> TYPE_MAP = new HashMap<>();

        private static final BaseTypeImpl BOOK_TYPE = new BaseTypeImpl(
                "org.babyfish.jimmer.sql.model.Book",
                new BasePropImpl("id"),
                new BasePropImpl("name"),
                new BasePropImpl("edition"),
                new BasePropImpl("price"),
                new BasePropImpl("tenant"),
                new BasePropImpl("store", () -> TYPE_MAP.get("BookStore"), true, false),
                new BasePropImpl("authors", () -> TYPE_MAP.get("Author"), false, true),
                new BasePropImpl("chapters", () -> TYPE_MAP.get("Chapter"), false, true),
                new BasePropImpl("creator", () -> TYPE_MAP.get("User"), false, false),
                new BasePropImpl("editor", () -> TYPE_MAP.get("User"), false, false)
        );

        private static final BaseTypeImpl BOOK_STORE_TYPE = new BaseTypeImpl(
                "org.babyfish.jimmer.sql.model.BookStore",
                new BasePropImpl("id"),
                new BasePropImpl("name"),
                new BasePropImpl("website"),
                new BasePropImpl("books", () -> TYPE_MAP.get("Book"), false, true)
        );

        private static final BaseTypeImpl AUTHOR_TYPE = new BaseTypeImpl(
                "org.babyfish.jimmer.sql.model.Author",
                new BasePropImpl("id"),
                new BasePropImpl("firstName"),
                new BasePropImpl("lastName"),
                new BasePropImpl("gender"),
                new BasePropImpl("books", () -> TYPE_MAP.get("Author"), false, true)
        );

        private static final BaseTypeImpl CHAPTER_TYPE = new BaseTypeImpl(
                "org.babyfish.jimmer.sql.model.Chapter",
                new BasePropImpl("id"),
                new BasePropImpl("index"),
                new BasePropImpl("title"),
                new BasePropImpl("book", () -> TYPE_MAP.get("Author"), false, false)
        );

        private static final BaseTypeImpl TREE_NODE_TYPE = new BaseTypeImpl(
                "org.babyfish.jimmer.sql.model.TreeNode",
                new BasePropImpl("id"),
                new BasePropImpl("name"),
                new BasePropImpl("childNodes", () -> TYPE_MAP.get("TreeNode"), false, true),
                new BasePropImpl("parent", () -> TYPE_MAP.get("TreeNode"), true, false)
        );

        private static final BaseTypeImpl USER_TYPE = new BaseTypeImpl(
                "org.babyfish.jimmer.sql.model.User",
                new BasePropImpl("id"),
                new BasePropImpl("name"),
                new BasePropImpl("gender")
        );

        private MyDtoCompiler(BaseType baseType, String dtoFilePath) {
            super(baseType, dtoFilePath);
        }
        
        static MyDtoCompiler book() {
            return new MyDtoCompiler(BOOK_TYPE, "src/main/dto/pkg/Book.dto");
        }
        
        static MyDtoCompiler treeNode() {
            return new MyDtoCompiler(TREE_NODE_TYPE, "src/main/dto/pkg/TreeNode.dto");
        }

        @Override
        protected Collection<BaseType> getSuperTypes(BaseType baseType) {
            return Collections.emptyList();
        }

        @Override
        protected Map<String, BaseProp> getDeclaredProps(BaseType baseType) {
            return ((BaseTypeImpl) baseType).propMap;
        }

        @Override
        protected Map<String, BaseProp> getProps(BaseType baseType) {
            return ((BaseTypeImpl) baseType).propMap;
        }

        @Override
        protected BaseType getTargetType(BaseProp baseProp) {
            return ((BasePropImpl) baseProp).getTargetType();
        }

        @Override
        protected boolean isGeneratedValue(BaseProp baseProp) {
            return true;
        }

        @Override
        protected List<String> getEnumConstants(BaseProp baseProp) {
            return null;
        }

        static {
            TYPE_MAP.put("Book", BOOK_TYPE);
            TYPE_MAP.put("BookStore", BOOK_STORE_TYPE);
            TYPE_MAP.put("Author", AUTHOR_TYPE);
            TYPE_MAP.put("Chapter", CHAPTER_TYPE);
            TYPE_MAP.put("TreeNode", TREE_NODE_TYPE);
            TYPE_MAP.put("User", USER_TYPE);
        }
    }
}
