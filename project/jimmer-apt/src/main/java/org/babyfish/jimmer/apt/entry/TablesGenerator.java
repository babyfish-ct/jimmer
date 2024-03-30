package org.babyfish.jimmer.apt.entry;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.util.ClassNames;
import org.babyfish.jimmer.impl.util.StringUtil;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Collection;

public class TablesGenerator extends AbstractSummaryGenerator {

    private final String packageName;

    private final String simpleName;

    private final Collection<TypeElement> typeElements;

    private final Filer filer;

    private final boolean isEx;

    public TablesGenerator(String packageName, String simpleName, Collection<TypeElement> typeElements, Filer filer, boolean isEx) {
        this.packageName = packageName;
        this.simpleName = simpleName;
        this.typeElements = typeElements;
        this.filer = filer;
        this.isEx = isEx;
    }

    public void generate() {
        TypeSpec typeSpec = typeSpec();
        try {
            JavaFile
                    .builder(
                            packageName,
                            typeSpec
                    )
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate draft interface for '%s'",
                            packageName + '.' + simpleName
                    ),
                    ex
            );
        }
    }

    private TypeSpec typeSpec() {
        TypeSpec.Builder builder = TypeSpec
                .interfaceBuilder(
                        ClassName.get(
                                packageName,
                                simpleName
                        )
                )
                .addModifiers(Modifier.PUBLIC);
        for (TypeElement typeElement : typeElements) {
            builder.addField(field(typeElement));
        }
        return builder.build();
    }

    private FieldSpec field(TypeElement typeElement) {
        String suffix = isEx ? "TableEx" : "Table";
        ClassName tableClassName = ClassNames.of(typeElement, name -> name + suffix);
        return FieldSpec
                .builder(
                        tableClassName,
                        distinctName(
                                StringUtil.snake(
                                        typeElement.getSimpleName().toString() + suffix,
                                        StringUtil.SnakeCase.UPPER
                                )
                        ),
                        Modifier.PUBLIC,
                        Modifier.STATIC,
                        Modifier.FINAL
                )
                .initializer("$T.$$", tableClassName)
                .build();
    }
}
