//package org.babyfish.jimmer.apt.tuple;
//
//import com.squareup.javapoet.ClassName;
//import com.squareup.javapoet.JavaFile;
//import com.squareup.javapoet.MethodSpec;
//import com.squareup.javapoet.TypeSpec;
//import org.babyfish.jimmer.apt.Context;
//import org.babyfish.jimmer.apt.GeneratorException;
//import org.babyfish.jimmer.apt.MetaException;
//import org.babyfish.jimmer.sql.TypedTuple;
//import org.jetbrains.annotations.Nullable;
//
//import javax.annotation.processing.RoundEnvironment;
//import javax.lang.model.element.*;
//import java.io.IOException;
//import java.util.*;
//
//public class TypedTupleProcessor {
//
//    private static final String[] EMPTY_STR_ARR = new String[0];
//
//    static final String BASE_TABLE_SUFFIX = "BaseTable";
//
//    static final String MAPPER_SUFFIX = "Mapper";
//
//    private final Context context;
//
//    private final Set<String> delayedTupleTypeNames;
//
//    public TypedTupleProcessor(Context context, Set<String> delayedTupleTypeNames) {
//        this.context = context;
//        this.delayedTupleTypeNames = delayedTupleTypeNames;
//    }
//
//    public void process(RoundEnvironment roundEnv) {
//        List<TypeElement> typeElements = new ArrayList<>();
//        for (Element element : roundEnv.getElementsAnnotatedWith(TypedTuple.class)) {
//            TypeElement typeElement = (TypeElement) element;
//            if (!delayedTupleTypeNames.contains(typeElement.getQualifiedName().toString())) {
//                typeElements.add(typeElement);
//            }
//        }
//        for (String typeName : delayedTupleTypeNames) {
//            TypeElement typeElement = context.getElements().getTypeElement(typeName);
//            typeElements.add(typeElement);
//        }
//        TreeNode rootNode = new TreeNode(null, 0, null, null);
//        for (TypeElement typeElement : typeElements) {
//            validate(typeElement);
//            addTreeNode(typeElement, TreeNode.FEATURE_BASE_ROW, rootNode).setLeaf();
//            addTreeNode(typeElement, TreeNode.FEATURE_MAPPER, rootNode).setLeaf();
//        }
//        new TreeHandler(context).handle(rootNode);
//    }
//
//    private void validate(TypeElement typeElement) {
//        if (typeElement.getKind() != ElementKind.CLASS) {
//            throw new MetaException(
//                    typeElement,
//                    "The type decorated by \"@" +
//                            TypedTuple.class.getName() +
//                            "\" must be class"
//            );
//        }
//        if (!typeElement.getSuperclass().toString().equals("java.lang.Object")) {
//            throw new MetaException(
//                    typeElement,
//                    "The type decorated by \"@" +
//                            TypedTuple.class.getName() +
//                            "\" cannot inherit other class"
//            );
//        }
//        if (!typeElement.getInterfaces().isEmpty()) {
//            throw new MetaException(
//                    typeElement,
//                    "The type decorated by \"@" +
//                            TypedTuple.class.getName() +
//                            "\" cannot inherit interfaces"
//            );
//        }
//        if (!typeElement.getTypeParameters().isEmpty()) {
//            throw new MetaException(
//                    typeElement,
//                    "The type decorated by \"@" +
//                            TypedTuple.class.getName() +
//                            "\" cannot be generic type"
//            );
//        }
//    }
//
//    private static TreeNode addTreeNode(Element element, int feature, TreeNode rootNode) {
//        Element parentElement = element.getEnclosingElement();
//        if (parentElement instanceof PackageElement) {
//            return rootNode.childNode(parentElement, feature).childNode(element, feature).setRoot();
//        }
//        TypeElement tp = (TypeElement) parentElement;
//        if (tp.getAnnotation(TypedTuple.class) != null) {
//            throw new MetaException(
//                    element,
//                    "The type decorated by \"@" +
//                            TypedTuple.class.getName() +
//                            "\" cannot be declared in another type decorated by \"@" +
//                            TypedTuple.class.getName() +
//                            "\""
//            );
//        }
//        if (tp.getKind() == ElementKind.CLASS && !element.getModifiers().contains(Modifier.STATIC)) {
//            throw new MetaException(
//                    element,
//                    "The type decorated by \"@" +
//                            TypedTuple.class.getName() +
//                            "\" cannot be non-static nested class"
//            );
//        }
//        return addTreeNode(tp, feature, rootNode).childNode(element, feature);
//    }
//
//    private static class TreeNode {
//
//        static final int FEATURE_NAMESPACE = 0;
//
//        static final int FEATURE_BASE_ROW = 1;
//
//        static final int FEATURE_MAPPER = 2;
//
//        final String key;
//
//        final int feature;
//
//        final Element element;
//
//        final TreeNode parentNode;
//
//        private Map<String, TreeNode> childNodeMap;
//
//        boolean leaf;
//
//        boolean root;
//
//        TreeNode(String key, int feature, Element element, TreeNode parentNode) {
//            this.key = key;
//            this.feature = feature;
//            this.element = element;
//            this.parentNode = parentNode;
//        }
//
//        Collection<TreeNode> childNodes() {
//            Map<String, TreeNode> childNodeMap = this.childNodeMap;
//            if (childNodeMap == null) {
//                return Collections.emptyList();
//            }
//            return childNodeMap.values();
//        }
//
//        TreeNode childNode(Element element, int feature) {
//            String key;
//            if (element instanceof PackageElement) {
//                key = ((PackageElement) element).getQualifiedName().toString();
//            } else if (element.getAnnotation(TypedTuple.class) == null) {
//                key = element.getSimpleName().toString();
//            } else {
//                key = element.getSimpleName().toString();
//                if (feature != 0) {
//                    key += ":feature(" + feature + ")";
//                }
//            }
//            Map<String, TreeNode> childNodeMap = this.childNodeMap;
//            if (childNodeMap == null) {
//                this.childNodeMap = childNodeMap = new TreeMap<>();
//            }
//            return childNodeMap.computeIfAbsent(
//                    key,
//                    it -> new TreeNode(
//                            it,
//                            element instanceof TypeElement ? feature : FEATURE_NAMESPACE,
//                            element,
//                            this
//                    )
//            );
//        }
//
//        @Override
//        public String toString() {
//            return key;
//        }
//
//        public void setLeaf() {
//            this.leaf = true;
//        }
//
//        TreeNode setRoot() {
//            this.root = true;
//            return this;
//        }
//
//        @Nullable
//        private String simpleName() {
//            if (!(element instanceof TypeElement)) {
//                return null;
//            }
//            String simpleName = element.getSimpleName().toString();
//            if (leaf) {
//                String suffix;
//                switch (feature) {
//                    case FEATURE_BASE_ROW:
//                        suffix = BASE_TABLE_SUFFIX;
//                        break;
//                    case FEATURE_MAPPER:
//                        suffix = MAPPER_SUFFIX;
//                        break;
//                    default:
//                        suffix = "";
//                        break;
//                }
//                return simpleName + suffix;
//            }
//            if (root) {
//                return simpleName + "_";
//            }
//            return simpleName;
//        }
//
//        public ClassName className() {
//            List<String> simpleNames = new ArrayList<>();
//            String packageName = "";
//            for (TreeNode treeNode = this; treeNode != null; treeNode = treeNode.parentNode) {
//                String simpleName = treeNode.simpleName();
//                if (simpleName != null) {
//                    simpleNames.add(0, simpleName);
//                } else {
//                    packageName = ((PackageElement)treeNode.element).getQualifiedName().toString();
//                    break;
//                }
//            }
//            return ClassName.get(
//                    packageName,
//                    simpleNames.get(0),
//                    simpleNames.subList(1, simpleNames.size()).toArray(EMPTY_STR_ARR)
//            );
//        }
//    }
//
//    private static class TreeHandler {
//
//        private final Context context;
//
//        TreeHandler(Context context) {
//            this.context = context;
//        }
//
//        void handle(TreeNode treeNode) {
//            if (!(treeNode.element instanceof TypeElement)) {
//                for (TreeNode childNode : treeNode.childNodes()) {
//                    handle(childNode);
//                }
//            } else if (treeNode.root) {
//                try {
//                    JavaFile
//                            .builder(
//                                    ((PackageElement)treeNode.parentNode.element).getQualifiedName().toString(),
//                                    generate(treeNode)
//                            )
//                            .indent("    ")
//                            .build()
//                            .writeTo(context.getFiler());
//                } catch (IOException ex) {
//                    throw new GeneratorException(
//                            String.format(
//                                    "Cannot generate type tuple classes for '%s'",
//                                    ((TypeElement) treeNode.element).getQualifiedName().toString()
//                            ),
//                            ex
//                    );
//                }
//            }
//        }
//
//        private TypeSpec generate(TreeNode treeNode) {
//            String simpleName = treeNode.simpleName();
//            assert simpleName != null;
//            TypeSpec.Builder builder = TypeSpec
//                    .classBuilder(simpleName)
//                    .addModifiers(Modifier.PUBLIC);
//            if (!treeNode.root) {
//                builder.addModifiers(Modifier.STATIC);
//            }
//            if (treeNode.leaf) {
//                generateMembers(
//                        (TypeElement) treeNode.element,
//                        treeNode.className(),
//                        treeNode.feature,
//                        builder
//                );
//            } else {
//                builder.addMethod(
//                    MethodSpec
//                            .constructorBuilder()
//                            .addModifiers(Modifier.PRIVATE)
//                            .build()
//                );
//                for (TreeNode childNode : treeNode.childNodes()) {
//                    builder.addType(generate(childNode));
//                }
//            }
//            return builder.build();
//        }
//
//        private void generateMembers(
//                TypeElement typeElement,
//                ClassName className,
//                int feature,
//                TypeSpec.Builder builder
//        ) {
//            if (feature == TreeNode.FEATURE_BASE_ROW) {
//                new BaseTableMemberGenerator(context, typeElement, className, builder).generate();
//            }
//            if (feature == TreeNode.FEATURE_MAPPER) {
//                new MapperMemberGenerator(context, typeElement, className, builder).generate();
//            }
//        }
//    }
//}
