package testpkg.annotations

annotation class Task(
    val value: String,
    val priority: Priority = Priority.NORMAL,
    val estimation: Int = 8
)