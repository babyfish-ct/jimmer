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
    public void testIllegalCode() {
        DtoAstException ex = Assertions.assertThrows(DtoAstException.class, () -> {
            MyDtoCompiler.book().compile(
                    "BookInput {\n" +
                            "#<allScalars>\n" +
                            "}"
            );
        });
        Assertions.assertEquals(
                "Error at line 2: token recognition error at: '<'",
                ex.getMessage()
        );
    }

    @Test
    public void test() {
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
                        "}"
        );
        assertContentEquals(
                ("[" +
                        "--->input BookInput{" +
                        "--->--->@optional id, " +
                        "--->--->name, " +
                        "--->--->edition, " +
                        "--->--->price, " +
                        "--->--->id(store) as storeId, " +
                        "--->--->id(authors) as authorIds" +
                        "--->}, " +
                        "--->input CompositeBookInput{" +
                        "--->--->@optional id, " +
                        "--->--->name, " +
                        "--->--->edition, " +
                        "--->--->price, " +
                        "--->--->id(store) as storeId, " +
                        "--->--->id(authors) as authorIds, " +
                        "--->--->chapters: input{" +
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
                        "--->input TreeNodeInput{" +
                        "--->--->name, " +
                        "--->--->@optional childNodes: input{" +
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
                        "--->input BookInput{" +
                        "--->--->tenant, " +
                        "--->--->name, " +
                        "--->--->edition, " +
                        "--->--->id(store) as storeId, " +
                        "--->--->id(authors) as authorIds" +
                        "--->}, " +
                        "--->input CompositeInput{" +
                        "--->--->tenant, " +
                        "--->--->name, " +
                        "--->--->edition, " +
                        "--->--->id(store) as storeId, " +
                        "--->--->id(authors) as authorIdList, " +
                        "--->--->chapters: input{" +
                        "--->--->--->index, " +
                        "--->--->--->title" +
                        "--->--->}" +
                        "--->}" +
                        "]",
                dtoTypes.toString()
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

        @Override
        public boolean isView() {
            return false;
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
