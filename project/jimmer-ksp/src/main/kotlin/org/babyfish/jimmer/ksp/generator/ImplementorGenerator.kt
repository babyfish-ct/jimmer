package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.*
import org.babyfish.jimmer.jackson.ImmutableModuleRequiredException
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import org.babyfish.jimmer.meta.PropId
import kotlin.reflect.KClass

class ImplementorGenerator(
    private val type: ImmutableType,
    private val parent: TypeSpec.Builder
) {

    fun generate() {
        parent.addType(
            TypeSpec
                .interfaceBuilder(IMPLEMENTOR)
                .addModifiers(KModifier.PRIVATE, KModifier.ABSTRACT)
                .addSuperinterface(type.className)
                .addSuperinterface(IMMUTABLE_SPI_CLASS_NAME)
                .apply {
                    addGetFun(PropId::class)
                    addGetFun(String::class)
                    addTypeFun()
                    addDummyPropForNoImmutableModuleError()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addGetFun(
        argType: KClass<*>
    ) {
        addFunction(
            FunSpec
                .builder("__get")
                .addParameter("prop", argType)
                .addModifiers(KModifier.OVERRIDE)
                .returns(ANY.copy(nullable = true))
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            val appender = CaseAppender(this, type, argType)
                            if (argType == PropId::class) {
                                beginControlFlow("return when (prop.asIndex())")
                                appender.addIllegalCase()
                                addStatement("__get(prop.asName())")
                            } else {
                                beginControlFlow("return when (prop)")
                            }
                            for (prop in type.propsOrderById) {
                                appender.addCase(prop)
                                addStatement(prop.name)
                            }
                            addElseForNonExistingProp(type, argType)
                            endControlFlow()
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addTypeFun() {
        addFunction(
            FunSpec
                .builder("__type")
                .addModifiers(KModifier.OVERRIDE)
                .returns(IMMUTABLE_TYPE_CLASS_NAME)
                .addCode("return %T.type", type.draftClassName(PRODUCER))
                .build()
        )
    }

    private fun TypeSpec.Builder.addDummyPropForNoImmutableModuleError() {
        addProperty(
            PropertySpec
                .builder("dummyPropForJacksonError__", INT)
                .getter(
                    FunSpec
                        .getterBuilder()
                        .addStatement("throw %T()", ImmutableModuleRequiredException::class)
                        .build()
                )
                .build()
        )
    }
}