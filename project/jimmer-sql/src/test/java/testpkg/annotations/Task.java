package testpkg.annotations;

public @interface Task {

    String value();

    Priority priority() default Priority.NORMAL;

    int estimation() default 8;
}
