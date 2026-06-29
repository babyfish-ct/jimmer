package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DtoCompilerTest {

    @Test
    public void testSimpleByAlias() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "import org.babyfish.jimmer.sql.model.{Book as B}" +
                        "input BookInput {\n" +
                        "    #allScalars\n" +
                        "    -tenant\n" +
                        "    id(store)\n" +
                        "    id(authors) as authorIds\n" +
                        "}\n" +
                        "input CompositeBookInput {\n" +
                        "    #allScalars(B)\n" +
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
                        "--->static input BookInput {" +
                        "--->--->@static @optional id, " +
                        "--->--->name, " +
                        "--->--->edition, " +
                        "--->--->price, " +
                        "--->--->@static id(store) as storeId, " +
                        "--->--->id(authors) as authorIds" +
                        "--->}, " +
                        "--->static input CompositeBookInput {" +
                        "--->--->@static @optional id, " +
                        "--->--->name, " +
                        "--->--->edition, " +
                        "--->--->price, " +
                        "--->--->@static id(store) as storeId, " +
                        "--->--->id(authors) as authorIds, " +
                        "--->--->chapters: static input {" +
                        "--->--->--->@static @optional id, " +
                        "--->--->--->index, " +
                        "--->--->--->title, " +
                        "--->--->--->uncivilized" +
                        "--->--->}" +
                        "--->}" +
                        "]").replace("--->", ""),
                dtoTypes.toString()
        );
    }

    @Test
    public void testSimpleByThis() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "input BookInput {\n" +
                        "    #allScalars\n" +
                        "    -tenant\n" +
                        "    id(store)\n" +
                        "    id(authors) as authorIds\n" +
                        "}\n" +
                        "input CompositeBookInput {\n" +
                        "    #allScalars(this)\n" +
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
                        "--->static input BookInput {" +
                        "--->--->@static @optional id, " +
                        "--->--->name, " +
                        "--->--->edition, " +
                        "--->--->price, " +
                        "--->--->@static id(store) as storeId, " +
                        "--->--->id(authors) as authorIds" +
                        "--->}, " +
                        "--->static input CompositeBookInput {" +
                        "--->--->@static @optional id, " +
                        "--->--->name, " +
                        "--->--->edition, " +
                        "--->--->price, " +
                        "--->--->@static id(store) as storeId, " +
                        "--->--->id(authors) as authorIds, " +
                        "--->--->chapters: static input {" +
                        "--->--->--->@static @optional id, " +
                        "--->--->--->index, " +
                        "--->--->--->title, " +
                        "--->--->--->uncivilized" +
                        "--->--->}" +
                        "--->}" +
                        "]").replace("--->", ""),
                dtoTypes.toString()
        );
    }

    @Test
    public void testOptionalAllScalars() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "input BookInput {\n" +
                        "    #allScalars?" +
                        "}\n"
        );
        assertContentEquals(
                "static input BookInput {" +
                        "--->@static @optional id, " +
                        "--->@static @optional name, " +
                        "--->@static @optional edition, " +
                        "--->@static @optional price, " +
                        "--->@static @optional tenant" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testRequiredAllScalars() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "input BookInput {\n" +
                        "    #allScalars!" +
                        "}\n"
        );
        assertContentEquals(
                "static input BookInput {" +
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
    public void testOptionalAllReferences() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "input BookInput {\n" +
                        "    #allReferences?" +
                        "}\n"
        );
        assertContentEquals(
                "static input BookInput {" +
                        "--->@static @optional id(store) as storeId, " +
                        "--->@static @optional id(creator) as creatorId, " +
                        "--->@static @optional id(editor) as editorId" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testRequiredAllReferences() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "input BookInput {\n" +
                        "    #allReferences!\n" +
                        "}\n"
        );
        assertContentEquals(
                "static input BookInput {" +
                        "--->@required id(store) as storeId, " +
                        "--->@required id(creator) as creatorId, " +
                        "--->@required id(editor) as editorId" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testAllReferencesWithNegative() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "input BookInput {\n" +
                        "    #allReferences!\n" +
                        "    -storeId\n" +
                        "}\n"
        );
        assertContentEquals(
                "static input BookInput {" +
                        "--->@required id(creator) as creatorId, " +
                        "--->@required id(editor) as editorId" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testAllReferencesWithOverride() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "input BookInput {\n" +
                        "    #allReferences!\n" +
                        "    id(store) as parentId\n" +
                        "}\n"
        );
        assertContentEquals(
                "static input BookInput {" +
                        "--->@required id(creator) as creatorId, " +
                        "--->@required id(editor) as editorId, " +
                        "--->@static id(store) as parentId" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testMixedMacro() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "input BookInput {\n" +
                        "    #allScalars(this)" +
                        "    #allReferences(this)" +
                        "}\n"
        );
        assertContentEquals(
                "static input BookInput {" +
                        "--->@static @optional id, " +
                        "--->name, " +
                        "--->edition, " +
                        "--->price, " +
                        "--->tenant, " +
                        "--->@static id(store) as storeId, " +
                        "--->id(creator) as creatorId, " +
                        "--->id(editor) as editorId" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testInputRequired() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "input BookInput {\n" +
                        "    id!\n" +
                        "    id(store)\n" +
                        "}\n"
        );
        assertContentEquals(
                "static input BookInput {" +
                        "--->@required id, " +
                        "--->@static id(store) as storeId" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testSpecificationRequired() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "specification BookInput {\n" +
                        "    id!\n" +
                        "    associatedIdEq(store)!\n" +
                        "}\n"
        );
        assertContentEquals(
                "specification BookInput {@required id, @required id(store) as storeId}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testRecursive() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.treeNode(
                "input TreeNodeInput {" +
                        "    name" +
                        "    childNodes*" +
                        "}"
        );
        assertContentEquals(
                "[" +
                        "--->static input TreeNodeInput {" +
                        "--->--->name, " +
                        "--->--->@static @optional childNodes: ..." +
                        "--->}" +
                        "]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testRecursive2() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.treeNode(
                "input TreeNodeInput {\n" +
                        "    name\n" +
                        "    childNodes {\n" +
                        "        name\n" +
                        "        childNodes*\n" +
                        "    }\n" +
                        "}"
        );
        assertContentEquals(
                "[" +
                        "static input TreeNodeInput {" +
                        "--->name, " +
                        "--->childNodes: static input {" +
                        "--->--->name, " +
                        "--->--->@static @optional childNodes: ..." +
                        "--->}" +
                        "}" +
                        "]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testMultipleRecursions() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.treeNode(
                "input TreeNodeInput {" +
                        "    name" +
                        "    parent*" +
                        "    childNodes*" +
                        "}"
        );
        assertContentEquals(
                "[" +
                        "static input TreeNodeInput {" +
                        "--->name, " +
                        "--->@static @optional parent: static input {" +
                        "--->--->name, " +
                        "--->--->@static @optional parent: ..." +
                        "--->}..., " +
                        "--->@static @optional childNodes: static input {" +
                        "--->--->name, " +
                        "--->--->@static @optional childNodes: ..." +
                        "--->}..." +
                        "}" +
                        "]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testFlat1() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
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
    public void testFold() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "BookView {\n" +
                        "    id\n" +
                        "    fold(summary) {\n" +
                        "        name\n" +
                        "        edition\n" +
                        "    }\n" +
                        "    fold(details)? {\n" +
                        "        price\n" +
                        "        fold(audit) {\n" +
                        "            tenant\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        );
        assertContentEquals(
                "BookView {" +
                        "--->id, " +
                        "--->fold(summary): {" +
                        "--->--->name, " +
                        "--->--->edition" +
                        "--->}, " +
                        "--->@optional fold(details): {" +
                        "--->--->price, " +
                        "--->--->fold(audit): {" +
                        "--->--->--->tenant" +
                        "--->--->}" +
                        "--->}" +
                        "}",
                dtoTypes.get(0).toString()
        );
        Assertions.assertEquals(2, dtoTypes.get(0).getFoldProps().size());
        Assertions.assertFalse(dtoTypes.get(0).getFoldProps().get(0).isNullable());
        Assertions.assertTrue(dtoTypes.get(0).getFoldProps().get(1).isNullable());
        Assertions.assertEquals(
                "audit",
                dtoTypes.get(0)
                        .getFoldProps()
                        .get(1)
                        .getTargetType()
                        .getFoldProps()
                        .get(0)
                        .getName()
        );
    }

    @Test
    public void testFlatInsideFold() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "BookView {\n" +
                        "    fold(storeInfo) {\n" +
                        "        flat(store) {\n" +
                        "            name as storeName\n" +
                        "            website\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        );
        assertContentEquals(
                "BookView {" +
                        "--->fold(storeInfo): {" +
                        "--->--->@optional store.name as storeName, " +
                        "--->--->@optional store.website" +
                        "--->}" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testFoldInsideFlat() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "BookView {\n" +
                        "    flat(store) {\n" +
                        "        fold(key) {\n" +
                        "            name\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        );
        assertContentEquals(
                "BookView {" +
                        "--->@optional fold(storeKey): {" +
                        "--->--->@optional store.name" +
                        "--->}" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testFlat2() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.treeNode(
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
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
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
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
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
    public void testFlat5() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.treeNode(
                "FlatTree {\n" +
                        "    id\n" +
                        "    name\n" +
                        "    flat(parent) {\n" +
                        "        as(^ -> parent) {\n" +
                        "            #allScalars\n" +
                        "            #allReferences\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        );
        assertContentEquals(
                "[FlatTree {" +
                        "--->id, " +
                        "--->name, " +
                        "--->@optional parent.id as parentId, " +
                        "--->@optional parent.name as parentName, " +
                        "--->@optional id(parent.parent) as parentParentId" +
                        "}]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testFlat6() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "specification BookSpecification {\n" +
                        "    id\n" +
                        "    name\n" +
                        "    flat(authors) {\n" +
                        "        valueIn(id) as authorIds\n" +
                        "    }\n" +
                        "}"
        );
        assertContentEquals(
                "[specification BookSpecification {" +
                        "--->@optional id, " +
                        "--->@optional name, " +
                        "--->@optional valueIn(authors.id) as authorIds" +
                        "}]",
                dtoTypes.toString()
        );
        DtoProp<?, ?> prop = dtoTypes.get(0).getDtoProps().get(2);
        Assertions.assertEquals("authorIds", prop.getName());
        Assertions.assertEquals("valueIn", prop.getFuncName());
    }

    @Test
    public void testFlat7() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.treeNode(
                "TreeFlatView {\n" +
                        "    #allScalars(this)\n" +
                        "    flat(parent) {\n" +
                        "        as(^ -> parent) {\n" +
                        "            #allScalars(this)\n" +
                        "            flat(parent) {\n" +
                        "                as(^ -> parent) {\n" +
                        "                    #allScalars(this)\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n"
        );
        assertContentEquals(
                "TreeFlatView {" +
                        "--->id, " +
                        "--->name, " +
                        "--->@optional parent.id as parentId, " +
                        "--->@optional parent.name as parentName, " +
                        "--->@optional parent.parent.id.id as parentParentId, " +
                        "--->@optional parent.parent.name.name as parentParentName" +
                        "}",
                dtoTypes.get(0).toString()
        );
    }

    @Test
    public void testUserProp() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "import com.company.pkg.data.User\n" +
                        "import com.company.pkg.data.Configuration as Cfg\n" +
                        "import com.company.pkg.data.{\n" +
                        "    ConfigurationKey as _K\n, " +
                        "    NestedElement as _E\n" +
                        "}\n" +
                        "import java.util.SequencedSet as _OSet\n" +
                        "import com.company.pkg.TopLevel as _T\n" +
                        "\n" +
                        "specification Customer {\n" +
                        "    a: Int\n" +
                        "    b: User?\n" +
                        "    c: MutableList<Cfg>\n" +
                        "    d: List<Map<_K, Array<_E?>>>?\n" +
                        "    e: _OSet<_T.Nested>\n" +
                        "}"
        );
        assertContentEquals(
                "specification Customer {" +
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
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
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
    public void testQbeSpecification() {
        DtoType<BaseType, BaseProp> dtoType = MyDtoCompiler.book(
                "specification BookSpecification {\n" +
                        "    gt(price)\n" +
                        "    lt(price)\n" +
                        "    flat(store) {" +
                        "        as(^ -> parent) {\n" +
                        "            ge(name)\n" +
                        "            le(name)\n" +
                        "        }\n" +
                        "    }\n" +
                        "    flat(authors) {\n" +
                        "        like/i(firstName, lastName) as authorName\n" +
                        "    }\n" +
                        "}"
        ).get(0);
        assertContentEquals(
                "specification BookSpecification {" +
                        "--->@optional gt(price) as minPriceExclusive, " +
                        "--->@optional lt(price) as maxPriceExclusive, " +
                        "--->@optional ge(store.name) as parentMinName, " +
                        "--->@optional le(store.name) as parentMaxName, " +
                        "--->@optional like(authors.(firstName|lastName)) as authorName" +
                        "}",
                dtoType.toString()
        );
    }

    @Test
    public void testDoc() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.treeNode(
                "/**\n" +
                        " * The recursive tree input\n" +
                        " */\n" +
                        "input TreeNodeInput {\n" +
                        "    /**\n" +
                        "     * The name of current tree node\n" +
                        "     */\n" +
                        "    name\n" +
                        "    /**\n" +
                        "     * The child nodes of current tree node\n" +
                        "     */\n" +
                        "    childNodes*\n" +
                        "}\n"
        );
        assertContentEquals(
                "[" +
                        "--->@doc(The recursive tree input) " +
                        "--->static input TreeNodeInput {" +
                        "--->--->" +
                        "--->--->@doc(The name of current tree node) " +
                        "--->--->name, " +
                        "--->--->@doc(The child nodes of current tree node) " +
                        "--->--->@static @optional childNodes: ..." +
                        "--->}" +
                        "]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testInterfaceImplementation() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "import com.company.project.model.common.Named\n" +
                        "input BookInput implements Named {\n" +
                        "    id\n" +
                        "    name\n" +
                        "    authors implements Named, java.lang.Comparable<Author> {\n" +
                        "        id\n" +
                        "        firstName\n" +
                        "        lastName\n" +
                        "    }\n" +
                        "}\n"
        );
        assertContentEquals(
                "[" +
                        "--->static input BookInput implements com.company.project.model.common.Named {" +
                        "--->--->@static @optional id, " +
                        "--->--->name, " +
                        "--->--->authors: static input implements " +
                        "--->--->--->com.company.project.model.common.Named, " +
                        "--->--->--->java.lang.Comparable<org.babyfish.jimmer.sql.model.Author> {" +
                        "--->--->--->--->@static @optional id, " +
                        "--->--->--->--->firstName, " +
                        "--->--->--->--->lastName" +
                        "--->--->--->}" +
                        "--->}" +
                        "]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testInputModifier() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "dynamic input BookInput {\n" +
                        "    fixed id?\n" +
                        "    static name?\n" +
                        "    dynamic edition?\n" +
                        "    fuzzy price?\n" +
                        "    fuzzy store {\n" +
                        "        fuzzy name?" +
                        "        website" +
                        "    }\n" +
                        "}\n"
        );
        assertContentEquals(
                "[dynamic input BookInput {" +
                        "--->@fixed @optional id, " +
                        "--->@static @optional name, " +
                        "--->@dynamic @optional edition, " +
                        "--->@fuzzy @optional price, " +
                        "--->@fuzzy store: dynamic input {" +
                        "--->--->@fuzzy @optional name, " +
                        "--->--->@dynamic website" +
                        "--->}" +
                        "}]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testIssue705() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "specification BookSpecification {\n" +
                        "    flat(store) {\n" +
                        "        eq(id)! as storeId" +
                        "    }\n" +
                        "}\n"
        );
        assertContentEquals(
                "[specification BookSpecification {" +
                        "--->@required eq(store.id) as storeId}" +
                        "]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testConfig() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "BookView {\n" +
                        "    #allScalars\n" +
                        "    -tenant\n" +
                        "    !fetchType(JOIN_ALWAYS)\n" +
                        "    store {\n" +
                        "        #allScalars\n" +
                        "    }\n" +
                        "    !limit(2, 2)\n" +
                        "    !batch(10)\n" +
                        "    !orderBy(firstName asc, lastName asc)\n" +
                        "    authors {\n" +
                        "        firstName\n" +
                        "        lastName\n" +
                        "    }\n" +
                        "}\n"
        );
        assertContentEquals(
                "[BookView {" +
                        "--->id, " +
                        "--->name, " +
                        "--->edition, " +
                        "--->price, " +
                        "--->!fetchType(JOIN_ALWAYS) " +
                        "--->store: {" +
                        "--->--->id, " +
                        "--->--->name, " +
                        "--->--->website" +
                        "--->}, " +
                        "--->!orderBy(firstName asc, lastName asc) " +
                        "--->!limit(2, 2) " +
                        "--->!batch(10) " +
                        "--->authors: {" +
                        "--->--->firstName, " +
                        "--->--->lastName" +
                        "--->}" +
                        "}]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testZeroOffsetConfig() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "BookView {\n" +
                        "    #allScalars\n" +
                        "    -tenant\n" +
                        "    !fetchType(JOIN_ALWAYS)\n" +
                        "    store {\n" +
                        "        #allScalars\n" +
                        "    }\n" +
                        "    !limit(2, 0)\n" +
                        "    !batch(10)\n" +
                        "    !orderBy(firstName asc, lastName asc)\n" +
                        "    authors {\n" +
                        "        firstName\n" +
                        "        lastName\n" +
                        "    }\n" +
                        "}\n"
        );
        assertContentEquals(
                "[BookView {" +
                        "--->id, " +
                        "--->name, " +
                        "--->edition, " +
                        "--->price, " +
                        "--->!fetchType(JOIN_ALWAYS) " +
                        "--->store: {" +
                        "--->--->id, " +
                        "--->--->name, " +
                        "--->--->website" +
                        "--->}, " +
                        "--->!orderBy(firstName asc, lastName asc) " +
                        "--->!limit(2) " +
                        "--->!batch(10) " +
                        "--->authors: {" +
                        "--->--->firstName, " +
                        "--->--->lastName" +
                        "--->}" +
                        "}]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testRecursiveConfig() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.treeNode(
                "TreeNode {\n" +
                        "    name\n" +
                        "    !fetchType(JOIN_IF_NO_CACHE)\n" +
                        "    parent*\n" +
                        "    !batch(4)\n" +
                        "    !where(name ilike 'X%' or name != 'YYY')\n" +
                        "    !orderBy(name desc)\n" +
                        "    childNodes*\n" +
                        "}\n"
        );
        assertContentEquals(
                "[TreeNode {" +
                        "--->name, " +
                        "--->@optional " +
                        "--->!fetchType(JOIN_IF_NO_CACHE) " +
                        "--->parent: {" +
                        "--->--->name, " +
                        "--->--->@optional " +
                        "--->--->!fetchType(JOIN_IF_NO_CACHE) " +
                        "--->--->parent: ..." +
                        "--->}..., " +
                        "--->@optional " +
                        "--->!where((name ilike \"X%\" or name <> \"YYY\")) " +
                        "--->!orderBy(name desc) " +
                        "--->!batch(4) " +
                        "--->childNodes: {" +
                        "--->--->name, " +
                        "--->--->@optional " +
                        "--->--->!where((name ilike \"X%\" or name <> \"YYY\")) " +
                        "--->--->!orderBy(name desc) " +
                        "--->--->!batch(4) " +
                        "--->--->childNodes: ..." +
                        "--->}..." +
                        "}]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testWhereBook() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.book(
                "BookView {\n" +
                        "    #allScalars\n" +
                        "    !where(uncivilized = true)\n" +
                        "    chapters {\n" +
                        "        #allScalars - uncivilized\n" +
                        "    }\n" +
                        "}"
        );
        assertContentEquals(
                "[BookView {" +
                        "--->id, " +
                        "--->name, " +
                        "--->edition, " +
                        "--->price, " +
                        "--->tenant, " +
                        "--->!where(uncivilized = true) " +
                        "--->chapters: {" +
                        "--->--->id, " +
                        "--->--->index, " +
                        "--->--->title" +
                        "--->}" +
                        "}]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testIssue1036() {
        List<DtoType<BaseType, BaseProp>> dtoTypes = MyDtoCompiler.treeNode(
                "TreeNodeView {\n" +
                        "    #allScalars\n" +
                        "    !orderBy(name asc)\n" +
                        "    !recursion(TreeNodeRecursiveStrategy)\n" +
                        "    childNodes*" +
                        "}"
        );
        assertContentEquals(
                "[TreeNodeView {" +
                        "--->id, " +
                        "--->name, " +
                        "--->@optional " +
                        "--->!orderBy(name asc) " +
                        "--->!recursion(org.babyfish.jimmer.sql.model.TreeNodeRecursiveStrategy) " +
                        "--->childNodes: ..." +
                        "}]",
                dtoTypes.toString()
        );
    }

    @Test
    public void testIllegalPropertyName() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "BookView {\n" +
                            "    city\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:2 : There is no property \"city\" in " +
                        "\"org.babyfish.jimmer.sql.model.Book\" or its super types\n" +
                        "    city\n" +
                        "    ^",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalSyntax() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "BookView {\n" +
                            "    #<allScalars>\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:2 : extraneous input '<' expecting {'allScalars', 'allReferences'}\n" +
                        "    #<allScalars>\n" +
                        "     ^",
                ex.getMessage()
        );
    }

    @Test
    public void testPolymorphicDtoWithImplicitDefault() {
        DtoType<BaseType, BaseProp> dtoType = MyDtoCompiler.client(
                "ClientView {\n" +
                        "    id\n" +
                        "    name\n" +
                        "    #subtypes {\n" +
                        "        Organization class Org implements marker.Marker {\n" +
                        "            taxCode\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        ).get(0);
        DtoPolymorphism<BaseType, BaseProp> polymorphism = dtoType.getPolymorphism();
        Assertions.assertNotNull(polymorphism);
        Assertions.assertFalse(polymorphism.isExhaustive());
        Assertions.assertNotNull(polymorphism.getDefaultBranch());
        Assertions.assertTrue(polymorphism.getDefaultBranch().isImplicit());
        Assertions.assertEquals("Default", polymorphism.getDefaultBranch().getClassName());
        Assertions.assertEquals(1, polymorphism.getSubtypeBranches().size());
        DtoPolymorphicBranch<BaseType, BaseProp> branch = polymorphism.getSubtypeBranches().get(0);
        Assertions.assertEquals("org.babyfish.jimmer.sql.model.Organization", branch.getTargetType().getQualifiedName());
        Assertions.assertEquals("Org", branch.getClassName());
        Assertions.assertEquals(
                Collections.singletonList("taxCode"),
                branch.getDtoType().getProps().stream().map(AbstractProp::getAlias).collect(Collectors.toList())
        );
        Assertions.assertEquals(
                Collections.singletonList("marker.Marker"),
                branch.getDtoType().getSuperInterfaces().stream().map(TypeRef::toString).collect(Collectors.toList())
        );
    }

    @Test
    public void testPolymorphicDtoWithExplicitDefault() {
        DtoType<BaseType, BaseProp> dtoType = MyDtoCompiler.client(
                "ClientView {\n" +
                        "    name\n" +
                        "    #subtypes {\n" +
                        "        #default class Other implements marker.UnlistedClientView {\n" +
                        "        }\n" +
                        "        Person {\n" +
                        "            firstName\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        ).get(0);
        DtoPolymorphicBranch<BaseType, BaseProp> defaultBranch = dtoType.getPolymorphism().getDefaultBranch();
        Assertions.assertFalse(defaultBranch.isImplicit());
        Assertions.assertEquals("Other", defaultBranch.getClassName());
        Assertions.assertEquals(
                Collections.singletonList("marker.UnlistedClientView"),
                defaultBranch.getDtoType().getSuperInterfaces().stream().map(TypeRef::toString).collect(Collectors.toList())
        );
        Assertions.assertTrue(defaultBranch.getDtoType().getProps().isEmpty());
    }

    @Test
    public void testExhaustivePolymorphicDto() {
        DtoType<BaseType, BaseProp> dtoType = MyDtoCompiler.client(
                "ClientView {\n" +
                        "    name\n" +
                        "    #subtypes {\n" +
                        "        #exhaustive\n" +
                        "        Person {\n" +
                        "            firstName\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        ).get(0);
        DtoPolymorphism<BaseType, BaseProp> polymorphism = dtoType.getPolymorphism();
        Assertions.assertTrue(polymorphism.isExhaustive());
        Assertions.assertNull(polymorphism.getDefaultBranch());
        Assertions.assertEquals(
                new LinkedHashSet<>(Arrays.asList(
                        "org.babyfish.jimmer.sql.model.Organization",
                        "org.babyfish.jimmer.sql.model.Person"
                )),
                polymorphism.getSubtypeBranches()
                        .stream()
                        .map(it -> it.getTargetType().getQualifiedName())
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
        DtoPolymorphicBranch<BaseType, BaseProp> personBranch = polymorphism.getSubtypeBranches()
                .stream()
                .filter(it -> it.getTargetType().getName().equals("Person"))
                .findFirst()
                .orElseThrow(AssertionError::new);
        Assertions.assertFalse(personBranch.isImplicit());
        Assertions.assertEquals(
                Collections.singletonList("firstName"),
                personBranch.getDtoType().getProps().stream().map(AbstractProp::getAlias).collect(Collectors.toList())
        );
        DtoPolymorphicBranch<BaseType, BaseProp> organizationBranch = polymorphism.getSubtypeBranches()
                .stream()
                .filter(it -> it.getTargetType().getName().equals("Organization"))
                .findFirst()
                .orElseThrow(AssertionError::new);
        Assertions.assertTrue(organizationBranch.isImplicit());
        Assertions.assertEquals("Organization", organizationBranch.getClassName());
        Assertions.assertTrue(organizationBranch.getDtoType().getProps().isEmpty());
    }

    @Test
    public void testExhaustivePolymorphicDtoWithInstantiableRoot() {
        DtoType<BaseType, BaseProp> dtoType = MyDtoCompiler.payment(
                "PaymentView {\n" +
                        "    amount\n" +
                        "    #subtypes {\n" +
                        "        #exhaustive\n" +
                        "        Payment class Root {\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        ).get(0);
        DtoPolymorphism<BaseType, BaseProp> polymorphism = dtoType.getPolymorphism();
        Assertions.assertTrue(polymorphism.isExhaustive());
        Assertions.assertNull(polymorphism.getDefaultBranch());
        Assertions.assertEquals(
                new LinkedHashSet<>(Arrays.asList("Root", "WirePayment")),
                polymorphism.getSubtypeBranches()
                        .stream()
                        .map(DtoPolymorphicBranch::getClassName)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }

    @Test
    public void testSubtypesBlockCannotBeUsedBySpecification() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.client(
                    "specification ClientSpec {\n" +
                            "    name\n" +
                            "    #subtypes {\n" +
                            "        Person {\n" +
                            "            firstName\n" +
                            "        }\n" +
                            "    }\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Client.dto:3 : Polymorphic specification DTOs are not supported by #subtypes\n" +
                        "    #subtypes {\n" +
                        "    ^",
                ex.getMessage()
        );
    }

    @Test
    public void testDefaultBranchCannotBeUsedWithExhaustive() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.client(
                    "ClientView {\n" +
                            "    name\n" +
                            "    #subtypes {\n" +
                            "        #exhaustive\n" +
                            "        #default {\n" +
                            "        }\n" +
                            "        Person {\n" +
                            "            firstName\n" +
                            "        }\n" +
                            "    }\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Client.dto:5 : #default branch cannot be used together with #exhaustive\n" +
                        "        #default {\n" +
                        "        ^",
                ex.getMessage()
        );
    }

    @Test
    public void testDefaultBranchBodyMustBeEmpty() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.client(
                    "ClientView {\n" +
                            "    name\n" +
                            "    #subtypes {\n" +
                            "        #default {\n" +
                            "            id\n" +
                            "        }\n" +
                            "        Person {\n" +
                            "            firstName\n" +
                            "        }\n" +
                            "    }\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Client.dto:4 : The body of #default branch must be empty\n" +
                        "        #default {\n" +
                        "                 ^",
                ex.getMessage()
        );
    }

    @Test
    public void testAbstractRootCannotBeSubtypeBranch() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.client(
                    "ClientView {\n" +
                            "    name\n" +
                            "    #subtypes {\n" +
                            "        Client {\n" +
                            "        }\n" +
                            "    }\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Client.dto:4 : Illegal subtype branch \"org.babyfish.jimmer.sql.model.Client\", it is not instantiable\n" +
                        "        Client {\n" +
                        "        ^",
                ex.getMessage()
        );
    }

    @Test
    public void testDuplicateSubtypeBranchClassName() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.client(
                    "ClientView {\n" +
                            "    name\n" +
                            "    #subtypes {\n" +
                            "        Organization class Person {\n" +
                            "            taxCode\n" +
                            "        }\n" +
                            "        Person {\n" +
                            "            firstName\n" +
                            "        }\n" +
                            "    }\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Client.dto:7 : Duplicated polymorphic DTO branch class name \"Person\"\n" +
                        "        Person {\n" +
                        "        ^",
                ex.getMessage()
        );
    }

    @Test
    public void testDuplicateAlias() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "BookView {\n" +
                            "    id\n" +
                            "    name as id\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:3 : Duplicated property alias \"id\"\n" +
                        "    name as id\n" +
                        "            ^",
                ex.getMessage()
        );
    }

    @Test
    public void testDuplicateAlias2() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
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
                "/User/test/Book.dto:5 : Duplicated property alias \"name\"\n" +
                        "        name\n" +
                        "        ^",
                ex.getMessage()
        );
    }

    @Test
    public void testDuplicateAlias3() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "BookView {\n" +
                            "    id\n" +
                            "    name as myName\n" +
                            "    myName: String\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:4 : Duplicated property alias \"myName\"\n" +
                        "    myName: String\n" +
                        "    ^",
                ex.getMessage()
        );
    }

    @Test
    public void testReferenceTwice() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "BookView {\n" +
                            "    authors {\n" +
                            "        #allScalars\n" +
                            "    }\n" +
                            "    id(authors) as authorIds\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:5 : Base property \"entity::authors\" cannot be referenced too many times\n" +
                        "    id(authors) as authorIds\n" +
                        "       ^",
                ex.getMessage()
        );
    }

    @Test
    public void testNoBody() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "BookView {\n" +
                            "    flat(store)\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:2 : Illegal property \"store\", the child body is required\n" +
                        "    flat(store)\n" +
                        "              ^",
                ex.getMessage()
        );
    }

    @Test
    public void testUnnecessaryBody() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "BookView {\n" +
                            "    id(store) {\n" +
                            "        id\n" +
                            "}\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:2 : Illegal property \"store\", " +
                        "child body cannot be specified by it is id view property\n" +
                        "    id(store) {\n" +
                        "              ^",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalIdFunc() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "BookView {\n" +
                            "    id(name)\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:2 : Cannot call the function \"id\" " +
                        "because the current prop \"entity::name\" is not entity level association property\n" +
                        "    id(name)\n" +
                        "    ^",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalFlatFunc() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "BookView {\n" +
                            "    flat(authors)\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:2 : Cannot call the function \"flat\" because " +
                        "the current prop \"entity::authors\" is list and " +
                        "the current dto type is not specification\n" +
                        "    flat(authors)\n" +
                        "    ^",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalRequired() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "input BookSpecification {\n" +
                            "    id\n" +
                            "    id(store)!\n" +
                            "}\n"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:3 : Illegal required modifier '!' for non-id property, " +
                        "the declared type is neither unsafe nor specification\n" +
                        "    id(store)!\n" +
                        "             ^",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalRecursiveFunc() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "BookView {\n" +
                            "    store*\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:2 : Illegal symbol \"*\", the property \"store\" is not recursive\n" +
                        "    store*\n" +
                        "         ^",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalRecursiveFunc2() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "Book {\n" +
                            "    name\n" +
                            "    flat(store)*\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:3 : Illegal symbol \"*\", the property \"store\" is not recursive\n" +
                        "    flat(store)*\n" +
                        "               ^",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalAlias() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "Book {\n" +
                            "    name\n" +
                            "    flat(store) as parent {\n" +
                            "        name as parentName\n" +
                            "    }\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:3 : The alias cannot be specified when the function `flat` is used\n" +
                        "    flat(store) as parent {\n" +
                        "                   ^",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalNegativeProp() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "BookView {\n" +
                            "#allScalars\n" +
                            "-tag\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:3 : There is no property alias \"tag\" that is need to be removed\n" +
                        "-tag\n" +
                        " ^",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalInputModifier1() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "BookView {\n" +
                            "dynamic name\n" +
                            "}\n"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:2 : Illegal modifier \"dynamic\", the declaring dto type is not input\n" +
                        "dynamic name\n" +
                        "^",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalInputModifier2() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "input BookInput {\n" +
                            "dynamic name\n" +
                            "}\n"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:2 : Illegal modifier \"dynamic\", the current property \"name\" is not nullable\n" +
                        "dynamic name\n" +
                        "^",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalInputModifier3() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book(
                    "unsafe input BookInput {\n" +
                            "dynamic id(store)!\n" +
                            "}\n"
            );
        });
        Assertions.assertEquals(
                "/User/test/Book.dto:2 : Illegal modifier \"dynamic\", the current property \"storeId\" is not nullable\n" +
                        "dynamic id(store)!\n" +
                        "^",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalRecursiveChildFetcher() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.treeNode(
                    "TreeNodeView {\n" +
                            "name\n" +
                            "childNodes* {\n" +
                            "    name\n" +
                            "}" +
                            "}\n"
            );
        });
        Assertions.assertEquals(
                "/User/test/TreeNode.dto:3 : Illegal symbol \"*\", the child type of recursive property \"childNodes\" cannot not specified\n" +
                        "childNodes* {\n" +
                        "            ^",
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

        final List<BaseType> superTypes = new ArrayList<>();

        final List<BaseType> directSubTypes = new ArrayList<>();

        final boolean instantiable;

        BaseTypeImpl(String qualifiedName, BaseProp... baseProps) {
            this(qualifiedName, true, baseProps);
        }

        BaseTypeImpl(String qualifiedName, boolean instantiable, BaseProp... baseProps) {
            this.qualifiedName = qualifiedName;
            this.instantiable = instantiable;
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

        @Nullable
        public BaseProp getIdProp() {
            return propMap.get("id");
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
        public boolean isEmbedded() {
            return false;
        }

        @Override
        public boolean isReference() {
            return !isList;
        }

        @Override
        public boolean isLogicalDeleted() {
            return false;
        }

        @Override
        public boolean isExcludedFromAllScalars() {
            return false;
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
                new BasePropImpl("website", null, true, false),
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
                new BasePropImpl("uncivilized"),
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

        private static final BaseTypeImpl CLIENT_TYPE = new BaseTypeImpl(
                "org.babyfish.jimmer.sql.model.Client",
                false,
                new BasePropImpl("id"),
                new BasePropImpl("type"),
                new BasePropImpl("name")
        );

        private static final BaseTypeImpl ORGANIZATION_TYPE = new BaseTypeImpl(
                "org.babyfish.jimmer.sql.model.Organization",
                new BasePropImpl("taxCode")
        );

        private static final BaseTypeImpl PERSON_TYPE = new BaseTypeImpl(
                "org.babyfish.jimmer.sql.model.Person",
                new BasePropImpl("firstName"),
                new BasePropImpl("lastName")
        );

        private static final BaseTypeImpl PAYMENT_TYPE = new BaseTypeImpl(
                "org.babyfish.jimmer.sql.model.Payment",
                true,
                new BasePropImpl("id"),
                new BasePropImpl("type"),
                new BasePropImpl("amount")
        );

        private static final BaseTypeImpl WIRE_PAYMENT_TYPE = new BaseTypeImpl(
                "org.babyfish.jimmer.sql.model.WirePayment",
                new BasePropImpl("swiftCode")
        );

        private MyDtoCompiler(DtoFile dtoFile) throws IOException {
            super(dtoFile);
        }

        static List<DtoType<BaseType, BaseProp>> book(String code) {
            try {
                return new MyDtoCompiler(
                        new DtoFile(
                                new MockedOsFile(
                                        "/User/test/Book.dto",
                                        code
                                ),
                                "project",
                                "src/main/dto",
                                Arrays.asList("org", "babyfish", "jimmer", "sql", "model"),
                                "Book.dto"
                        )
                ).compile(BOOK_TYPE);
            } catch (IOException ex) {
                Assertions.fail(ex);
                return null;
            }
        }

        static List<DtoType<BaseType, BaseProp>> treeNode(String code) {
            try {
                return new MyDtoCompiler(
                        new DtoFile(
                                new MockedOsFile(
                                        "/User/test/TreeNode.dto",
                                        code
                                ),
                                "project",
                                "src/main/dto",
                                Arrays.asList("org", "babyfish", "jimmer", "sql", "model"),
                                "TreeNode.dto"
                        )
                ).compile(TREE_NODE_TYPE);
            } catch (IOException ex) {
                Assertions.fail(ex);
                return null;
            }
        }

        static List<DtoType<BaseType, BaseProp>> client(String code) {
            try {
                return new MyDtoCompiler(
                        new DtoFile(
                                new MockedOsFile(
                                        "/User/test/Client.dto",
                                        code
                                ),
                                "project",
                                "src/main/dto",
                                Arrays.asList("org", "babyfish", "jimmer", "sql", "model"),
                                "Client.dto"
                        )
                ).compile(CLIENT_TYPE);
            } catch (IOException ex) {
                Assertions.fail(ex);
                return null;
            }
        }

        static List<DtoType<BaseType, BaseProp>> payment(String code) {
            try {
                return new MyDtoCompiler(
                        new DtoFile(
                                new MockedOsFile(
                                        "/User/test/Payment.dto",
                                        code
                                ),
                                "project",
                                "src/main/dto",
                                Arrays.asList("org", "babyfish", "jimmer", "sql", "model"),
                                "Payment.dto"
                        )
                ).compile(PAYMENT_TYPE);
            } catch (IOException ex) {
                Assertions.fail(ex);
                return null;
            }
        }

        @Override
        protected Collection<BaseType> getSuperTypes(BaseType baseType) {
            return ((BaseTypeImpl) baseType).superTypes;
        }

        @Nullable
        @Override
        protected BaseType getType(String qualifiedName) {
            for (BaseType type : TYPE_MAP.values()) {
                if (type.getQualifiedName().equals(qualifiedName)) {
                    return type;
                }
            }
            return null;
        }

        @Override
        protected Collection<BaseType> getDirectSubTypes(BaseType baseType) {
            return ((BaseTypeImpl) baseType).directSubTypes;
        }

        @Override
        protected boolean isSameType(BaseType baseType1, BaseType baseType2) {
            return baseType1.getQualifiedName().equals(baseType2.getQualifiedName());
        }

        @Override
        protected boolean isInstantiable(BaseType baseType) {
            return ((BaseTypeImpl) baseType).instantiable;
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
        protected @Nullable BaseProp getIdProp(BaseType baseType) {
            return ((BaseTypeImpl) baseType).getIdProp();
        }

        @Override
        protected boolean isGeneratedValue(BaseProp baseProp) {
            return true;
        }

        @Override
        protected List<String> getEnumConstants(BaseProp baseProp) {
            return null;
        }

        @Override
        protected boolean isSameType(BaseProp baseProp1, BaseProp baseProp2) {
            return true;
        }

        @Override
        protected SimplePropType getSimplePropType(BaseProp baseProp) {
            if (baseProp.getName().equals("name") || baseProp.getName().endsWith("Name")) {
                return SimplePropType.STRING;
            }
            return SimplePropType.NONE;
        }

        @Override
        protected SimplePropType getSimplePropType(PropConfig.PathNode<BaseProp> pathNode) {
            BaseProp baseProp = pathNode.getProp();
            if (baseProp.getName().equals("name") || baseProp.getName().endsWith("Name")) {
                return SimplePropType.STRING;
            }
            if (baseProp.getName().equals("uncivilized")) {
                return SimplePropType.BOOLEAN;
            }
            return SimplePropType.NONE;
        }

        @Override
        protected Integer getGenericTypeCount(String qualifiedName) {
            switch (qualifiedName) {
                case "java.lang.Comparable":
                case "java.util.SequencedSet":
                    return 1;
                default:
                    return 0;
            }
        }

        static {
            TYPE_MAP.put("Book", BOOK_TYPE);
            TYPE_MAP.put("BookStore", BOOK_STORE_TYPE);
            TYPE_MAP.put("Author", AUTHOR_TYPE);
            TYPE_MAP.put("Chapter", CHAPTER_TYPE);
            TYPE_MAP.put("TreeNode", TREE_NODE_TYPE);
            TYPE_MAP.put("User", USER_TYPE);
            TYPE_MAP.put("Client", CLIENT_TYPE);
            TYPE_MAP.put("Organization", ORGANIZATION_TYPE);
            TYPE_MAP.put("Person", PERSON_TYPE);
            TYPE_MAP.put("Payment", PAYMENT_TYPE);
            TYPE_MAP.put("WirePayment", WIRE_PAYMENT_TYPE);

            inherit(ORGANIZATION_TYPE, CLIENT_TYPE);
            inherit(PERSON_TYPE, CLIENT_TYPE);
            inherit(WIRE_PAYMENT_TYPE, PAYMENT_TYPE);
        }

        private static void inherit(BaseTypeImpl subType, BaseTypeImpl superType) {
            subType.superTypes.add(superType);
            subType.propMap.putAll(superType.propMap);
            superType.directSubTypes.add(subType);
            superType.directSubTypes.sort(Comparator.comparing(BaseType::getQualifiedName));
        }
    }

    private static class MockedOsFile implements OsFile {

        private final String absolutePath;

        private final String content;

        private MockedOsFile(String absolutePath, String content) {
            this.absolutePath = absolutePath;
            this.content = content;
        }

        @Override
        public String getAbsolutePath() {
            return absolutePath;
        }

        @Override
        public Reader openReader() throws IOException {
            return new StringReader(content);
        }
    }
}
