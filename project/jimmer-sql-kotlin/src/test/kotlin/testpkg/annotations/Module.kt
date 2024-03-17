package testpkg.annotations

@Target(AnnotationTarget.CLASS)
annotation class Module(
    vararg val value: String
)