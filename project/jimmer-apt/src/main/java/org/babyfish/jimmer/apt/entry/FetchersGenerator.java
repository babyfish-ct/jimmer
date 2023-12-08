package org.babyfish.jimmer.apt.entry;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.util.ClassNames;
import org.babyfish.jimmer.impl.util.StringUtil;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Collection;

public class FetchersGenerator extends AbstractSummaryGenerator {

    private final String packageName;

    private final Collection<TypeElement> typeElements;

    private final Filer filer;

    public FetchersGenerator(String packageName, Collection<TypeElement> typeElements, Filer filer) {
        this.packageName = packageName;
        this.typeElements = typeElements;
        this.filer = filer;
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
                            packageName + "Fetchers"
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
                                "Fetchers"
                        )
                )
                .addModifiers(Modifier.PUBLIC);
        for (TypeElement typeElement : typeElements) {
            builder.addField(field(typeElement));
        }
        return builder.build();
    }

    private FieldSpec field(TypeElement typeElement) {
        ClassName fetcherClassName = ClassNames.of(typeElement, name -> name + "Fetcher");
        return FieldSpec
                .builder(
                        fetcherClassName,
                        distinctName(
                                StringUtil.snake(
                                        typeElement.getSimpleName().toString() + "Fetcher",
                                        StringUtil.SnakeCase.UPPER
                                )
                        ),
                        Modifier.PUBLIC,
                        Modifier.STATIC,
                        Modifier.FINAL
                )
                .initializer("$T.$$", fetcherClassName)
                .build();
    }
}
