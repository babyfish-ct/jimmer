package testpkg.annotations

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION)
annotation class Job(
    val value: Array<Task>
)