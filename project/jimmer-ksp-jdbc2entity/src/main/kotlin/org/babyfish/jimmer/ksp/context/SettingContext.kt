package org.babyfish.jimmer.ksp.context

import org.babyfish.jimmer.ksp.context.Settings.Companion.toBean
import java.util.concurrent.atomic.AtomicReference

object SettingContext {
    private val settingsRef = AtomicReference<Settings?>()

    val settings: Settings
        get() = settingsRef.get() ?: Settings()

    fun initialize(op: Map<String, String>) {
        val toMap = settings.toMap()
        val map = toMap + op
        val mapToBean = toBean(map)
        settingsRef.compareAndSet(null, mapToBean)
    }
}
