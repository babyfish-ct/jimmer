package testpkg.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
annotation class ConsiderAs(
    vararg val types: Type
)