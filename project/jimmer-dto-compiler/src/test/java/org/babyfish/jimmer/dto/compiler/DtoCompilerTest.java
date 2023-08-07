package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
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
                        "--->--->@optional id, " +
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
                        "--->flat(store): {" +
                        "--->--->@optional id as parentId, " +
                        "--->--->name as parentName, " +
                        "--->--->website as parentWebsite" +
                        "--->}" +
                        "]",
                dtoTypes.get(0).getHiddenFlatProps().toString()
        );
    }

    @Test
    public void testFlat2() {
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
                        "--->--->@optional id, " +
                        "--->--->name, " +
                        "--->--->@optional store.id as parentId, " +
                        "--->--->@optional store.name as parentName" +
                        "--->}" +
                        "]",
                dtoTypes.toString()
        );
        assertContentEquals(
                "[" +
                        "--->flat(store): {" +
                        "--->--->@optional id as parentId, " +
                        "--->--->name as parentName" +
                        "--->}" +
                        "]",
                dtoTypes.get(0).getHiddenFlatProps().toString()
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
                "Error at line 2: There is no property \"city\" in \"org.babyfish.jimmer.sql.model.Book\" or its super types",
                ex.getMessage()
        );
    }

    @Test
    public void testIllegalInputProperty() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "input BookInput {\n" +
                            "    authorIds\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 2: The property \"authorIds\" cannot be declared in input dto because it is view",
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
                "Error at line 2: token recognition error at: '<'",
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
                "Error at line 3: Duplicated property alias \"id\"",
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
                "Error at line 5: Duplicated property alias \"name\"",
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
                "Error at line 5: Base property \"entity::authors\" cannot be referenced twice",
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
                "Error at line 2: Illegal property \"store\", the child body is required",
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
                "Error at line 2: Illegal property \"store\", child body cannot be specified by it is id view property",
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
                "Error at line 2: Cannot call the function \"id\" because the current " +
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
                "Error at line 2: Cannot call the function \"flat\" " +
                        "because the current prop \"entity::authors\" is list",
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
                "Error at line 4: Illegal symbol \"*\", the property \"store\" is not recursive",
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
                "Error at line 5: Illegal symbol \"*\", the property \"store\" is not recursive",
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
                "Error at line 3: The alias cannot be specified when the function `flat` is used",
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
                "Error at line 3: Illegal dto type \"Book\", the base property \"store\" is defined differently " +
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
                "Error at line 3: Illegal dto type \"BookView\", " +
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
                "Error at line 3: There is no property alias \"tag\" that is need to be removed",
                ex.getMessage()
        );
    }

    private static void assertContentEquals(String expected, String actual) {
        Assertions.assertEquals(
                expected.replace("--->", ""),
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

        private final boolean isView;

        BasePropImpl(String name) {
            this(name, null, false, false);
        }

        BasePropImpl(String name, Supplier<BaseType> targetTypeSupplier, boolean isNullable, boolean isList) {
            this(name, targetTypeSupplier, isNullable, isList, false);
        }

        BasePropImpl(String name, Supplier<BaseType> targetTypeSupplier, boolean isNullable, boolean isList, boolean isView) {
            this.name = name;
            this.targetTypeSupplier = targetTypeSupplier;
            this.isNullable = isNullable;
            this.isList = isList;
            this.isView = isView;
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

        @Override
        public boolean isView() {
            return isView;
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
                new BasePropImpl("authorIds", null, false, true, true),
                new BasePropImpl("chapters", () -> TYPE_MAP.get("Chapter"), false, true)
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

        private MyDtoCompiler(BaseType baseType) {
            super(baseType);
        }
        
        static MyDtoCompiler book() {
            return new MyDtoCompiler(BOOK_TYPE);
        }
        
        static MyDtoCompiler treeNode() {
            return new MyDtoCompiler(TREE_NODE_TYPE);
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

        static {
            TYPE_MAP.put("Book", BOOK_TYPE);
            TYPE_MAP.put("BookStore", BOOK_STORE_TYPE);
            TYPE_MAP.put("Author", AUTHOR_TYPE);
            TYPE_MAP.put("Chapter", CHAPTER_TYPE);
            TYPE_MAP.put("TreeNode", TREE_NODE_TYPE);
        }
    }
}
